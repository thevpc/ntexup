/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.container;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
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
public class NTxColumnBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.COLUMN)
                .alias("col")
                .parseParam().matchesNamedPair(NTxPropName.ROWS).then()
                .parseParam((info, buildContext) -> {
                    NElement p = info.peek();
                    if(p.isInt()) {
                        info.read();
                        info.node().setProperty(NTxProp.of(NTxPropName.ROWS,p));
                        return true;
                    }else{
                        return false;
                    }
                })
                .afterParsingAllParams((NTxAllArgumentReader info, NTxNodeBuilderContext buildContext)->{
                    info.node().setProperty(NTxProp.of(NTxPropName.COLUMNS,NElement.ofInt(1)));
                })
                .selfBounds((rendererContext) -> selfBounds(rendererContext))
                .renderComponent((ctx) -> renderMain(ctx))
                ;
    }


    public NTxBounds2D selfBounds(NTxNodeRendererContext rendererContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxNode node = rendererContext.node();
        NTxBounds2D expectedBounds = rendererContext.defaultSelfBounds();
        NTxGridRendererHelper h = new NTxGridRendererHelper(node.children());
        return h.computeBound(node, rendererContext, expectedBounds);
    }

    public void renderMain(NTxNodeRendererContext ctx) {
        ctx = ctx.withDefaultStyles(defaultStyles);
        NTxBounds2D expectedBounds = ctx.selfBounds();
        NTxGridRendererHelper h = new NTxGridRendererHelper(ctx.node().children());
        h.render(ctx, expectedBounds);
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
