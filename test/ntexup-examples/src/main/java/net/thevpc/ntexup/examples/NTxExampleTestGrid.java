/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProps;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;


import java.awt.*;

/**
 * @author vpc
 */
public class NTxExampleTestGrid {

    public static void main(String[] args) {
        Nuts.require();
        NTxEngine e = new DefaultNTxEngine();
        NTxDocumentFactory f = e.documentFactory();
        NTxDocument d = f.ofDocument(null)
                .add(f.ofPage()
                        .addChild(
                                f.ofGrid(3, 2)
                                        .setProperty(NTxPropName.COLUMNS_WEIGHT, NElement.ofDoubleArray(1, 2, 3, 4, 5, 6))
                                        .addChild(f.ofRectangle()
                                                .setProperty(NTxProps.backgroundColor(Color.BLUE))
                                                .setProperty(NTxProps.colspan(4))
                                        )
                                        .addChild(
                                                f.ofRectangle().setProperty(NTxProps.backgroundColor(Color.RED))
                                                        .setProperty(NTxProps.rowspan(4))
                                        )
                                        .addChild(f.ofGrid(4, 4)
                                                .setProperty(NTxProps.colspan(3))
                                                .setProperty(NTxProps.rowspan(2))
                                                .addChild(f.ofRectangle().setProperty(NTxProps.backgroundColor(Color.BLUE))
                                                        .setProperty(NTxProps.colspan(5))
                                                        .setProperty(NTxProps.rowspan(2))
                                                )
                                                .addChild(f.ofRectangle().setProperty(NTxProps.backgroundColor(Color.RED)))
                                                .addChild(f.ofRectangle().setProperty(NTxProps.backgroundColor(Color.GREEN)))
                                                .addChild(f.ofRectangle().setProperty(NTxProps.backgroundColor(Color.YELLOW))))
                        )
                )
//                .add(f.page()
//                                .add(
//
//                                )
//                )
                ;
        NTxDocumentRenderer renderer = e.newScreenRenderer().get();
        renderer.render(d);
    }
}
