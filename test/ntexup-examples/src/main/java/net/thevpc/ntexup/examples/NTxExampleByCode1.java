/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.style.NTxProps;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.elem2d.NTxAlign;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.nuts.Nuts;

/**
 * @author vpc
 */
public class NTxExampleByCode1 {

    public static void main(String[] args) {
        Nuts.require();
        NTxEngine e = new DefaultNTxEngine();
        NTxDocumentFactory f = e.documentFactory();
        NTxDocument d = f.ofDocument(null)
                .add(f.ofPage()
                        .addChild(
                                f.ofGroup()
                                        .setFontSize(50)
                                        .setFontBold(true)
                                        .addChild(
                                                f.ofRectangle()
                                                        .setPosition(0, 0)
                                                        .setSize(100, 15)
                                                        .setBackgroundColor("#3f4e70")
                                        )
                                        .addChild(
                                                f.ofRectangle()
                                                        .setPosition(0, 15)
                                                        .setSize(100, 1)
                                                        .setBackgroundColor("#21283a")
                                        )
                                        .addChild(
                                                f.ofPlain("Ceci est un Titre pas comme les autres")
                                                        .setPosition(50, 3)
                                                        .setForegroundColor("white")
                                                        .setFontSize(40)
                                                        .setOrigin(NTxAlign.TOP)
                                        )
                                        .addChild(
                                                f.ofCircle()
                                                        .setPosition(70, 50)
                                                        .setSize(20)
                                                        .setForegroundColor("yellow")
                                                        .setBackgroundColor("#ffaa00")
                                                        .setOrigin(NTxAlign.CENTER)
                                        )
                                        .addChild(
                                                f.ofRectangle()
                                                        .setPosition(80, 70)
                                                        .setSize(20, 20)
                                                        .setProperty(NTxProps.preserveShapeRatio())
                                                        .setForegroundColor("green")
                                                        .setBackgroundColor("#555500")
                                                        .setOrigin(NTxAlign.CENTER)
                                        )
                                        .addChild(
                                                f.ofGrid(2, 2)
                                                        .setProperty(NTxProps.rowsWeight(1, 3))
                                                        .addChild(f.ofGlue())
                                                        .addChild(f.ofGlue())
                                                        .addChild(

                                                                f.ofUnorderedList()
                                                                        .addChild(f.ofPlain("Ceci est un premier point"))
                                                                        .addChild(f.ofPlain("Ceci est un second point"))
                                                                        .addChild(f.ofPlain("Ceci est un troisieme point"))
                                                                        .setGridColor("gray")

                                                        )
                                        )
                                        .addChild(
                                                f.ofEquation("x=\\frac{-b \\pm \\sqrt {b^2-4ac}}{2a}")
                                                        .setPosition(10, 80)
                                                        .setOrigin(NTxAlign.LEFT)
                                        )
                        )
                );
        NTxDocumentRenderer renderer = e.newScreenRenderer().get();
        renderer.render(d);
    }
}
