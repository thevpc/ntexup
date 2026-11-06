package net.thevpc.ntexup.extension.scoreboard;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;

/**
 * Resolves scoreboard assets (data files, icons) with a few sensible
 * fallbacks.
 *
 * <p>Nodes that live inside a theme {@code @define} template can carry the
 * <em>theme</em> file as their source, so a path written in a slide
 * ({@code icon: "../../images/logo.png"}) may resolve against the wrong
 * directory. The page source and the document source are tried as fallbacks,
 * which is what users naturally expect.</p>
 */
public final class ScoreboardPaths {

    private ScoreboardPaths() {
    }

    public static NPath resolve(NTxRendererContext ctx, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String value = raw.trim();
        NElement expression = NElement.ofString(value);
        NElement evaluated;
        try {
            evaluated = ctx.evalExpression(expression).orNull();
        } catch (RuntimeException ex) {
            evaluated = null;
        }
        if (evaluated == null) {
            evaluated = expression;
        }

        NPath first = tryResolve(ctx, evaluated);
        if (isFile(first)) {
            return first;
        }
        NPath page = tryResolve(ctx, evaluated, ctx.compiledPage() == null ? null : ctx.compiledPage().source());
        if (isFile(page)) {
            return page;
        }
        NPath document = tryResolve(ctx, evaluated, ctx.document() == null ? null : ctx.document().source());
        if (isFile(document)) {
            return document;
        }
        // Absolute path or a path relative to the process working directory.
        try {
            NPath direct = NPath.of(value);
            if (isFile(direct)) {
                return direct;
            }
        } catch (RuntimeException ignored) {
            // not a valid path
        }
        return first != null ? first : (page != null ? page : document);
    }

    private static NPath tryResolve(NTxRendererContext ctx, NElement evaluated) {
        try {
            return ctx.resolvePath(evaluated);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static NPath tryResolve(NTxRendererContext ctx, NElement evaluated, Object source) {
        if (source == null) {
            return null;
        }
        try {
            return NTxUtils.resolvePath(evaluated, source);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean isFile(NPath path) {
        try {
            return path != null && path.isRegularFile();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
