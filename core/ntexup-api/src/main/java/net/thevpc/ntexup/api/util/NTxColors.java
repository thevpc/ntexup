package net.thevpc.ntexup.api.util;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.nuts.elem.NElement;

import java.awt.*;

public class NTxColors {
    public static Color[] DEFAULT_CODE_PALETTE = new Color[]{
            new Color(0x26355D),
            new Color(0xAF47D2),
            new Color(0xDE3163),
            new Color(0x2ECC71),
            new Color(0xFF8F00),
            new Color(0xFF7F50),
            new Color(0xFFBF00),
            new Color(0xFFDB00),
            new Color(0x9FE2BF),
            new Color(0x40E0D0),
            new Color(0xCCCCFF),
            new Color(0x6495ED)
    };

    public static Color resolveDefaultColorByIndex(int index, Color[] colors, NTxNode node, NTxNodeRendererContext ctx) {
        if (colors == null || colors.length == 0) {
            NElement v = ctx.resolveVarValue("documentColors", node).orNull();
            if(v!=null && v.isArray()){
                colors=NTxValue.of(v).asColorArray().orNull();
            }
            if(colors==null || colors.length == 0){
                colors = NTxColors.DEFAULT_CODE_PALETTE;
            }
        }
        //index is one indexed!
        if(colors==null || colors.length==0){
            return null;
        }
        if(index==0 || index==1){
            return colors[0];
        }else if(index>0){
            int i = (index-1) % colors.length;
            return colors[i];
        }else {
            // index<0
            index=-index;
            int i = index % colors.length;
            return colors[colors.length-1-i];
        }
    }

    public static float[] hsb(Color c) {
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    }

    public static Color withH(Color c, float h) {
        float[] o = hsb(c);
        return new Color(Color.HSBtoRGB(h, o[1], o[2]));
    }

    public static Color withS(Color c, float s) {
        float[] o = hsb(c);
        return new Color(Color.HSBtoRGB(o[0], s, o[2]));
    }

    public static Color withB(Color c, float b) {
        float[] o = hsb(c);
        return new Color(Color.HSBtoRGB(o[0], o[1], b));
    }
}
