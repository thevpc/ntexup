/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.net.nscoreboard.util;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 *
 * @author vpc
 */
public class Utils {
//    public static ModelPoint nextLinePoint(ModelPoint source, ModelPoint target, double speed) {
//        double distance = source.distance(target);
//        if (distance <= speed) {
//            return target;
//        }
//        double sx = source.getX();
//        double sy = source.getY();
//        double tx = target.getX();
//        double ty = target.getY();
////        double dx = tx - sx;
////        double dy = ty - sy;
////        double theta=Math.atan2(dx, dy);
////        double dTheta=Math.toDegrees(theta);
////        double pTheta=theta/Math.PI;
//        //double distance=Math.sqrt((dx-sx)*(dx-sx)+(dy-sy)*(dy-sy));
//        double nx = (tx - sx) / distance * speed + sx;
//        double ny = (ty - sy) / distance * speed + sy;
//
//        return new ModelPoint(nx, ny);
//    }
    public static String firstNonBlank(String ...all) {
        for (String s : all) {
            if(s!=null && s.length()>0){
                return s;
            }
        }
        return "";
    }

    public static Font prepareFont(String font, double w, double h) {
        float size = 1;
        Font f = new Font(font, Font.PLAIN, (int) size);
        int[] last = null;
        Font lastFont = null;
        if (h <= 0 && w <= 0) {
            return f;
        }
        int extra=10;
        while (true) {
            f=f.deriveFont(Font.PLAIN, size);
            int[] curr = fontSize(f);
            if (h > 0 && curr[1] > h+extra) {
                if (last == null) {
                    return f;
                }
                return lastFont;
            }
            if (w > 0 && curr[0] > w+extra) {
                if (last == null) {
                    return f;
                }
                return lastFont;
            }
            last = curr;
            lastFont = f;
            size++;
        }
    }

    public static int[] fontSize(Font f) {
        // Create an offscreen BufferedImage
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        // Get the Graphics context from the BufferedImage
        Graphics graphics = image.getGraphics();

        // Set the font on the Graphics context
        graphics.setFont(f);

        // Obtain the FontMetrics object
        FontMetrics fontMetrics = graphics.getFontMetrics();

        // Use the FontMetrics object
//        System.out.println("Ascent: " + fontMetrics.getAscent());
//        System.out.println("Descent: " + fontMetrics.getDescent());
//        System.out.println("Height: " + fontMetrics.getHeight());
        String refString = "AjWw";
        int stringWidth = 0;
        int height = 0;
        for (int i = 0; i < refString.length(); i++) {
            String s = String.valueOf(refString.charAt(i));
            stringWidth = Math.max(fontMetrics.stringWidth(s), stringWidth);
            height = Math.max(fontMetrics.getHeight(), height);
        }
        // Dispose of the Graphics context
        graphics.dispose();
        return new int[]{stringWidth, height};
    }
}
