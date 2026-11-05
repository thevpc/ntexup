package net.thevpc.ntexup.engine.renderer.htmlstatic;

import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;

import java.util.Locale;

/**
 * Factory for the static HTML exporter: unlike the image-based {@code html}
 * renderer, it produces a folder of paint-stream files plus a canvas player
 * that replays them, keeping page navigation and animation alive without
 * a server.
 */
public class HtmlStaticDocumentStreamRendererFactory implements NTxDocumentRendererFactory {

    @Override
    public NScoredCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context) {
        switch (String.valueOf(context.rendererType()).toLowerCase(Locale.ROOT)) {
            case "html-static":
            case "htmlstatic":
            case "canvas":
                return NScoredCallable.ofValid(() -> {
                    NTxDocumentStreamRendererConfig config = new NTxDocumentStreamRendererConfig();
                    return new HtmlStaticDocumentRenderer(context.engine(), config);
                });
            default:
                return NScoredCallable.ofInvalid(() -> NMsg.ofP("Invalid renderer type: " + context.rendererType()));
        }
    }
}
