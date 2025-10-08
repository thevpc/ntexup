/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.nuts.concurrent.NScorableCallable;
import net.thevpc.nuts.text.NMsg;

/**
 * @author vpc
 */
public class ScreenDocumentStreamRendererFactory implements NTxDocumentRendererFactory {

    @Override
    public NScorableCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context) {
        switch (String.valueOf(context.rendererType()).toLowerCase()) {
            case "screen":
                return NScorableCallable.ofValid( () -> new ScreenDocumentRenderer(context.engine()));
            default:
                return NScorableCallable.ofInvalid(() -> NMsg.ofPlain("factory"));
        }
    }

}
