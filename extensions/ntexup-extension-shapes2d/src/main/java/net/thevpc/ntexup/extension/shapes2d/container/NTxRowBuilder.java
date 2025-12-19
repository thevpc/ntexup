/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.container;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.parser.NTxAllArgumentReader;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes2d.container.util.NTxGridRendererHelper;
import net.thevpc.nuts.elem.NElement;

/**
 * @author vpc
 */
public class NTxRowBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.ROW)
                .parseParam().matchesNamedPair(NTxPropName.COLUMNS).then()
                .parseParam((info, buildContext) -> {
                    NElement p = info.peek();
                    if(p.isInt()) {
                        info.read();
                        info.node().setProperty(NTxProp.of(NTxPropName.COLUMNS,p));
                        return true;
                    }else{
                        return false;
                    }
                })
                .afterParsingAllParams((NTxAllArgumentReader info, NTxNodeBuilderContext buildContext)->{
                    info.node().setProperty(NTxProp.of(NTxPropName.ROWS,NElement.ofInt(1)));
                })
                .selfBounds(this::selfBounds)
                .renderComponent(this::renderMain)
                ;
    }


    public NTxBounds2 selfBounds(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2 expectedBounds = rendererContext.defaultSelfBounds();
//        HGraphics g = rendererContext.graphics();
//        g.setColor(Color.RED);
//        g.drawRect(expectedBounds);
        NTxGridRendererHelper h = new NTxGridRendererHelper(node.children());
        return h.computeBound(node, rendererContext, expectedBounds);
    }

    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2 expectedBounds = rendererContext.selfBounds();
        NTxGridRendererHelper h = new NTxGridRendererHelper(rendererContext.node().children());
        h.render(rendererContext, expectedBounds);
    }


//    @Override
//    protected void processImplicitStyles(NTxArgumentParseInfo info) {
//        switch (info.getUid()) {
//            case "vgrid":
//            case "column": {
//                if(info.node().getProperty(NTxPropName.COLUMNS).isNotPresent()) {
//                    info.node().setProperty(NTxPropName.COLUMNS, NElement.ofInt(1));
//                }
//                if(info.node().getProperty(NTxPropName.ROWS).isNotPresent()) {
//                    info.node().setProperty(NTxPropName.ROWS, NElement.ofInt(-1));
//                }
//                break;
//            }
//            case "hgrid":
//            case "row": {
//                if(info.node().getProperty(NTxPropName.COLUMNS).isNotPresent()) {
//                    info.node().setProperty(NTxPropName.COLUMNS, NElement.ofInt(-1));
//                }
//                if(info.node().getProperty(NTxPropName.ROWS).isNotPresent()) {
//                    info.node().setProperty(NTxPropName.ROWS, NElement.ofInt(1));
//                }
//                break;
//            }
//        }
//    }

}
