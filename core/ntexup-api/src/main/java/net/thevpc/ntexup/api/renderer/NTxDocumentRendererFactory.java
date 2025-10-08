/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.api.renderer;

import net.thevpc.nuts.concurrent.NScorableCallable;

/**
 * @author vpc
 */
public interface NTxDocumentRendererFactory {

    NScorableCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context);

}
