package net.thevpc.ntexup.engine.renderer.ntxp;

import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;

import java.util.Locale;

/**
 * Factory for the {@code .ntxp} container: it wraps the html-static bundle in an
 * archive that names itself, so a player can tell an ntexup container from any
 * other zip before it unpacks anything.
 */
public class NtxpDocumentRendererFactory implements NTxDocumentRendererFactory {

    @Override
    public NScoredCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context) {
        switch (String.valueOf(context.rendererType()).toLowerCase(Locale.ROOT)) {
            case "ntxp":
                return NScoredCallable.ofValid(() -> {
                    NTxDocumentStreamRendererConfig config = new NTxDocumentStreamRendererConfig();
                    return new NtxpDocumentRenderer(context.engine(), config);
                });
            default:
                return NScoredCallable.ofInvalid(() -> NMsg.ofP("Invalid renderer type: " + context.rendererType()));
        }
    }
}
