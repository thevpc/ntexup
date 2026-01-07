package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxEngineTools;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.renderer.NTxSpiUtils;
import net.thevpc.ntexup.engine.util.NTxUtilsImages;
import net.thevpc.ntexup.engine.util.NTxUtilsPages;
import net.thevpc.ntexup.engine.util.NTxUtilsPoints;
import net.thevpc.ntexup.engine.util.NTxUtilsText;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class MyNTxEngineTools implements NTxEngineTools {
    private NTxEngine engine;

    public MyNTxEngineTools(NTxEngine engine) {
        this.engine = engine;
    }

    @Override
    public BufferedImage toBufferedImage(Image originalImage) {
        return NTxUtilsImages.toBufferedImage(originalImage);
    }

    @Override
    public Image resizeImage(Image originalImage, int targetWidth, int targetHeight) {
        return NTxUtilsImages.resizeImage(originalImage, targetWidth, targetHeight);
    }

    @Override
    public BufferedImage resizeBufferedImage(BufferedImage img, Dimension size) {
        return NTxUtilsImages.resizeImage(img, size);
    }

    @Override
    public BufferedImage resizeBufferedImage(BufferedImage img, int newW, int newH) {
        return NTxUtilsImages.resizeImage(img, newW, newH);
    }

    @Override
    public NTxDocumentStreamRendererConfig validateDocumentStreamRendererConfig(NTxDocumentStreamRendererConfig config) {
        return NTxSpiUtils.validateDocumentStreamRendererConfig(config);
    }

    public String trimBloc(String code) {
        return NTxUtilsText.trimBloc(code);
    }

    @Override
    public boolean addPoints(NTxNode line, NTxPoint2D[] points) {
        return NTxUtilsPoints.addPoints(line, points);
    }


    @Override
    public boolean addPoint(NTxNode line, NTxPoint2D point) {
        return NTxUtilsPoints.addPoint(line, point);
    }


    @Override
    public java.util.List<NTxNode> resolvePages(NTxDocument document) {
        return NTxUtilsPages.resolvePages(document);
    }

    @Override
    public java.util.List<NTxNode> resolvePages(NTxNode part) {
        return NTxUtilsPages.resolvePages(part);
    }

    @Override
    public void fillPages(NTxNode part, List<NTxNode> all) {
        NTxUtilsPages.fillPages(part, all);
    }




}
