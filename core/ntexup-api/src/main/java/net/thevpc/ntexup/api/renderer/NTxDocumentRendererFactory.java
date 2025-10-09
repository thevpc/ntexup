/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.api.renderer;

import net.thevpc.nuts.concurrent.NScoredCallable;

/**
 * @author vpc
 */
public interface NTxDocumentRendererFactory {

    NScoredCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context);

}
