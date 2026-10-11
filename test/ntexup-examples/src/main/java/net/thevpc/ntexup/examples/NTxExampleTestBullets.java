/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.style.NTxProps;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentRenderer;
import net.thevpc.nuts.Nuts;

import java.awt.*;

/**
 * @author vpc
 */
public class NTxExampleTestBullets {

    public static void main(String[] args) {
        Nuts.require();
        NTxEngine e = new DefaultNTxEngine();
        NTxDocumentFactory f = e.documentFactory();
        NTxDocument d = f.ofDocument(null)
                .add(f.ofPage()
                        .addChild(
//                                f.text("Example 1").set(HStyles.fontSize(12))
                                f.ofUnorderedList()
                                        .setProperty(NTxProps.gridColor(Color.GRAY))
                                        .addChild(f.ofPlain("Example 1").setProperty(NTxProps.backgroundColor(Color.CYAN)))
//                                        .add(f.square(80).set(HStyles.backgroundColor(Color.YELLOW)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)))
//                                        .add(f.square(80).set(HStyles.backgroundColor(Color.RED)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)))
//                                        .add(f.square(80).set(HStyles.backgroundColor(Color.BLUE)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)))
//                                        .add(f.text("Example 1").set(HStyles.fontSize(12)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)).set(HStyles.backgroundColor(Color.CYAN)))
//                                        .add(f.text("Example 2").set(HStyles.fontSize(12)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)))
//                                        .add(f.text("Example 3").set(HStyles.fontSize(12)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)))
//                                        .add(f.text("Example 4").set(HStyles.fontSize(12)).set(HStyles.position(HAlign.LEFT))).set(HStyles.origin(HAlign.LEFT))
//                                        .add(f.text("Example 5").set(HStyles.fontSize(12)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)))
//                                        .add(f.text("Example 6").set(HStyles.fontSize(12)).set(HStyles.position(HAlign.LEFT)).set(HStyles.origin(HAlign.LEFT)))
//                        .add(
//                                cc(f)
//                        )
                        ));


        NTxDocumentRenderer renderer = e.newScreenRenderer().get();
        renderer.render(d);
    }

//    private static HGridContainer cc(NtxDocumentFactory f){
//        List<NtxNode> all = new ArrayList<>();
//        int nb=6;
//        for (int i = 0; i < nb; i++) {
//            all.add(f.circle(50)
//                    .set(HStyles.anchor(HAnchor.CENTER))
//                    .set(HStyles.backgroundColor(Color.GREEN))
//                    .set(HStyles.lineColor(Color.GREEN.darker()))
//            );
////            all.add(child.set(HStyles.anchor(HAnchor.LEFT)));
//            if(i%2==0) {
//                all.add(f.rectangle().set(HStyles.backgroundColor(Color.BLUE)).set(HStyles.anchor(HAnchor.LEFT)));
//            }else{
//                all.add(f.rectangle().set(HStyles.backgroundColor(Color.RED)).set(HStyles.anchor(HAnchor.LEFT)));
//            }
//        }
//        return f.grid(2, nb, all.toArray(new NtxNode[0]))
//                .set(HStyles.columnsWeight(1, 20))
//                ;
//    }
}
