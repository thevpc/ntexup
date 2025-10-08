package net.thevpc.ntexup.engine.renderer.pdf;

import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.nuts.concurrent.NScorableCallable;
import net.thevpc.nuts.text.NMsg;

/**
 * Factory class for creating PDF document stream renderers.
 */
public class PdfDocumentStreamRendererFactory implements NTxDocumentRendererFactory {

    @Override
    public NScorableCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context) {
        switch (String.valueOf(context.rendererType()).toLowerCase()) {
            case "pdf":
                return NScorableCallable.ofValid( () -> {
                    NTxDocumentStreamRendererConfig config = new NTxDocumentStreamRendererConfig();
                    return new PdfDocumentRenderer(context.engine(), config);
                });
            default:
                return NScorableCallable.ofInvalid(() -> NMsg.ofPlain("Invalid renderer type: " + context.rendererType()));
        }
    }
}
