package net.thevpc.ntexup.api.util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NTxUtilsImages {
    public static void ensureLoadedImage(Image image) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w < 0 || h < 0) {
            MediaTracker tracker = new MediaTracker(new Component() {});
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (tracker.isErrorID(0)) {
                throw new IllegalArgumentException("Image failed to load");
            }
        }
    }
    public static BufferedImage toBufferedImage(Image originalImage) {
        if (originalImage instanceof BufferedImage) {
            return (BufferedImage) originalImage;
        }
        ensureLoadedImage(originalImage);
        BufferedImage bimage = new BufferedImage(originalImage.getWidth(null), originalImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(originalImage, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

    public static BufferedImage resizeImage(Image originalImage, int targetWidth, int targetHeight) {
        BufferedImage buffImg = toBufferedImage(originalImage);
        int newW=targetWidth;
        int newH=targetHeight;
        int width = buffImg.getWidth();
        int height = buffImg.getHeight();
        if(newH==width && height==newH) {
            return buffImg;
        }
        BufferedImage resizedImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.drawImage(buffImg, 0, 0, newW, newH, null);
        graphics2D.dispose();
        return resizedImage;
    }

    public static BufferedImage resizeImage(Image img, Dimension size) {
        if(size==null){
            return toBufferedImage(img);
        }
        return NTxUtilsImages.resizeImage(img, size.width,size.height);
//        int newW=size.width;
//        int newH=size.height;
//        int width = img.getWidth();
//        int height = img.getHeight();
//        if(newH==width && height==newH){
//            return img;
////            BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
////            Graphics2D g2d = dimg.createGraphics();
////            g2d.drawImage(img, 0, 0, null);
////            g2d.dispose();
////            return dimg;
//        }
//        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
//        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
//
//        Graphics2D g2d = dimg.createGraphics();
//        g2d.drawImage(tmp, 0, 0, null);
//        g2d.dispose();
//
//        return dimg;
    }
//    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
//        int width = img.getWidth();
//        int height = img.getHeight();
//        if(newH==width && height==newH){
//            BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
//            Graphics2D g2d = dimg.createGraphics();
//            g2d.drawImage(img, 0, 0, null);
//            g2d.dispose();
//            return dimg;
//        }
//        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
//        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
//
//        Graphics2D g2d = dimg.createGraphics();
//        g2d.drawImage(tmp, 0, 0, null);
//        g2d.dispose();
//
//        return dimg;
//    }
}
