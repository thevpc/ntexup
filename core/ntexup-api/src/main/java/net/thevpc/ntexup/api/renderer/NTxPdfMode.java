/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package net.thevpc.ntexup.api.renderer;

/**
 * How a PDF stream renderer should encode document pages.
 */
public enum NTxPdfMode {
    /**
     * Render pages as real PDF vector/text primitives using a PDF Graphics2D
     * bridge. Produces lightweight files with selectable text.
     */
    VECTOR,
    /**
     * Render pages as high-resolution raster images at the configured DPI and
     * embed them in the PDF (heavier files, non selectable text).
     */
    RASTER
}