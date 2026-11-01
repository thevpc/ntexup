package net.thevpc.ntexup.engine.renderer.dump;

import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;

import java.util.Locale;

/**
 * Factory class for creating TSON "dump" document renderers: instead of a
 * visual output, the renderer captures the tree of primitives effectively
 * painted for each page (used by coding agents to validate rendering).
 */
public class DumpDocumentStreamRendererFactory implements NTxDocumentRendererFactory {

    @Override
    public NScoredCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context) {
        switch (String.valueOf(context.rendererType()).toLowerCase(Locale.ROOT)) {
            case "dump":
            case "tson":
                return NScoredCallable.ofValid(() -> {
                    NTxDocumentStreamRendererConfig config = new NTxDocumentStreamRendererConfig();
                    return new DumpDocumentRenderer(context.engine(), config);
                });
            default:
                return NScoredCallable.ofInvalid(() -> NMsg.ofP("Invalid renderer type: " + context.rendererType()));
        }
    }
}
