package net.thevpc.ntexup.extension.animatedgif;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import com.madgag.gif.fmsware.GifDecoder;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.platform.NStoreType;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class GifResizer {

    public interface GifFrameTransformer {
        default BufferedImage frame(BufferedImage image, int index){
            return image;
        }

        default float speed() {
            return 1;
        }

        default int loopCount() {
            return -1;
        }

        default Color transparentColor() {
            return null;
        }

        Dimension size();
    }

    private static String hash(byte[] source) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(source);
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static FutureTask<NPath> transformWithCache(byte[] source, String suffix,
                                                       GifFrameTransformer transform,
                                                       Map<String,FutureTask<NPath>> pendingCache,
                                                       NTxEngine engine
                                                       ) {
        Color t = transform.transparentColor();
        String name = hash(source);
        if(t!=null){
            name = name + "-r" + t.getRed()+"-g" + t.getGreen()+"-b" + t.getBlue();
        }

        int iLoopCount = transform.loopCount();
        if (iLoopCount >= 0) {
            name = name + "-l" + iLoopCount;
        }

        float speed = transform.speed();
        if(speed>0){
            name = name + "-s" + speed;
        }

        Dimension size = transform.size();
        if(size!=null && size.width>0 && size.height>0){
            name = name + "-w" + size.width+ "-h" + size.height;
        }

        if(!NBlankable.isBlank(suffix)) {
            name = name + "-" + suffix;
        }

        synchronized (pendingCache){
            FutureTask<NPath> f = pendingCache.get(name);
            if(f!=null){
                return f;
            }
        }

        NPath appFolder = NApp.of().getFolder(NStoreType.CACHE);
        if(appFolder==null){
            appFolder=NPath.ofUserHome().resolve(".cache/ntexup");
        }
        NPath n = appFolder.resolve("gif-resizer").mkdirs().resolve(name + ".gif");
        if(n.isRegularFile()){
            FutureTask<NPath> f = new FutureTask<>(new Callable<NPath>() {
                @Override
                public NPath call() throws Exception {
                    return n;
                }
            });
            f.run();
            return f;
        }
        FutureTask<NPath> f = new FutureTask<>(new Callable<NPath>() {
            @Override
            public NPath call() throws Exception {
                byte[] bytes = transform(source, transform,engine);
                n.writeBytes(bytes);
                return n;
            }
        });
        String finalName = name;
        new Thread(()->{
            f.run();
            synchronized (pendingCache) {
                pendingCache.remove(finalName);
            }
        }).start();
        return f;
    }

    public static byte[] transform(byte[] source, GifFrameTransformer transform, NTxEngine engine) {
        GifDecoder decoder = new GifDecoder();
        decoder.read(new ByteArrayInputStream(source));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(outputStream);
        encoder.setTransparent(transform.transparentColor());
        int loopCount = decoder.getLoopCount();
        int iLoopCount = transform.loopCount();
        if (iLoopCount >= 0) {
            loopCount = iLoopCount;
        }
        encoder.setRepeat(loopCount);
        float speed = transform.speed();
        if(speed<=0){
            speed=1;
        }
        for (int i = 0; i < decoder.getFrameCount(); i++) {
            int delay = (int) (decoder.getDelay(i) * speed);
            if (delay <= 0) {
                delay = 1;
            }
            encoder.setDelay(delay);
            BufferedImage frame = decoder.getFrame(i);
            BufferedImage tframe = transform.frame(frame, i);
            if (tframe == null) {
                tframe = frame;
            }
            Dimension size = transform.size();
            if(size!=null && size.width>0 && size.height>0){
                tframe= engine.tools().resizeBufferedImage(tframe, size.width, size.height);
            }
            encoder.addFrame(tframe);
        }
        encoder.finish();
        return outputStream.toByteArray();
    }

    public static byte[] transform0(byte[] source, Function<BufferedImage, BufferedImage> transform) {

        List<BufferedImage> images = new ArrayList<>(25);
        List<Integer> delays = new ArrayList<>(25);
        int delay = 0;

        ImageOutputStream output = null;
        GifSequenceWriter writer = null;

        try {

            String[] imageatt = new String[]{
                    "imageLeftPosition",
                    "imageTopPosition",
                    "imageWidth",
                    "imageHeight"
            };

            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            ImageInputStream ciis = ImageIO.createImageInputStream(new ByteArrayInputStream(source));
            reader.setInput(ciis, false);
            int noi = reader.getNumImages(true);
            BufferedImage master = null;

            for (int i = 0; i < noi; i++) {

                BufferedImage image = reader.read(i);
                IIOMetadata metadata = reader.getImageMetadata(i);

                Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
                NodeList children = tree.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node nodeItem = children.item(j);
                    if (nodeItem.getNodeName().equals("ImageDescriptor")) {
                        Map<String, Integer> imageAttr = new HashMap<String, Integer>();
                        NamedNodeMap attr = nodeItem.getAttributes();
//                        for (int index = 0; index < attr.getLength(); index++) {
//                            Node node = attr.item(index);
//                            System.out.println("----> " + node.getNodeName() + "=" + node.getNodeValue());
//                        }
                        for (int k = 0; k < imageatt.length; k++) {
                            Node attnode = attr.getNamedItem(imageatt[k]);
                            imageAttr.put(imageatt[k], Integer.valueOf(attnode.getNodeValue()));
                        }

                        if (master == null) {
                            master = new BufferedImage(imageAttr.get("imageWidth"), imageAttr.get("imageHeight"), BufferedImage.TYPE_INT_ARGB);
                        }

                        Graphics2D g2d = master.createGraphics();
                        g2d.drawImage(image, imageAttr.get("imageLeftPosition"), imageAttr.get("imageTopPosition"), null);
                        g2d.dispose();

//                        BufferedImage frame = mirror(copyImage(master));
                        BufferedImage frame = copyImage(master);
                        frame = transform.apply(frame);
                        //ImageIO.write(frame, "png", new File("img" + i + ".png"));
                        images.add(frame);

                    } else if (nodeItem.getNodeName().equals("GraphicControlExtension")) {
                        NamedNodeMap attr = nodeItem.getAttributes();
                        Node delayNode = attr.getNamedItem("delayTime");
                        if (delayNode != null) {
                            delay = Math.max(delay, Integer.valueOf(delayNode.getNodeValue()));
                            delays.add(delay);
                        }
                    }
                }

            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            output = new MemoryCacheImageOutputStream(bos);
            writer = new GifSequenceWriter(output, images.get(0).getType(), delay * 10, true);

            for (int i = 0; i < images.size(); i++) {
                BufferedImage nextImage = images.get(i);
                writer.writeToSequence(nextImage);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new IllegalArgumentException(e);
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
            }
            try {
                output.close();
            } catch (Exception e) {
            }
        }
    }

    public static BufferedImage transform(BufferedImage img) {

        BufferedImage mirror = createCompatibleImage(img);
        Graphics2D g2d = mirror.createGraphics();
        AffineTransform at = new AffineTransform();
        at.setToScale(1, -1);
        at.translate(0, -img.getHeight());
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return mirror;

    }

    public static BufferedImage copyImage(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        BufferedImage newImage = createCompatibleImage(img);
        Graphics graphics = newImage.createGraphics();

        int x = 0;//(width - img.getWidth()) / 2;
        int y = 0;//(height - img.getHeight()) / 2;

        graphics.drawImage(img, x, y, img.getWidth(), img.getHeight(), null);
        graphics.dispose();

        return newImage;
    }

    public static BufferedImage createCompatibleImage(BufferedImage image) {
        return getGraphicsConfiguration().createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency());
    }

    public static GraphicsConfiguration getGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    public static class GifSequenceWriter {

        protected ImageWriter gifWriter;
        protected ImageWriteParam imageWriteParam;
        protected IIOMetadata imageMetaData;

        /**
         * Creates a new GifSequenceWriter
         *
         * @param outputStream        the ImageOutputStream to be written to
         * @param imageType           one of the imageTypes specified in BufferedImage
         * @param timeBetweenFramesMS the time between frames in miliseconds
         * @param loopContinuously    wether the gif should loop repeatedly
         * @throws IIOException if no gif ImageWriters are found
         * @author Elliot Kroo (elliot[at]kroo[dot]net)
         */
        public GifSequenceWriter(
                ImageOutputStream outputStream,
                int imageType,
                int timeBetweenFramesMS,
                boolean loopContinuously) throws IIOException, IOException {
            // my method to create a writer
            gifWriter = getWriter();
            imageWriteParam = gifWriter.getDefaultWriteParam();
            ImageTypeSpecifier imageTypeSpecifier
                    = ImageTypeSpecifier.createFromBufferedImageType(imageType);

            imageMetaData
                    = gifWriter.getDefaultImageMetadata(imageTypeSpecifier,
                    imageWriteParam);

            String metaFormatName = imageMetaData.getNativeMetadataFormatName();

            IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

            IIOMetadataNode graphicsControlExtensionNode = getNode(
                    root,
                    "GraphicControlExtension");

            //restoreToBackgroundColor
            //restoreToPrevious
            graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
            graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute(
                    "transparentColorFlag",
                    "FALSE");
            graphicsControlExtensionNode.setAttribute(
                    "delayTime",
                    Integer.toString(timeBetweenFramesMS / 10));
            graphicsControlExtensionNode.setAttribute(
                    "transparentColorIndex",
                    "0");

            IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
            commentsNode.setAttribute("CommentExtension", "Created by MAH");

            IIOMetadataNode appEntensionsNode = getNode(
                    root,
                    "ApplicationExtensions");

            IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

            child.setAttribute("applicationID", "NETSCAPE");
            child.setAttribute("authenticationCode", "2.0");

            int loop = loopContinuously ? 0 : 1;

            child.setUserObject(new byte[]{0x1, (byte) (loop & 0xFF), (byte) ((loop >> 8) & 0xFF)});
            appEntensionsNode.appendChild(child);

            imageMetaData.setFromTree(metaFormatName, root);

            gifWriter.setOutput(outputStream);

            gifWriter.prepareWriteSequence(null);
        }

        public void writeToSequence(RenderedImage img) throws IOException {
            gifWriter.writeToSequence(
                    new IIOImage(
                            img,
                            null,
                            imageMetaData),
                    imageWriteParam);
        }

        /**
         * Close this GifSequenceWriter object. This does not close the
         * underlying stream, just finishes off the GIF.
         */
        public void close() throws IOException {
            gifWriter.endWriteSequence();
        }

        /**
         * Returns the first available GIF ImageWriter using
         * ImageIO.getImageWritersBySuffix("gif").
         *
         * @return a GIF ImageWriter object
         * @throws IIOException if no GIF image writers are returned
         */
        private static ImageWriter getWriter() throws IIOException {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
            if (!iter.hasNext()) {
                throw new IIOException("No GIF Image Writers Exist");
            } else {
                return iter.next();
            }
        }

        /**
         * Returns an existing child node, or creates and returns a new child
         * node (if the requested node does not exist).
         *
         * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child
         *                 node.
         * @param nodeName the name of the child node.
         * @return the child node, if found or a new node created with the given
         * name.
         */
        private static IIOMetadataNode getNode(
                IIOMetadataNode rootNode,
                String nodeName) {
            int nNodes = rootNode.getLength();
            for (int i = 0; i < nNodes; i++) {
                if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName)
                        == 0) {
                    return ((IIOMetadataNode) rootNode.item(i));
                }
            }
            IIOMetadataNode node = new IIOMetadataNode(nodeName);
            rootNode.appendChild(node);
            return (node);
        }
    }
}
