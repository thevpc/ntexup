package net.thevpc.ntexup.extension.svg;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;
import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.renderer.NTxImageTypeRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxGraphicsImageDrawer;
import net.thevpc.nuts.concurrent.NScorableCallable;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class NTxImageTypeRendererFactorySalamanderForSVG implements NTxImageTypeRendererFactory {
    @Override
    public NScorableCallable<NTxGraphicsImageDrawer> resolveRenderer(NPath path, NTxImageOptions options, NTxGraphics graphics) {
        if (path.getName().toLowerCase().endsWith(".svg")) {
            return NScorableCallable.ofValid(
                    () -> {
                        return new SvgNTxImageByTypeRenderer(path);
                    }
            );
        }
        return NScorableCallable.ofInvalid(() -> NMsg.ofC("not supported %s", path));
    }

    private static SVGIcon createSVGIconFromBytes(byte[] svgBytes) {
        // Load SVG from byte array
        SVGUniverse svgUniverse = new SVGUniverse();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(svgBytes);
        URI uri = null;
        try {
            uri = svgUniverse.loadSVG(byteArrayInputStream, "svgFromBytes");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SVGDiagram diagram = svgUniverse.getDiagram(uri);

        // Create SVGIcon
        SVGIcon icon = new SVGIcon();
        icon.setSvgURI(uri);
        icon.setSvgUniverse(svgUniverse);
        return icon;
    }



    private static class SvgNTxImageByTypeRenderer implements NTxGraphicsImageDrawer {
        private final NPath path;

        public SvgNTxImageByTypeRenderer(NPath path) {
            this.path = path;
        }

        @Override
        public void drawImage(double x, double y, NTxImageOptions options, NTxGraphics g) {
            if(!path.exists()){
                throw new IllegalArgumentException("file not found "+path);
            }
            URL u = path.toURL().orNull();
            SVGDiagram diagram;
            URI uri = null;
            SVGUniverse svgUniverse = new SVGUniverse();
            if(u!=null){
                uri = svgUniverse.loadSVG(u);
                diagram = svgUniverse.getDiagram(uri);
            }else {
                // Load SVG from byte array
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(path.readBytes());
                try {
                    uri = svgUniverse.loadSVG(byteArrayInputStream, "svgFromBytes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                diagram = svgUniverse.getDiagram(uri);
            }

            // Create SVGIcon
            SVGIcon icon = new SVGIcon();
            icon.setSvgURI(uri);
            icon.setScaleToFit(true);
            icon.setSvgUniverse(svgUniverse);
            icon.setAntiAlias(true);
            icon.setPreferredSize(options.getSize());
            icon.paintIcon(null, g.graphics2D(), (int) x, (int) y);
        }
    }
}
