package net.thevpc.ntexup.extension.scoreboard;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.scoreboard.data.ScoreboardDataParser;
import net.thevpc.ntexup.extension.scoreboard.model.ScoreboardModel;
import net.thevpc.ntexup.extension.scoreboard.render.ScoreboardRenderer;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

import java.util.Locale;

/**
 * {@code scoreboard} — an animated, customizable ranking component.
 *
 * <h2>Minimal usage</h2>
 * <pre>{@code
 * import("scoreboard")
 * scoreboard(
 *     title: "ENISo IoT Challenge",
 *     scores: [
 *         {name: "DagDeg", sub-name: "Vers", score: 62.5},
 *         {name: "Drive X", sub-name: "Alert Guard", score: 49}
 *     ]
 * )
 * }</pre>
 *
 * <h2>Data sources</h2>
 * <ul>
 *     <li>inline TSON (list of objects, list of rows, or a name→score map),</li>
 *     <li>inline CSV text,</li>
 *     <li>an external {@code .tson}, {@code .json}, {@code .yaml} or
 *         {@code .csv} file passed through {@code file:}.</li>
 * </ul>
 */
public class NTxScoreboardBuilder implements NTxNodeBuilder {

    private final ScoreboardRenderer renderer = new ScoreboardRenderer();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("scoreboard")
                .alias("score-board", "leaderboard", "scores")
                .parseParam()
                    .matchesNamedPair(
                            // data
                            "scores", "data", "entries", "items", "rows", "results", "ranking",
                            "file", "data-file", "datafile", "src", "source",
                            "format", "data-format", "dataformat",
                            // metadata
                            "title", "subtitle", "sub-title", "heading", "caption",
                            "icon", "logo", "image",
                            // theme & colours
                            "theme", "variant", "palette", "colors", "row-colors", "bar-colors",
                            "accent", "accent-color", "bar-color", "barcolor",
                            "track-color", "trackcolor", "bar-background", "bar-background-color",
                            "title-color", "titlecolor", "subtitle-color", "subtitlecolor",
                            "name-color", "namecolor", "score-color", "scorecolor",
                            "rank-color", "rankcolor",
                            // behaviour
                            "duration", "animation-duration", "reveal-duration",
                            "stagger", "row-stagger", "delay",
                            "show-title", "title-visible", "show-subtitle", "subtitle-visible",
                            "show-rank", "rank", "show-score", "show-value", "show-values", "show-unit",
                            "max", "max-score", "maximum",
                            "gap", "row-gap", "radius", "corner-radius", "row-radius",
                            "limit", "top", "max-rows",
                            "sort", "sorted", "pulse", "glow", "pulse-duration", "glow-duration",
                            "score-format", "number-format", "format-score",
                            "unit", "suffix", "font", "font-name"
                    )
                    .end()
                .renderComponent(this::render);
    }

    public void render(NTxRendererContext ctx) {
        NTxNode node = ctx.node();
        String signature = dataSignature(node);
        Cache cache = node.getAndSetUserObject(Cache.class, false, () -> new Cache(signature, loadModel(ctx, node)))
                .orElseGet(() -> new Cache(signature, loadModel(ctx, node)));
        if (!signature.equals(cache.signature)) {
            cache = new Cache(signature, loadModel(ctx, node));
            node.setUserObject(Cache.class.getName(), cache);
        }
        ScoreboardModel model = cache.model;
        String title = firstString(node, "title", "heading");
        model.setTitle(title != null ? title : cache.baseTitle);
        String subtitle = firstString(node, "subtitle", "sub-title", "caption");
        model.setSubtitle(subtitle != null ? subtitle : cache.baseSubtitle);
        renderer.render(ctx, model);
    }

    /**
     * Cheap identity-based signature of the data-bearing properties. The node
     * property elements are stable while the deck is not edited, so this keeps
     * the parsed model (and file I/O) out of the per-frame animation path.
     */
    private static String dataSignature(NTxNode node) {
        StringBuilder sb = new StringBuilder();
        for (String name : new String[]{"scores", "data", "entries", "items", "rows", "results", "ranking"}) {
            NElement e = NTxValue.ofProp(node, name).asElement().orNull();
            sb.append(name).append('#').append(e == null ? 0 : System.identityHashCode(e)).append(';');
        }
        for (String name : new String[]{"file", "data-file", "datafile", "src", "source", "format", "data-format", "dataformat"}) {
            NElement e = NTxValue.ofProp(node, name).asElement().orNull();
            sb.append(name).append('=').append(e == null ? "" : stringValue(e)).append(';');
        }
        return sb.toString();
    }

    private static String stringValue(NElement e) {
        if (e == null || e.isNull()) {
            return "";
        }
        if (e.isAnyString()) {
            return e.asStringValue().orElse("");
        }
        if (e.isName()) {
            return e.asNameValue().orElse("");
        }
        if (e.isNumber()) {
            return String.valueOf(e.asNumberValue().orElse(null));
        }
        return String.valueOf(System.identityHashCode(e));
    }

    private ScoreboardModel loadModel(NTxRendererContext ctx, NTxNode node) {
        ScoreboardModel model = null;
        try {
            NElement inline = firstElement(node, "scores", "data", "entries", "items", "rows", "results", "ranking");
            if (inline != null && !inline.isNull()) {
                if (inline.isAnyString()) {
                    String text = inline.asStringValue().orElse("").trim();
                    if (looksLikeInlineData(text)) {
                        model = parseInlineText(text, readFormat(node));
                    } else if (!text.isEmpty()) {
                        model = loadFile(ctx, text, readFormat(node));
                    }
                } else {
                    model = ScoreboardDataParser.parseElement(inline);
                }
            }
            if (model == null) {
                String file = firstString(node, "file", "data-file", "datafile", "src", "source");
                if (file != null && !file.trim().isEmpty()) {
                    model = loadFile(ctx, file.trim(), readFormat(node));
                }
            }
        } catch (RuntimeException ex) {
            ctx.log(NMsg.ofC("[scoreboard] cannot load scores: %s", ex).asWarning(), ctx.source());
        }
        if (model == null) {
            model = new ScoreboardModel();
        }

        return model.reindex();
    }

    private ScoreboardModel parseInlineText(String text, String format) {
        if ("csv".equals(format)) {
            return ScoreboardDataParser.parseCsv(text);
        }
        char first = text.isEmpty() ? 0 : text.charAt(0);
        if (first == '{' || first == '[') {
            try {
                return ScoreboardDataParser.parseElement(NElementReader.ofTson().read(text));
            } catch (RuntimeException ex) {
                // fall back to CSV below
            }
        }
        if (looksLikeCsv(text)) {
            return ScoreboardDataParser.parseCsv(text);
        }
        // Last chance: a single "name,score" pair.
        return ScoreboardDataParser.parseCsv(text);
    }

    private ScoreboardModel loadFile(NTxRendererContext ctx, String rawPath, String format) {
        NPath path = ScoreboardPaths.resolve(ctx, rawPath);
        if (path == null || !path.isRegularFile()) {
            ctx.log(NMsg.ofC("[scoreboard] score file not found: %s", rawPath).asWarning(), ctx.source());
            return new ScoreboardModel();
        }
        String name = path.name() == null ? "" : path.name().toLowerCase(Locale.ROOT);
        String fmt = format;
        if (fmt == null || fmt.isEmpty() || "auto".equals(fmt)) {
            if (name.endsWith(".csv")) {
                fmt = "csv";
            } else if (name.endsWith(".tsv") || name.endsWith(".tab")) {
                fmt = "tsv";
            } else if (name.endsWith(".json")) {
                fmt = "json";
            } else if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                fmt = "yaml";
            } else if (name.endsWith(".tson") || name.endsWith(".ntx")) {
                fmt = "tson";
            } else {
                fmt = "tson";
            }
        }
        fmt = fmt.toLowerCase(Locale.ROOT);
        try {
            String text = path.readString();
            if ("csv".equals(fmt) || "tsv".equals(fmt)) {
                return ScoreboardDataParser.parseCsv(text);
            }
            NElement root;
            switch (fmt) {
                case "json":
                    root = NElementReader.ofJson().read(text);
                    break;
                case "yaml":
                case "yml":
                    root = NElementReader.ofYaml().read(text);
                    break;
                case "tson":
                default:
                    root = NElementReader.ofTson().read(text);
                    break;
            }
            if (root == null || root.isNull()) {
                return new ScoreboardModel();
            }
            return ScoreboardDataParser.parseElement(root);
        } catch (RuntimeException ex) {
            ctx.log(NMsg.ofC("[scoreboard] cannot read score file %s: %s", path, ex).asWarning(), ctx.source());
            // A CSV file with a misleading extension is common enough to try.
            try {
                return ScoreboardDataParser.parseCsv(path.readString());
            } catch (RuntimeException ignored) {
                return new ScoreboardModel();
            }
        }
    }

    private static boolean looksLikeInlineData(String text) {
        if (text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        if (first == '{' || first == '[') {
            return true;
        }
        return looksLikeCsv(text);
    }

    private static boolean looksLikeCsv(String text) {
        return text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0
                || text.indexOf(',') >= 0 || text.indexOf(';') >= 0 || text.indexOf('\t') >= 0;
    }

    private static String readFormat(NTxNode node) {
        String s = firstString(node, "format", "data-format", "dataformat");
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private static NElement firstElement(NTxNode node, String... names) {
        for (String name : names) {
            NElement e = NTxValue.ofProp(node, name).asElement().orNull();
            if (e != null && !e.isNull()) {
                return e;
            }
        }
        return null;
    }

    private static String firstString(NTxNode node, String... names) {
        for (String name : names) {
            NElement e = NTxValue.ofProp(node, name).asElement().orNull();
            if (e != null && !e.isNull()) {
                if (e.isAnyString()) {
                    String s = e.asStringValue().orElse("");
                    if (!s.trim().isEmpty()) {
                        return s;
                    }
                } else if (e.isName()) {
                    String s = e.asNameValue().orElse("");
                    if (!s.trim().isEmpty()) {
                        return s;
                    }
                }
            }
        }
        return null;
    }

    private static final class Cache {
        final String signature;
        final ScoreboardModel model;
        final String baseTitle;
        final String baseSubtitle;

        Cache(String signature, ScoreboardModel model) {
            this.signature = signature;
            this.model = model;
            this.baseTitle = model.getTitle();
            this.baseSubtitle = model.getSubtitle();
        }
    }
}
