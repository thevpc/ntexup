/*
 * ntexup-player.js — replays an ntexup html-static export on a canvas.
 *
 * The exported data is the java2d call stream of the real engine, captured at
 * export time by ntexup-renderer-html-static. Nothing here measures, lays out
 * or shapes text: every path arrives as geometry, every color as a string and
 * every operation carries its own absolute transform and clip. The browser is a
 * playback device, not a renderer.
 *
 * Format contract (format 1):
 *   manifest.json  {format,width,height,periodMs,animated,
 *                   pages:[{index,file,frames,durationMs,loopStartMs,loopEndMs,
 *                           animated,bytes}],imageAssets:[sha...]}
 *   page json      {defs:[...],frames:[{t,ops:[...]}|{t,patch:[[i,v]],len?}]}
 *                  a {"$":id} object references defs; ids are interned
 *                  bottom-up, so a def only ever references lower ids.
 *
 * Usage:
 *   NTxPlayer.mount({manifest:"manifest.json", canvas:"ntexup-canvas",
 *                    onPage:function (index, total) {...}});
 * Returns a promise for a controller:
 *   {page, pages, goTo, next, prev, first, last, pause, resume, destroy}
 */
(function (global) {
    'use strict';

    var FORMAT = 1;
    var PREFETCH = 3;
    var KEEP = 4;
    var MAX_RATIO = 2;
    var DEFAULT_PAINT = "#000000";

    var warned = {};
    function warnOnce(kind, detail) {
        if (!warned[kind]) {
            warned[kind] = true;
            console.warn("ntexup-player: " + kind + " cannot be replayed on canvas" +
                (detail ? " (" + detail + ")" : "") + ", skipped");
        }
    }

    var GENERIC_FAMILIES = {
        "SansSerif": "sans-serif",
        "Serif": "serif",
        "Monospaced": "monospace",
        "Dialog": "sans-serif",
        "DialogInput": "monospace"
    };

    // ------------------------------------------------------------------ defs

    function isRef(v) {
        return v !== null && typeof v === "object" && !Array.isArray(v)
            && typeof v.$ === "number" && Object.keys(v).length === 1;
    }

    /**
     * Resolves the defs references of one page, memoised per defs entry. Each
     * resolved value is shared by every operation that interned it, which is what
     * keeps a 200-frame page from duplicating its geometry.
     */
    function makeResolver(defs) {
        var cache = new Array(defs.length);
        function resolve(v) {
            if (isRef(v)) {
                var id = v.$;
                if (id < 0 || id >= defs.length) {
                    warnOnce("def", "id " + id);
                    return null;
                }
                if (cache[id] === undefined) {
                    cache[id] = resolve(defs[id]);
                }
                return cache[id];
            }
            if (Array.isArray(v)) {
                var a = new Array(v.length);
                for (var i = 0; i < v.length; i++) {
                    a[i] = resolve(v[i]);
                }
                return a;
            }
            if (v !== null && typeof v === "object") {
                var o = {};
                for (var k in v) {
                    if (Object.prototype.hasOwnProperty.call(v, k)) {
                        o[k] = resolve(v[k]);
                    }
                }
                return o;
            }
            return v;
        }
        return resolve;
    }

    // ----------------------------------------------------------------- shapes

    /**
     * A recorded number may arrive as null: java rounds to three decimals and
     * NaN or Infinity has no JSON spelling, so the recorder ships null. Any
     * operation whose geometry is not finite is dropped, never fed NaN.
     */
    function ok() {
        for (var i = 0; i < arguments.length; i++) {
            var v = arguments[i];
            if (typeof v !== "number" || !isFinite(v)) {
                return false;
            }
        }
        return true;
    }

    var pathCache = new WeakMap();

    /**
     * Parses the grammar of PaintStreamFormat.shapePath: one command letter per
     * token, coordinates comma separated, only M L Q C Z.
     */
    function buildPath(d) {
        var p = new Path2D();
        var tokens = d.split(" ");
        for (var i = 0; i < tokens.length; i++) {
            var token = tokens[i];
            if (token === "") {
                continue;
            }
            var n = token.substring(1).split(",");
            switch (token.charAt(0)) {
                case "M":
                    p.moveTo(+n[0], +n[1]);
                    break;
                case "L":
                    p.lineTo(+n[0], +n[1]);
                    break;
                case "Q":
                    p.quadraticCurveTo(+n[0], +n[1], +n[2], +n[3]);
                    break;
                case "C":
                    p.bezierCurveTo(+n[0], +n[1], +n[2], +n[3], +n[4], +n[5]);
                    break;
                case "Z":
                    p.closePath();
                    break;
                default:
                    throw new Error("ntexup-player: unknown path command '"
                        + token.charAt(0) + "'");
            }
        }
        return p;
    }

    /**
     * @return the geometry of a recorded shape, built once per shape value.
     */
    function shapePath(shape) {
        if (shape === null || shape === undefined) {
            return null;
        }
        if (Array.isArray(shape)) {
            //a rectangular clip is the compact form, not a shape object
            var clip = new Path2D();
            if (ok(shape[0], shape[1], shape[2], shape[3])) {
                clip.rect(shape[0], shape[1], shape[2], shape[3]);
            }
            return clip;
        }
        var cached = pathCache.get(shape);
        if (cached) {
            return cached;
        }
        var path = new Path2D();
        switch (shape.k) {
            case "path":
                path = buildPath(shape.d || "");
                break;
            case "rect":
                if (ok(shape.x, shape.y, shape.width, shape.height)) {
                    path.rect(shape.x, shape.y, shape.width, shape.height);
                }
                break;
            case "oval":
                if (ok(shape.x, shape.y, shape.width, shape.height)) {
                    path.ellipse(shape.x + shape.width / 2, shape.y + shape.height / 2,
                        Math.abs(shape.width / 2), Math.abs(shape.height / 2), 0, 0, Math.PI * 2);
                }
                break;
            case "line":
                if (ok(shape.x1, shape.y1, shape.x2, shape.y2)) {
                    path.moveTo(shape.x1, shape.y1);
                    path.lineTo(shape.x2, shape.y2);
                }
                break;
            case "roundRect":
                //java sweeps quarter ellipses; a quadratic corner reads the same
                rounded(path, shape.x, shape.y, shape.width, shape.height,
                    ok(shape.arcwidth) ? shape.arcwidth / 2 : 0,
                    ok(shape.archeight) ? shape.archeight / 2 : 0);
                break;
            default:
                warnOnce("shape", shape.k);
                return null;
        }
        pathCache.set(shape, path);
        return path;
    }

    function rounded(path, x, y, w, h, rx, ry) {
        if (!ok(x, y, w, h)) {
            return;
        }
        if (rx <= 0 || ry <= 0) {
            path.rect(x, y, w, h);
            return;
        }
        rx = Math.min(rx, w / 2);
        ry = Math.min(ry, h / 2);
        path.moveTo(x, y + ry);
        path.quadraticCurveTo(x, y, x + rx, y);
        path.lineTo(x + w - rx, y);
        path.quadraticCurveTo(x + w, y, x + w, y + ry);
        path.lineTo(x + w, y + h - ry);
        path.quadraticCurveTo(x + w, y + h, x + w - rx, y + h);
        path.lineTo(x + rx, y + h);
        path.quadraticCurveTo(x, y + h, x, y + h - ry);
        path.closePath();
    }

    /**
     * `points` ships as one string of "x,y" pairs, the compact form java uses for
     * polygon coordinate arrays.
     */
    function pointsPath(points, close) {
        var p = new Path2D();
        if (typeof points !== "string") {
            return p;
        }
        var pairs = points.split(" ");
        for (var i = 0; i < pairs.length; i++) {
            var xy = pairs[i].split(",");
            var x = +xy[0];
            var y = +xy[1];
            if (!isFinite(x) || !isFinite(y)) {
                continue;
            }
            if (i === 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        if (close) {
            p.closePath();
        }
        return p;
    }

    // ----------------------------------------------------------------- paints

    function toCss(c) {
        //java ships #rrggbb, or #rrggbbaa when the alpha is not 255; CSS level 4
        //spells the same but has no 8-bit short form, so 4 digits are expanded
        if (typeof c !== "string" || c.charAt(0) !== "#") {
            return null;
        }
        if (c.length === 4) {
            return "#" + c.charAt(1) + c.charAt(1) + c.charAt(2) + c.charAt(2)
                + c.charAt(3) + c.charAt(3);
        }
        return c;
    }

    var paintCache = new WeakMap();

    function addStops(gradient, stops) {
        if (!Array.isArray(stops)) {
            return;
        }
        for (var i = 0; i < stops.length; i++) {
            var s = stops[i];
            var color = toCss(s && s[1]);
            if (s && ok(s[0]) && color) {
                gradient.addColorStop(Math.max(0, Math.min(1, s[0])), color);
            }
        }
    }

    function cycleMethod(cycle) {
        //the recorder ships the java enum name: NO_CYCLE, REPEAT, REFLECT
        if (cycle === "REPEAT") {
            return "repeat";
        }
        if (cycle === "REFLECT") {
            return "reflect";
        }
        return "pad";
    }

    /**
     * A recorded paint becomes a canvas style. Gradients are built in the
     * operation's user space, which is where java resolved them, so the current
     * transform needs nothing extra here.
     */
    function paintStyle(ctx, paint) {
        if (paint === null || paint === undefined) {
            return DEFAULT_PAINT;
        }
        if (typeof paint === "string") {
            return toCss(paint) || DEFAULT_PAINT;
        }
        var cached = paintCache.get(paint);
        if (cached !== undefined) {
            return cached;
        }
        var style = null;
        switch (paint.k) {
            case "linearGradient":
                if (paint.start && paint.end
                    && ok(paint.start[0], paint.start[1], paint.end[0], paint.end[1])) {
                    style = ctx.createLinearGradient(paint.start[0], paint.start[1],
                        paint.end[0], paint.end[1]);
                    addStops(style, paint.stops);
                    style.spreadMethod = cycleMethod(paint.cycle);
                }
                break;
            case "radialGradient":
                if (paint.center && paint.focus
                    && ok(paint.radius, paint.center[0], paint.center[1],
                        paint.focus[0], paint.focus[1])) {
                    style = ctx.createRadialGradient(paint.focus[0], paint.focus[1], 0,
                        paint.center[0], paint.center[1], Math.abs(paint.radius));
                    addStops(style, paint.stops);
                    style.spreadMethod = cycleMethod(paint.cycle);
                }
                break;
            default:
                warnOnce("paint", paint.k || "unknown");
                style = DEFAULT_PAINT;
                break;
        }
        if (style === null) {
            warnOnce("paint", paint.k + " has no usable geometry");
            style = DEFAULT_PAINT;
        }
        paintCache.set(paint, style);
        return style;
    }

    function applyStroke(ctx, stroke) {
        if (stroke === null || stroke === undefined) {
            return;
        }
        if (ok(stroke.width)) {
            ctx.lineWidth = Math.max(stroke.width, 0.02);
        }
        if (stroke.cap === "butt" || stroke.cap === "round" || stroke.cap === "square") {
            ctx.lineCap = stroke.cap;
        }
        if (stroke.join === "bevel" || stroke.join === "miter" || stroke.join === "round") {
            ctx.lineJoin = stroke.join;
        }
        if (ok(stroke.miterlimit)) {
            ctx.miterLimit = stroke.miterlimit;
        }
        if (Array.isArray(stroke.dash)) {
            var dash = [];
            for (var i = 0; i < stroke.dash.length; i++) {
                if (ok(stroke.dash[i])) {
                    dash.push(stroke.dash[i]);
                }
            }
            ctx.setLineDash(dash);
            ctx.lineDashOffset = ok(stroke.dashphase) ? stroke.dashphase : 0;
        }
    }

    // ----------------------------------------------------------------- images

    /**
     * Images are content addressed by the PNG sha256 that java encoded. A frame
     * asked for before the pixels land paints without them and asks to be
     * replayed once they arrive.
     * <p>
     * In a standalone export the pixels arrive as data: URLs from the asset map
     * instead of as files beside the page, because a page opened from the disk
     * cannot fetch its own directory.
     */
    function imageLoader(base, assets, onReady) {
        var cache = {};
        return function (sha) {
            var entry = cache[sha];
            if (!entry) {
                var img = new Image();
                entry = cache[sha] = {img: img, ready: false};
                img.onload = function () {
                    entry.ready = true;
                    onReady();
                };
                img.onerror = function () {
                    warnOnce("image", sha + " did not load");
                };
                img.src = assets && assets[sha]
                    ? assets[sha] : base + "assets/img/" + sha + ".png";
            }
            return entry;
        };
    }

    function pixelsOf(op, load) {
        var ref = op.ref;
        if (!ref || ref.k !== "png" || typeof ref.sha256 !== "string") {
            warnOnce("image", "no pixels recorded");
            return null;
        }
        var entry = load(ref.sha256);
        return entry.ready ? entry.img : null;
    }

    /**
     * `at` is either a [x,y] point or a six number transform, which is how
     * drawImage(Image, AffineTransform) and drawRenderedImage land in the stream.
     */
    function drawImageOp(ctx, op, load) {
        var img = pixelsOf(op, load);
        if (!img) {
            return;
        }
        var at = op.at;
        if (Array.isArray(at) && at.length === 6 && ok(at[0], at[1], at[2], at[3], at[4], at[5])) {
            ctx.transform(at[0], at[1], at[2], at[3], at[4], at[5]);
            ctx.drawImage(img, 0, 0, img.width, img.height);
            return;
        }
        if (Array.isArray(op.dest) && ok(op.dest[0], op.dest[1], op.dest[2], op.dest[3])) {
            ctx.drawImage(img, op.dest[0], op.dest[1], op.dest[2], op.dest[3]);
            return;
        }
        if (Array.isArray(at) && ok(at[0], at[1])) {
            ctx.drawImage(img, at[0], at[1], img.width, img.height);
            return;
        }
        warnOnce("image", "no usable placement");
    }

    // -------------------------------------------------------------------- ops

    /**
     * Replays one recorded operation. The stream is flat: every operation holds
     * its own absolute transform and clip, so nothing carries over and frames can
     * be replayed in any order.
     */
    function paintOp(ctx, op, load, ratio) {
        if (!op || typeof op !== "object" || typeof op.k !== "string") {
            return;
        }
        ctx.save();
        var t = op.transform;
        if (Array.isArray(t) && ok(t[0], t[1], t[2], t[3], t[4], t[5])) {
            //the recorded affine is absolute, so the device scale multiplies on
            //the left rather than composing with a previous operation
            ctx.setTransform(ratio * t[0], ratio * t[1], ratio * t[2], ratio * t[3],
                ratio * t[4], ratio * t[5]);
        } else {
            ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        }
        if (ok(op.alpha)) {
            ctx.globalAlpha = Math.max(0, Math.min(1, op.alpha));
        }
        if (op.smooth === false) {
            ctx.imageSmoothingEnabled = false;
        }
        if (op.clip !== null && op.clip !== undefined) {
            var clip = shapePath(op.clip);
            if (clip) {
                ctx.clip(clip);
            }
        }
        try {
            dispatch(ctx, op, load);
        } finally {
            ctx.restore();
        }
    }

    function fillStyle(ctx, op) {
        ctx.fillStyle = paintStyle(ctx, op.paint);
    }

    function strokeStyle(ctx, op) {
        ctx.strokeStyle = paintStyle(ctx, op.paint);
        applyStroke(ctx, op.stroke);
    }

    function arcPath(op) {
        var rx = op.width / 2;
        var ry = op.height / 2;
        var cx = op.x + rx;
        var cy = op.y + ry;
        if (!ok(rx, ry, cx, cy, op.start, op.extent) || rx <= 0 || ry <= 0) {
            return null;
        }
        var p = new Path2D();
        var half = Math.PI / 180;
        //java measures angles counter-clockwise from 3 o'clock with a
        //down-pointing y axis, so a java angle is the negative of a canvas one
        //and a positive sweep runs the other way round
        var from = -op.start * half;
        var to = -(op.start + op.extent) * half;
        if (Math.abs(op.extent) >= 360) {
            p.ellipse(cx, cy, rx, ry, 0, 0, Math.PI * 2);
        } else {
            p.ellipse(cx, cy, rx, ry, 0, from, to, op.extent > 0);
        }
        if (op.k === "fillArc") {
            //an arc fills as a pie wedge, closed to the centre
            var wedge = new Path2D();
            wedge.moveTo(cx, cy);
            wedge.addPath(p);
            wedge.closePath();
            return wedge;
        }
        return p;
    }

    /**
     * Text is glyph outlines decided by java at export time: translate to the
     * recorded baseline and fill. No font, shaping or measurement happens in the
     * browser, which is what makes the page identical on every machine.
     */
    function drawText(ctx, op, load) {
        var at = op.at;
        if (!Array.isArray(at) || !ok(at[0], at[1])) {
            return;
        }
        if (op.outline) {
            var path = shapePath(op.outline);
            if (path) {
                ctx.translate(at[0], at[1]);
                fillStyle(ctx, op);
                ctx.fill(path);
                return;
            }
        }
        //only the outline-less fallback needs a browser font
        var text = typeof op.s === "string" ? op.s
            : (op.text && typeof op.text.s === "string" ? op.text.s : null);
        if (text === null) {
            warnOnce("text", "no outline and no string");
            return;
        }
        var font = op.font || (op.text && Array.isArray(op.text.fonts) ? op.text.fonts[0] : null);
        var family = GENERIC_FAMILIES[font && font.family] || (font && font.family) || "sans-serif";
        var weight = font && font.style === "bold" ? "bold "
            : (font && font.style === "bolditalic" ? "bold italic "
                : (font && font.style === "italic" ? "italic " : ""));
        var size = ok(font && font.size) ? font.size : 12;
        ctx.translate(at[0], at[1]);
        fillStyle(ctx, op);
        ctx.font = weight + size + "px " + JSON.stringify(family);
        ctx.textBaseline = "alphabetic";
        ctx.fillText(text, 0, 0);
    }

    function dispatch(ctx, op, load) {
        switch (op.k) {
            case "fill": {
                var filled = shapePath(op.shape);
                if (filled) {
                    fillStyle(ctx, op);
                    ctx.fill(filled);
                }
                break;
            }
            case "draw": {
                var stroked = shapePath(op.shape);
                if (stroked) {
                    strokeStyle(ctx, op);
                    ctx.stroke(stroked);
                }
                break;
            }
            case "fillRect":
                if (ok(op.x, op.y, op.width, op.height)) {
                    fillStyle(ctx, op);
                    ctx.fillRect(op.x, op.y, op.width, op.height);
                }
                break;
            case "fillRoundRect":
            case "fillOval": {
                var shape = shapePath(rectLikeShape(op));
                if (shape) {
                    fillStyle(ctx, op);
                    ctx.fill(shape);
                }
                break;
            }
            case "drawRect":
            case "drawRoundRect":
            case "drawOval":
            case "drawLine": {
                var outline = shapePath(rectLikeShape(op));
                if (outline) {
                    strokeStyle(ctx, op);
                    ctx.stroke(outline);
                }
                break;
            }
            case "fillArc":
            case "drawArc": {
                var arc = arcPath(op);
                if (arc) {
                    if (op.k === "fillArc") {
                        fillStyle(ctx, op);
                        ctx.fill(arc);
                    } else {
                        strokeStyle(ctx, op);
                        ctx.stroke(arc);
                    }
                }
                break;
            }
            case "fillPolygon":
                fillStyle(ctx, op);
                ctx.fill(pointsPath(op.points, true));
                break;
            case "drawPolygon":
                strokeStyle(ctx, op);
                ctx.stroke(pointsPath(op.points, true));
                break;
            case "drawPolyline":
                strokeStyle(ctx, op);
                ctx.stroke(pointsPath(op.points, false));
                break;
            case "clearRect":
                if (ok(op.x, op.y, op.width, op.height)) {
                    ctx.clearRect(op.x, op.y, op.width, op.height);
                }
                break;
            case "text":
            case "chars":
            case "bytes":
            case "glyphs":
                drawText(ctx, op, load);
                break;
            case "image":
            case "renderedImage":
                drawImageOp(ctx, op, load);
                break;
            case "imagePart": {
                var part = pixelsOf(op, load);
                if (part && ok(op.dest[0], op.dest[1], op.dest2[0], op.dest2[1],
                    op.src[0], op.src[1], op.src2[0], op.src2[1])) {
                    ctx.drawImage(part, op.src[0], op.src[1],
                        op.src2[0] - op.src[0], op.src2[1] - op.src[1],
                        op.dest[0], op.dest[1],
                        op.dest2[0] - op.dest[0], op.dest2[1] - op.dest[1]);
                }
                break;
            }
            case "imageOp":
                if (op.ref) {
                    drawImageOp(ctx, op, load);
                } else {
                    warnOnce("imageOp", "a java2d filter baked its own pixels");
                }
                break;
            case "fill3DRect":
            case "draw3DRect":
                warnOnce(op.k, "replayed without the bevel");
                if (op.k === "fill3DRect") {
                    fillStyle(ctx, op);
                } else {
                    strokeStyle(ctx, op);
                }
                var flat = shapePath(rectLikeShape(op));
                if (flat) {
                    if (op.k === "fill3DRect") {
                        ctx.fill(flat);
                    } else {
                        ctx.stroke(flat);
                    }
                }
                break;
            case "copyArea":
            case "renderableImage":
                warnOnce(op.k);
                break;
            default:
                warnOnce("operation", op.k);
                break;
        }
    }

    function rectLikeShape(op) {
        switch (op.k) {
            case "fillRoundRect":
            case "drawRoundRect":
            case "fill3DRect":
            case "draw3DRect":
                return {k: "roundRect", x: op.x, y: op.y, width: op.width,
                    height: op.height, arcwidth: op.arcwidth, archeight: op.archeight};
            case "fillOval":
            case "drawOval":
                return {k: "oval", x: op.x, y: op.y, width: op.width, height: op.height};
            case "drawLine":
                return {k: "line", x1: op.x1, y1: op.y1, x2: op.x2, y2: op.y2};
            default:
                return {k: "rect", x: op.x, y: op.y, width: op.width, height: op.height};
        }
    }

    // ------------------------------------------------------------------ pages

    /**
     * Flattens a page into one operation list per frame, applying patches, so a
     * frame can be replayed by index without walking the stream again.
     */
    function expandFrames(page, resolve) {
        var frames = [];
        var slots = [];
        var list = page.frames || [];
        for (var i = 0; i < list.length; i++) {
            var frame = list[i];
            if (Array.isArray(frame.ops)) {
                slots = frame.ops.map(resolve);
            } else if (Array.isArray(frame.patch)) {
                slots = slots.slice();
                for (var k = 0; k < frame.patch.length; k++) {
                    var entry = frame.patch[k];
                    if (!Array.isArray(entry) || typeof entry[0] !== "number") {
                        continue;
                    }
                    while (slots.length < entry[0]) {
                        slots.push(null);
                    }
                    var value = resolve(entry[1]);
                    if (entry[0] === slots.length) {
                        slots.push(value);
                    } else {
                        slots[entry[0]] = value;
                    }
                }
                if (typeof frame.len === "number" && frame.len >= 0) {
                    slots = slots.slice(0, frame.len);
                }
            }
            frames.push({t: ok(frame.t) ? frame.t : i * 40, ops: slots.slice()});
        }
        return frames;
    }

    /**
     * The loop window in frame indices. A page with no cycle plays once and holds
     * its last frame: a continuous animation whose period is not a multiple of
     * the sample step has no exact cycle to wrap on, and wrapping a partial
     * period would show a jump.
     */
    function loopWindow(frames, loopStartMs, loopEndMs) {
        if (!ok(loopStartMs) || loopStartMs < 0 || frames.length < 2) {
            return null;
        }
        var first = -1;
        var last = -1;
        for (var i = 0; i < frames.length; i++) {
            if (first < 0 && frames[i].t >= loopStartMs) {
                first = i;
            }
            if (frames[i].t <= loopEndMs) {
                last = i;
            }
        }
        return last > first ? {first: first, count: last - first + 1} : null;
    }

    function PageStore(manifest, base) {
        var pages = manifest.pages || [];
        var cache = {};

        function decode(index, meta, json) {
            var frames = expandFrames(json, makeResolver(json.defs || []));
            var loaded = {
                meta: meta,
                frames: frames,
                loop: loopWindow(frames, meta.loopStartMs, meta.loopEndMs)
            };
            cache[index] = loaded;
            return loaded;
        }

        function load(index) {
            var existing = cache[index];
            if (existing) {
                return existing.then ? existing : Promise.resolve(existing);
            }
            var meta = pages[index];
            if (!meta) {
                return Promise.reject(new Error("ntexup-player: no page " + (index + 1)));
            }
            //a standalone export carries its pages inside the manifest it embeds
            if (meta.data) {
                return Promise.resolve(decode(index, meta, meta.data));
            }
            var entry = fetch(base + meta.file)
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("ntexup-player: " + meta.file + " -> " + response.status);
                    }
                    return response.json();
                })
                .then(function (json) {
                    return decode(index, meta, json);
                });
            cache[index] = entry;
            return entry;
        }

        return {
            count: pages.length,
            load: load,
            prefetch: function (index) {
                var from = Math.max(0, index - PREFETCH);
                var to = Math.min(pages.length - 1, index + PREFETCH);
                for (var i = from; i <= to; i++) {
                    load(i);
                }
                for (var key in cache) {
                    if (Object.prototype.hasOwnProperty.call(cache, key)) {
                        if (parseInt(key, 10) < from - 1 || parseInt(key, 10) > to + 1) {
                            delete cache[key];
                        }
                    }
                }
            }
        };
    }

    // ----------------------------------------------------------------- player

    function mount(options) {
        options = options || {};
        var canvas = typeof options.canvas === "string"
            ? document.getElementById(options.canvas) : options.canvas;
        if (!canvas) {
            throw new Error("ntexup-player: no canvas");
        }
        var ctx = canvas.getContext("2d");
        //options.data is the manifest itself, already in the page: a standalone
        //export is one file and must not reach for the network
        var data = options.data || null;
        var manifestUrl = options.manifest || "manifest.json";
        var base = data ? "" : manifestUrl.replace(/[^/]*$/, "");
        var listeners = [];
        var manifest = null;
        var store = null;
        var current = null;
        var index = 0;
        var startedAt = 0;
        var pausedAt = 0;
        var raf = 0;
        var ratio = 1;
        var playing = true;

        function on(target, type, handler) {
            target.addEventListener(type, handler);
            listeners.push([target, type, handler]);
        }

        function load() {
            return loader;
        }

        function sizeCanvas() {
            ratio = Math.min(MAX_RATIO, Math.max(1, global.devicePixelRatio || 1));
            canvas.width = Math.round(manifest.width * ratio);
            canvas.height = Math.round(manifest.height * ratio);
        }

        function render(i) {
            if (!current) {
                return;
            }
            var frames = current.frames;
            var step = ok(manifest.periodMs) && manifest.periodMs > 0 ? manifest.periodMs : 40;
            var at = Math.floor((playing ? performance.now() - startedAt : 0) / step);
            if (frames.length === 1) {
                at = 0;
            } else if (current.loop) {
                at = at < current.loop.first
                    ? Math.max(0, at)
                    : current.loop.first + ((at - current.loop.first) % current.loop.count);
            } else {
                at = Math.min(Math.max(0, at), frames.length - 1);
            }
            var f = frames[Math.min(at, frames.length - 1)];
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            for (var k = 0; k < f.ops.length; k++) {
                paintOp(ctx, f.ops[k], load(), ratio);
            }
        }

        var loader = imageLoader(base, data && data.assets, function () {
            if (!raf) {
                raf = global.requestAnimationFrame(function () {
                    raf = 0;
                    render(index);
                });
            }
        });

        function schedule() {
            if (raf || !current || current.frames.length < 2 || !playing) {
                if (!current || current.frames.length < 2) {
                    render(index);
                }
                return;
            }
            var step = function () {
                raf = 0;
                if (!playing || !current) {
                    return;
                }
                render(index);
                raf = global.requestAnimationFrame(step);
            };
            raf = global.requestAnimationFrame(step);
        }

        function emit() {
            if (typeof options.onPage === "function") {
                options.onPage(index, store.count);
            }
        }

        function hashIndex() {
            var match = /#p=(\d+)/.exec(global.location.hash || "");
            if (!match || !store) {
                return 0;
            }
            return Math.max(0, Math.min(store.count - 1, parseInt(match[1], 10) - 1));
        }

        function writeHash() {
            var hash = "#p=" + (index + 1);
            if (global.location.hash !== hash) {
                try {
                    global.history.replaceState(null, "", hash);
                } catch (e) {
                    global.location.hash = hash.substring(1);
                }
            }
        }

        function show(target) {
            if (!store) {
                return;
            }
            index = Math.max(0, Math.min(store.count - 1, target));
            startedAt = performance.now();
            playing = options.animate !== false;
            writeHash();
            emit();
            store.prefetch(index);
            var wanted = index;
            store.load(index).then(function (page) {
                if (wanted !== index) {
                    return;
                }
                current = page;
                schedule();
            }, function (error) {
                console.error(error);
                if (typeof options.onError === "function") {
                    options.onError(error);
                }
            });
        }

        function bindKeys() {
            on(global, "keydown", function (event) {
                var target = event.target;
                var tag = target && target.tagName;
                if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT"
                    || (target && target.isContentEditable)) {
                    return;
                }
                var step = event.ctrlKey || event.metaKey;
                var to = null;
                switch (event.key) {
                    case "ArrowRight":
                    case "ArrowDown":
                    case "PageDown":
                    case " ":
                    case "Spacebar":
                    case "Enter":
                        to = step ? store.count - 1 : index + 1;
                        break;
                    case "ArrowLeft":
                    case "ArrowUp":
                    case "PageUp":
                        to = step ? 0 : index - 1;
                        break;
                    case "Home":
                        to = 0;
                        break;
                    case "End":
                        to = store.count - 1;
                        break;
                    default:
                        return;
                }
                if (to !== null && to !== index) {
                    event.preventDefault();
                    show(to);
                }
            });
            on(canvas, "click", function (event) {
                show(event.shiftKey ? index - 1 : index + 1);
            });
            on(global, "hashchange", function () {
                var i = hashIndex();
                if (i !== index) {
                    show(i);
                }
            });
            on(global, "resize", function () {
                sizeCanvas();
                render(index);
            });
        }

        function destroy() {
            if (raf) {
                global.cancelAnimationFrame(raf);
                raf = 0;
            }
            for (var i = 0; i < listeners.length; i++) {
                listeners[i][0].removeEventListener(listeners[i][1], listeners[i][2]);
            }
            listeners = [];
        }

        var boot = data
            ? Promise.resolve(data)
            : fetch(manifestUrl).then(function (response) {
                if (!response.ok) {
                    throw new Error("ntexup-player: " + manifestUrl + " -> " + response.status);
                }
                return response.json();
            });

        return boot
            .then(function (loaded) {
                if (loaded.format !== FORMAT) {
                    throw new Error("ntexup-player: format " + loaded.format
                        + ", this player reads " + FORMAT);
                }
                manifest = loaded;
                store = PageStore(manifest, base);
                sizeCanvas();
                bindKeys();
                show(hashIndex());
                return {
                    get page() {
                        return index;
                    },
                    get pages() {
                        return store.count;
                    },
                    goTo: show,
                    next: function () {
                        show(index + 1);
                    },
                    prev: function () {
                        show(index - 1);
                    },
                    first: function () {
                        show(0);
                    },
                    last: function () {
                        show(store.count - 1);
                    },
                    pause: function () {
                        playing = false;
                        pausedAt = performance.now();
                        if (raf) {
                            global.cancelAnimationFrame(raf);
                            raf = 0;
                        }
                    },
                    resume: function () {
                        playing = true;
                        //keep the offset the animation had reached instead of replaying it
                        if (pausedAt) {
                            startedAt += performance.now() - pausedAt;
                            pausedAt = 0;
                        } else {
                            startedAt = performance.now();
                        }
                        schedule();
                    },
                    destroy: destroy
                };
            });
    }

    global.NTxPlayer = {mount: mount, FORMAT: FORMAT};
})(typeof window !== "undefined" ? window : this);
