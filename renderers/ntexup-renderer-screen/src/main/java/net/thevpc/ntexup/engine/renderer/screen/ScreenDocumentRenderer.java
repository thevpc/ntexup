/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererBase;
import net.thevpc.ntexup.api.renderer.NTxDocumentScreenRenderer;

import net.thevpc.ntexup.api.renderer.NTxDocumentRendererSupplier;
import net.thevpc.ntexup.api.renderer.NTxDocumentView;

/**
 * @author vpc
 */
public class ScreenDocumentRenderer extends NTxDocumentRendererBase implements NTxDocumentScreenRenderer {

    public ScreenDocumentRenderer(NTxEngine engine) {
        super(engine);
    }

    @Override
    public NTxDocumentView renderSupplier(NTxDocumentRendererSupplier document) {
        DocumentView dv = new DocumentView(document, engine, eventListenerDelegate, this);
        return dv;
    }
}
