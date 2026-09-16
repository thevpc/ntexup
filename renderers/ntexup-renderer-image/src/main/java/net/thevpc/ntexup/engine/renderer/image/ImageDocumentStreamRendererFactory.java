package net.thevpc.ntexup.engine.renderer.image;

import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.text.NMsg;

/**
 * Factory class for creating image document stream renderers (one image per slide).
 */
public class ImageDocumentStreamRendererFactory implements NTxDocumentRendererFactory {

    @Override
    public NScoredCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context) {
        switch (String.valueOf(context.rendererType()).toLowerCase()) {
            case "image":
                return NScoredCallable.ofValid(() -> {
                    NTxDocumentStreamRendererConfig config = new NTxDocumentStreamRendererConfig();
                    return new ImageDocumentRenderer(context.engine(), config);
                });
            default:
                return NScoredCallable.ofInvalid(() -> NMsg.ofP("Invalid renderer type: " + context.rendererType()));
        }
    }
}