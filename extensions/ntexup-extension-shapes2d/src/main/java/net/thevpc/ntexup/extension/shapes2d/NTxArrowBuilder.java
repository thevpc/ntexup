package net.thevpc.ntexup.extension.shapes2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

import java.awt.*;
import java.awt.geom.GeneralPath;


/**
 *
 */
public class NTxArrowBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.ARROW)
                .parseParam().matchesNamedPair(NTxPropName.WIDTH, NTxPropName.HEIGHT,"base","hat").end()
                .renderComponent(this::render)
                ;
    }


    public void render(NTxNodeRendererContext rendererContext) {
        NTxNode node=rendererContext.node();
        NTxBounds2D b = rendererContext.selfBounds(node, null, null);
        double x = b.getX();
        double y = b.getY();
        double width = b.getWidth();
        double height = b.getHeight();

        Paint color = rendererContext.getForegroundColor(node, true);
        NTxPoint2D base = NTxValue.of(node.getPropertyValue("base")).asPoint2D().orElse(null);
        if(base==null){
            Double base3 = NTxValue.of(node.getPropertyValue("base")).asDouble().orElse(null);
            if(base3==null){
                base=new NTxPoint2D(80, 20);
            }else{
                base=new NTxPoint2D(base3, base3);
            }
        }
        NTxPoint2D hat = NTxValue.of(node.getPropertyValue("hat")).asPoint2D().orElse(null);
        if(hat==null){
            Double base3 = NTxValue.of(node.getPropertyValue("hat")).asDouble().orElse(null);
            if(base3==null){
                hat=new NTxPoint2D(-1, -1);
            }else{
                hat=new NTxPoint2D(base3, base3);
            }
        }
        if(hat.y<0){
            hat.y=15;
        }
        if(hat.x<0){
            hat.x=0;
        }
        double baseH=base.y*height/100.0;
        double baseW=base.x*width/100.0;
        double hatH=hat.y*height/100.0;
        double hatW=hat.x*width/100.0;

        NTxGraphics g = rendererContext.graphics();
        g.setPaint(color);
        GeneralPath arrow = new GeneralPath();
        arrow.moveTo(x, y+height/2);
        arrow.lineTo(x, y+height/2-baseH/2);
        arrow.lineTo(x+baseW, y+height/2-baseH/2);
        arrow.lineTo(x+baseW-hatW, y+height/2-baseH/2-hatH);
        arrow.lineTo(x+width, y+height/2);
        arrow.lineTo(x+baseW-hatW, y+height/2+baseH/2+hatH);
        arrow.lineTo(x+baseW, y+height/2+baseH/2);
        arrow.lineTo(x, y+height/2+baseH/2);
        arrow.closePath();
        g.draw(arrow);
        g.fill(arrow);
        rendererContext.drawContour();
    }

}
