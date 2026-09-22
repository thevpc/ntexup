package net.thevpc.ntexup.api.renderer;

import java.io.Serializable;

public class NTxDocumentStreamRendererConfig implements Serializable, Cloneable {
    private NTxPageOrientation orientation;
    private int gridX;
    private int gridY;
    private int pageWidth;
    private int pageHeight;
    private boolean showPageNumber;
    private boolean showFileName;
    private boolean showDate;
    private int dpi;
    private float marginTop;
    private float marginBottom;
    private float marginLeft;
    private float marginRight;
    private NTxPdfMode pdfMode;

    public NTxPageOrientation getOrientation() {
        return orientation;
    }

    public NTxDocumentStreamRendererConfig setOrientation(NTxPageOrientation orientation) {
        this.orientation = orientation;
        return this;
    }

    public int getGridX() {
        return gridX;
    }

    public NTxDocumentStreamRendererConfig setGridX(int gridX) {
        this.gridX = gridX;
        return this;
    }

    public int getGridY() {
        return gridY;
    }

    public NTxDocumentStreamRendererConfig setGridY(int gridY) {
        this.gridY = gridY;
        return this;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public NTxDocumentStreamRendererConfig setPageWidth(int pageWidth) {
        this.pageWidth = pageWidth;
        return this;
    }

    public int getPageHeight() {
        return pageHeight;
    }

    public NTxDocumentStreamRendererConfig setPageHeight(int pageHeight) {
        this.pageHeight = pageHeight;
        return this;
    }

    public boolean isShowPageNumber() {
        return showPageNumber;
    }

    public NTxDocumentStreamRendererConfig setShowPageNumber(boolean showPageNumber) {
        this.showPageNumber = showPageNumber;
        return this;
    }

    public boolean isShowFileName() {
        return showFileName;
    }

    public NTxDocumentStreamRendererConfig setShowFileName(boolean showFileName) {
        this.showFileName = showFileName;
        return this;
    }

    public boolean isShowDate() {
        return showDate;
    }

    public NTxDocumentStreamRendererConfig setShowDate(boolean showDate) {
        this.showDate = showDate;
        return this;
    }

    public int getDpi() {
        return dpi;
    }

    public NTxDocumentStreamRendererConfig setDpi(int dpi) {
        this.dpi = dpi;
        return this;
    }

    public float getMarginTop() {
        return marginTop;
    }

    public NTxDocumentStreamRendererConfig setMarginTop(float marginTop) {
        this.marginTop = marginTop;
        return this;
    }

    public float getMarginBottom() {
        return marginBottom;
    }

    public NTxDocumentStreamRendererConfig setMarginBottom(float marginBottom) {
        this.marginBottom = marginBottom;
        return this;
    }

    public float getMarginLeft() {
        return marginLeft;
    }

    public NTxDocumentStreamRendererConfig setMarginLeft(float marginLeft) {
        this.marginLeft = marginLeft;
        return this;
    }

    public float getMarginRight() {
        return marginRight;
    }

    public NTxDocumentStreamRendererConfig setMarginRight(float marginRight) {
        this.marginRight = marginRight;
        return this;
    }

    /**
     * PDF encoding mode. {@link NTxPdfMode#VECTOR} by default.
     *
     * @return pdf mode
     */
    public NTxPdfMode getPdfMode() {
        return pdfMode == null ? NTxPdfMode.VECTOR : pdfMode;
    }

    public NTxDocumentStreamRendererConfig setPdfMode(NTxPdfMode pdfMode) {
        this.pdfMode = pdfMode;
        return this;
    }

    public NTxDocumentStreamRendererConfig copy() {
        try {
            return (NTxDocumentStreamRendererConfig) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
