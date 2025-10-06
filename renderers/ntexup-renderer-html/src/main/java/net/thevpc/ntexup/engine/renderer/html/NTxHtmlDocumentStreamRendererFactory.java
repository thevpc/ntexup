/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.renderer.html;

import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.nuts.concurrent.NCallableSupport;
import net.thevpc.nuts.util.NMsg;

/**
 * @author vpc
 */
public class NTxHtmlDocumentStreamRendererFactory implements NTxDocumentRendererFactory {
    @Override
    public NCallableSupport<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context) {
        switch (String.valueOf(context.rendererType()).toLowerCase()) {
            case "html":
                return NCallableSupport.ofValid( () -> new NTxHtmlDocumentRenderer(context.engine()));
            default:
                return NCallableSupport.ofInvalid(() -> NMsg.ofPlain("factory"));
        }
    }


}
