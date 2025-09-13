package net.thevpc.ntexup.engine.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.ntexup.engine.util.NTx2DUtils;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.elem.NUpletElement;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.text.NTextStyle;
import net.thevpc.nuts.text.NTextStyles;
import net.thevpc.nuts.text.NTexts;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NTxTextRendererBuilderImpl implements NTxTextRendererBuilder {

    public String lang;
    public String code;
    public List<NTxRichTextRow> rows = new ArrayList<>();
    public Rectangle2D.Double bounds;
    private Paint defaultColor;
    private NTxEngine engine;
    private NtxFontInfo defaultFont;

    public NTxTextRendererBuilderImpl(NTxEngine engine, Paint defaultColor, NtxFontInfo defaultFont) {
        this.defaultColor = defaultColor;
        this.engine = engine;
        this.defaultFont = defaultFont;
    }

    public void appendNText(String lang, String rawText, NText text, NTxNode node, NTxNodeRendererContext ctx) {
        NTxHighlighterMapper.highlightNutsText(lang, rawText, text, node, ctx, this);
    }

    @Override
    public void appendText(String rawText, NTxTextOptions options, NTxNode node, NTxNodeRendererContext ctx) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }
        if (options == null || !options.isStyled()) {
            appendPlain(rawText, ctx);
            return;
        }
        NTexts nTexts = NTexts.of();
        List<NTextStyle> styles = new ArrayList<>();
        if (options.bold != null && options.bold) {
            styles.add(NTextStyle.bold());
        }
        if (options.italic != null && options.italic) {
            styles.add(NTextStyle.italic());
        }
        if (options.underlined != null && options.underlined) {
            styles.add(NTextStyle.underlined());
        }
        if (options.strikeThrough != null && options.strikeThrough) {
            styles.add(NTextStyle.striked());
        }
        if (options.foregroundColor instanceof Color) {
            styles.add(NTextStyle.foregroundColor(((Color) options.foregroundColor).getRGB()));
        }
        if (options.foregroundColorIndex != null) {
            styles.add(NTextStyle.primary(options.foregroundColorIndex));
        }
        if (options.backgroundColor instanceof Color) {
            styles.add(NTextStyle.backgroundTrueColor(((Color) options.backgroundColor).getRGB()));
        }
        if (options.backgroundColorIndex != null) {
            styles.add(NTextStyle.primary(options.backgroundColorIndex));
        }
        NText nText = nTexts.ofStyled(rawText, NTextStyles.of(styles.toArray(new NTextStyle[0])));
        appendNText("", rawText, nText, node, ctx);
    }

    @Override
    public void appendCustom(String lang, String rawText, NTxTextOptions options, NTxNode node, NTxNodeRendererContext ctx) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }
        NTxTextRendererFlavor hTextRendererFlavor = engine.textRendererFlavor(lang).orElse(null);
        if (hTextRendererFlavor == null) {
            hTextRendererFlavor = engine.textRendererFlavor("").get();
        }
        hTextRendererFlavor.buildText(rawText, options, node, ctx, this);
    }

    public void appendPlain(String text, NTxNodeRendererContext ctx) {
        while (text.startsWith("\n")) {
            this.nextLine();
            text = text.substring(1);
        }
        int end = 0;
        while (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
            end++;
        }

        List<String> a = NStringUtils.split(text, "\n", false, false);

        NTxGraphics g = ctx.graphics();
        for (int j = 0; j < a.size(); j++) {
            if (j > 0) {
                this.nextLine();
            }
            NTxRichTextToken c = new NTxRichTextToken(NTxRichTextTokenType.PLAIN, a.get(j));
            c.textOptions.defaultFont = defaultFont;
            c.textOptions.sr = ctx.sizeRef();
            g.setFont(c.textOptions.resolveFont(ctx.graphics()));
            c.bounds = g.getStringBounds(c.text);
            this.currRow().addToken(c);
        }
        for (int i = 0; i < end; i++) {
            this.nextLine();
        }
    }


    public NTxRichTextRow nextLine() {
        rows.add(new NTxRichTextRow());
        return rows.get(rows.size() - 1);
    }

    public NTxRichTextRow currRow() {
        if (rows.isEmpty()) {
            NTxRichTextRow e = new NTxRichTextRow();
            rows.add(e);
            return e;
        }
        return rows.get(rows.size() - 1);
    }

    public NTxBounds2 computeBound(NTxNodeRendererContext ctx) {
        NTxGraphics g = ctx.graphics();
        Font oldFont = g.getFont();
        bounds = new Rectangle2D.Double(0, 0, 0, 0);
        double maxxY = 0;
        for (int i = 0; i < rows.size(); i++) {
            NTxRichTextRow row = rows.get(i);
            double minX = 0;
            double minY = 0;
            double maxX = 0;
            double maxY = 0;
            for (int j = 0; j < row.tokens.size(); j++) {
                NTxRichTextToken c = row.tokens.get(j);
                c.xOffset = maxX;
                maxX += c.bounds.getWidth();
                maxY = Math.max(maxY, c.bounds.getHeight());
            }
            row.textBounds = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
            if (i == 0) {
                row.yOffset = -row.textBounds.getMinY();
            } else {
                row.yOffset = rows.get(i - 1).yOffset + rows.get(i - 1).textBounds.getHeight();//+ textBounds[i].getMinY();
            }
            Rectangle2D.Double.union(bounds, row.textBounds, bounds);
            maxxY = row.yOffset + row.textBounds.getHeight();
        }
        return new NTxBounds2(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), maxxY);
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void setCode(String rawText) {
        this.code = rawText;
    }

    @Override
    public void addToken(NTxRichTextToken col) {
        rows.get(rows.size() - 1).tokens.add(col);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public void render(NTxNode p, NTxNodeRendererContext rendererContext, NTxBounds2 bgBounds, NTxBounds2 selfBounds) {
        boolean debug = rendererContext.isDebug(p);
        double x = selfBounds.getX();
        double y = selfBounds.getY();
        NTxGraphics g0 = rendererContext.graphics();
        NtxFontInfo fontInfo = NTxValueByName.getFontInfo(p, rendererContext);
        if (fontInfo == null) {
            fontInfo = defaultFont == null ? new NtxFontInfo() : defaultFont.copy();
        } else {
            fontInfo = fontInfo.copy().applyDefaults(defaultFont);
        }
        NTxTextOptions textOptions = new NTxTextOptions()
//                .setFont(NTxValueByName.getFont(p, rendererContext))
                .setForegroundColor(NTxValueByName.getForegroundColor(p, rendererContext, true));


        NTxNodeRendererUtils.paintBackground(p, rendererContext, g0, bgBounds);
        NOptional<NTxShadow> shadowOptional = NTxValueByName.readStyleAsShadow(p, NTxPropName.SHADOW, rendererContext);
        if (shadowOptional.isPresent()) {
            NTxShadow shadow = shadowOptional.get().copy();
            textOptions.setShadow(shadow);
            if (shadow.getColor() == null) {
                if (textOptions.getForegroundColor() instanceof Color) {
                    shadow.setColor(((Color) textOptions.getForegroundColor()).darker());
                } else {
                    shadow.setColor(textOptions.getForegroundColor());
                }
            }
        }
        textOptions.sr = rendererContext.sizeRef();
        NTxTextPath tp = parseNTxTextPath(rendererContext.computePropertyValue(p, "text-path").orNull());
        if (tp == null) {
            for (NTxRichTextRow row : this.rows) {
                for (NTxRichTextToken col : row.tokens) {
                    switch (col.type) {
                        case PLAIN:
                        case STYLED: {
                            NTxTextOptions options2 = textOptions.copy().copyNonNullFrom(col.textOptions);

                            options2.defaultFont = fontInfo;
                            options2.sr = textOptions.sr;
                            options2.resolveFont(rendererContext.graphics(), true);
                            int ascent = g0.getFontMetrics(options2.getComputedFont()).getAscent();
                            g0.drawString(
                                    col.text
                                    , x + col.xOffset
                                    , (y + row.yOffset) + ascent,
                                    options2
                            );
                            break;
                        }
                        case IMAGE_PAINTER: {
                            Rectangle2D b1 = col.bounds;
                            NTxDouble2 b2 = col.imagePainter.size();
                            col.imagePainter.paint(g0, (x + col.xOffset), y + row.yOffset);
                            if (debug) {
                                g0.drawRect(
                                        x + col.xOffset,
                                        y + row.yOffset,
                                        col.bounds.getWidth(),
                                        col.bounds.getHeight()
                                );
                            }
                            break;
                        }
                    }
                }
            }
        } else {
            NTxSizeRef sr = rendererContext.sizeRef();

            textOptions.defaultFont = fontInfo;
            textOptions.sr = rendererContext.sizeRef();
            Font font = textOptions.resolveFont(rendererContext.graphics(), true);
            FontMetrics fm = rendererContext.graphics().getFontMetrics(font);
            double lineHeight = fm.getAscent() + fm.getDescent() + fm.getLeading();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<Point2D> baseCurve = null;
                int baseIndex = 0;
                if (rowIndex < tp.curves.length) {
                    baseCurve = Arrays.stream(tp.curves[rowIndex].points).map(pp -> (Point2D) new Point2D.Double(sr.x(pp.x).get(), sr.y(pp.y).get())).collect(Collectors.toList());
                    baseIndex = 0;
                } else {
                    baseIndex = rowIndex - tp.curves.length + 1;
                    baseCurve = Arrays.stream(tp.curves[tp.curves.length - 1].points).map(pp -> (Point2D) new Point2D.Double(sr.x(pp.x).get(), sr.y(pp.y).get())).collect(Collectors.toList());
                }

                NTxRichTextRow row = rows.get(rowIndex);
                // get curve for this row (translate, scale, zoom)

                List<Point2D> rowCurve = computeRowCurve(baseCurve, baseIndex, lineHeight, 1);
                if(debug) {
                    rendererContext.graphics().setColor(Color.RED);
                    rendererContext.graphics().drawPolyline(
                            rowCurve.stream().mapToDouble(pp -> pp.getX()).toArray(),
                            rowCurve.stream().mapToDouble(pp -> pp.getY()).toArray(),
                            rowCurve.size()
                    );
                }

                List<Double> segmentLengths = NTx2DUtils.computeSegmentLengths(rowCurve);

                // render tokens along this row curve
                renderRowAlongCurve(row, rowCurve, segmentLengths, textOptions, rendererContext, fontInfo);
            }
        }
        rendererContext.drawContour();
    }

    public void renderRowAlongCurve(NTxRichTextRow row, List<Point2D> curvePoints, List<Double> segmentLengths, NTxTextOptions baseOptions, NTxNodeRendererContext rendererContext, NtxFontInfo fontInfo) {
        double s = 0; // distance along curve
        for (NTxRichTextToken token : row.tokens) {
            token.textOptions.defaultFont = fontInfo;
            token.textOptions.sr = rendererContext.sizeRef();
            token.textOptions.resolveFont(rendererContext.graphics(), true);

            double tokenWidth = computeTokenWidth(token, rendererContext); // text advance or image width

            Point2D pos = NTx2DUtils.getPointAtLength(curvePoints, segmentLengths, s + tokenWidth / 2);
            double angle = NTx2DUtils.getTangentAngle(curvePoints, segmentLengths, s + tokenWidth / 2);

            AffineTransform at = new AffineTransform();
            at.translate(pos.getX(), pos.getY());
            at.rotate(angle);
            at.translate(-tokenWidth / 2, 0);

            if (token.type == NTxRichTextTokenType.PLAIN || token.type == NTxRichTextTokenType.STYLED) {
                NTxTextOptions options2 = baseOptions.copy().copyNonNullFrom(token.textOptions);
                options2.defaultFont = fontInfo;
                options2.sr = rendererContext.sizeRef();
                options2.resolveFont(rendererContext.graphics(), true);
                drawGlyphVectorAlongCurve(token.text, curvePoints, segmentLengths, s, options2, rendererContext);
            } else if (token.type == NTxRichTextTokenType.IMAGE_PAINTER) {
                NTxGraphics g = rendererContext.graphics();
                Graphics2D g2d = g.graphics2D();
                AffineTransform old = g2d.getTransform();
                g2d.transform(at);
                token.imagePainter.paint(g, 0, 0);
                g2d.setTransform(old);
            }

            s += tokenWidth; // move along the curve for the next token
        }
    }

    private void drawGlyphVectorAlongCurve(
            String text,
            List<Point2D> curvePoints,
            List<Double> segmentLengths,
            double tokenStartS,
            NTxTextOptions options,
            NTxNodeRendererContext rendererContext) {

        if (text == null || text.isEmpty()) return;

        Graphics2D g = rendererContext.graphics().graphics2D();
        Font font = options.getComputedFont();
        GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), text);

        Paint oldPaint = g.getPaint();
        Font oldFont = g.getFont();
        g.setFont(font);
        g.setPaint(options.getForegroundColor());

        for (int i = 0; i < gv.getNumGlyphs(); i++) {
            Shape glyph = gv.getGlyphOutline(i);
            Point2D glyphPos = gv.getGlyphPosition(i);
            Point2D pos = NTx2DUtils.getPointAtLength(curvePoints, segmentLengths, tokenStartS + glyphPos.getX());
            double angle = NTx2DUtils.getTangentAngle(curvePoints, segmentLengths, tokenStartS + glyphPos.getX());
            AffineTransform at = new AffineTransform();
            at.translate(pos.getX(), pos.getY());
            at.rotate(angle);
            at.translate(-gv.getGlyphPosition(i).getX(), 0);
            g.fill(at.createTransformedShape(glyph));
        }

        g.setFont(oldFont);
        g.setPaint(oldPaint);
    }

    private double computeTokenWidth(NTxRichTextToken token, NTxNodeRendererContext rendererContext) {
        if (token.type == NTxRichTextTokenType.PLAIN || token.type == NTxRichTextTokenType.STYLED) {
            Font font = token.textOptions.getComputedFont();
            FontMetrics fm = rendererContext.graphics().getFontMetrics(font);
            return fm.stringWidth(token.text); // approximate advance along baseline
        } else if (token.type == NTxRichTextTokenType.IMAGE_PAINTER) {
            NTxDouble2 size = token.imagePainter.size(); // width of image
            return size.getX();
        } else {
            return 0;
        }
    }

    public List<Point2D> computeRowCurve(List<Point2D> baseCurve, int rowIndex, double lineHeight, double zoomFactor) {
        List<Point2D> rowCurve = new ArrayList<>();
        double scale = /*1.0 + rowIndex */ zoomFactor;

        for (Point2D p : baseCurve) {
            double x = p.getX();
            double y = p.getY() + rowIndex * lineHeight; // vertical offset
            x *= scale;
            y *= scale;
            rowCurve.add(new Point2D.Double(x, y));
        }
        return rowCurve;
    }

    private static NTxTextPath parseNTxTextPath(NElement e) {
        if (e == null) {
            return null;
        }
        if (e.isNumber()) {
            return null;
        }
        List<NTxTextPathCurve> curvesOk = new ArrayList<>();
        if (e.isNamedUplet()) {
            NOptional<NTxTextPathCurve> cc = parseCurve(e);
            if (cc.isPresent()) {
                curvesOk.add(cc.get());
            }
        } else if (e.isObject()) {
            NOptional<NTxPoint2D[]> uu = NTxValue.of(e).asPoint2DArray();
            boolean found = false;
            if (uu.isPresent()) {
                NTxPoint2D[] g = uu.get();
                if (g.length >= 2) {
                    found = true;
                    curvesOk.add(new NTxTextPathCurve(g));
                }
            }
            if (!found) {
                for (NElement child : e.asObject().get().children()) {
                    NOptional<NTxTextPathCurve> cc = parseCurve(child);
                    if (cc.isPresent()) {
                        curvesOk.add(cc.get());
                    }
                }
            }
        } else if (e.isArray()) {
            NOptional<NTxPoint2D[]> uu = NTxValue.of(e).asPoint2DArray();
            boolean found = false;
            if (uu.isPresent()) {
                NTxPoint2D[] g = uu.get();
                if (g.length >= 2) {
                    found = true;
                    curvesOk.add(new NTxTextPathCurve(g));
                }
            }
            if (!found) {
                for (NElement child : e.asArray().get().children()) {
                    NOptional<NTxTextPathCurve> cc = parseCurve(child);
                    if (cc.isPresent()) {
                        curvesOk.add(cc.get());
                    }
                }
            }
        } else {
            //error here
        }
        if (!curvesOk.isEmpty()) {
            NTxTextPath pp = new NTxTextPath();
            pp.curves = curvesOk.toArray(new NTxTextPathCurve[0]);
            return pp;
        }
        return null;
    }

    private static NOptional<NTxTextPathCurve> parseCurve(NElement child) {
        if (child.isNamedUplet()) {
            NUpletElement u = child.asUplet().get();
            String name = u.name().get();
            switch (name) {
                case "points":
                case "curve": {
                    NOptional<NTxPoint2D[]> uu = NTxValue.of(u).asPoint2DArray();
                    if (uu.isPresent()) {
                        NTxPoint2D[] g = uu.get();
                        if (g.length >= 2) {
                            return NOptional.of(new NTxTextPathCurve(g));
                        }
                    }
                    return NOptional.ofNamedEmpty("missing curve");
                }
                case "arc": {

                    Double cx = null;
                    Double cy = null;
                    Double rx = null;
                    Double ry = null;
                    Double startAngle = null;
                    Double endAngle = null;
                    Integer points = null;

                    for (NElement param : u.params()) {
                        if (param.isNamedPair()) {
                            NPairElement p = param.asPair().get();
                            switch (p.key().asStringValue().get()) {
                                case "cx":
                                case "x": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        cx = g.get();
                                    }
                                    break;
                                }
                                case "y":
                                case "cy": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        cy = g.get();
                                    }
                                    break;
                                }
                                case "rx": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        rx = g.get();
                                    }
                                    break;
                                }
                                case "ry": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        ry = g.get();
                                    }
                                    break;
                                }
                                case "startAngle":
                                case "from": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        startAngle = g.get();
                                    }
                                    break;
                                }
                                case "toAngle":
                                case "to": {
                                    NOptional<Double> g = p.value().asDoubleValue();
                                    if (g.isPresent()) {
                                        endAngle = g.get();
                                    }
                                    break;
                                }
                                case "count": {
                                    NOptional<Integer> g = p.value().asIntValue();
                                    if (g.isPresent()) {
                                        points = g.get();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (cx == null && cy == null) {
                        return NOptional.ofNamedEmpty("missing arc");
                    }
                    if (rx == null && rx == null) {
                        return NOptional.ofNamedEmpty("missing arc");
                    }
                    if (cx == null) {
                        cx = cy;
                    }
                    if (cy == null) {
                        cy = cx;
                    }
                    if (rx == null) {
                        rx = ry;
                    }
                    if (ry == null) {
                        ry = rx;
                    }
                    if (startAngle == null) {
                        startAngle = 0.0;
                    }
                    if (endAngle == null) {
                        endAngle = 360.0;
                    }
                    if (points == null) {
                        points = 360;
                    }
                    if (points <= 2) {
                        points = 100;
                    }
                    List<NTxPoint2D> arc = NTxUtils.createArc2(cx, cy, rx, ry, startAngle, endAngle, points);
                    Collections.reverse(arc);
                    return NOptional.of(new NTxTextPathCurve(arc.toArray(new NTxPoint2D[0])));
                }
            }
        } else if (child.isArray() || child.isObject()) {
            NOptional<NTxPoint2D[]> uu = NTxValue.of(child).asPoint2DArray();
            if (uu.isPresent()) {
                NTxPoint2D[] g = uu.get();
                if (g.length >= 2) {
                    return NOptional.of(new NTxTextPathCurve(g));
                }
            }
            return NOptional.ofNamedEmpty("missing array");
        }
        return NOptional.ofNamedEmpty("missing curve");
    }

    private static class NTxTextPathCurve {
        private NTxPoint2D[] points;

        public NTxTextPathCurve(NTxPoint2D[] points) {
            this.points = points;
        }
    }

    private static class NTxTextPath {
        private NTxTextPathCurve[] curves;
    }
}
