package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public interface NTxEngineTools {
    BufferedImage toBufferedImage(Image originalImage);

    Image resizeImage(Image originalImage, int targetWidth, int targetHeight);

    BufferedImage resizeBufferedImage(BufferedImage img, Dimension size);

    BufferedImage resizeBufferedImage(BufferedImage img, int newW, int newH);

    NTxDocumentStreamRendererConfig validateDocumentStreamRendererConfig(NTxDocumentStreamRendererConfig config);

    String trimBloc(String code);
    boolean addPoints(NTxNode line, NTxPoint2D[] points);

    boolean addPoint(NTxNode line, NTxPoint2D point);

    java.util.List<NTxNode> resolvePages(NTxDocument document);

    java.util.List<NTxNode> resolvePages(NTxNode part);

    void fillPages(NTxNode part, List<NTxNode> all);
}
