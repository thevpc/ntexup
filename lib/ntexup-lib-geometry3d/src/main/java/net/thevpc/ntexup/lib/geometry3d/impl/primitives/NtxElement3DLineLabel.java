package net.thevpc.ntexup.lib.geometry3d.impl.primitives;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxSize;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;

import java.awt.*;

public class NtxElement3DLineLabel {
    private String text;
    private double position = 50.0;
    private double orientationAngle;
    private boolean orientation3d;
    private NTxPoint2D offset2d;
    private NTxPoint3D offset3d;
    private NTxPoint3D offsetRel3d;
    private NTxSize offsetPerpSize;
    private NTxSize offsetParallelSize;
    private NTxSize offsetUpSize;
    private Double offsetPerp;
    private Double offsetParallel;
    private String fontFamily;
    private NTxSize fontSize;
    private Boolean fontBold;
    private Boolean fontItalic;
    private Boolean fontUnderline;
    private Boolean fontStrike;
    private Paint foregroundColor;
    private Paint backgroundColor;

    public NTxPoint2D getOffset2d() {
        return offset2d;
    }

    public NtxElement3DLineLabel setOffset2d(NTxPoint2D offset2d) {
        this.offset2d = offset2d;
        return this;
    }

    public NTxPoint3D getOffset3d() {
        return offset3d;
    }

    public NtxElement3DLineLabel setOffset3d(NTxPoint3D offset3d) {
        this.offset3d = offset3d;
        return this;
    }

    public NTxPoint3D getOffsetRel3d() {
        return offsetRel3d;
    }

    public NtxElement3DLineLabel setOffsetRel3d(NTxPoint3D offsetRel3d) {
        this.offsetRel3d = offsetRel3d;
        return this;
    }

    public NTxSize getOffsetPerpSize() {
        return offsetPerpSize;
    }

    public NtxElement3DLineLabel setOffsetPerpSize(NTxSize offsetPerpSize) {
        this.offsetPerpSize = offsetPerpSize;
        return this;
    }

    public NTxSize getOffsetParallelSize() {
        return offsetParallelSize;
    }

    public NtxElement3DLineLabel setOffsetParallelSize(NTxSize offsetParallelSize) {
        this.offsetParallelSize = offsetParallelSize;
        return this;
    }

    public NTxSize getOffsetUpSize() {
        return offsetUpSize;
    }

    public NtxElement3DLineLabel setOffsetUpSize(NTxSize offsetUpSize) {
        this.offsetUpSize = offsetUpSize;
        return this;
    }

    public Double getOffsetPerp() {
        return offsetPerp;
    }

    public NtxElement3DLineLabel setOffsetPerp(Double offsetPerp) {
        this.offsetPerp = offsetPerp;
        if (offsetPerp != null) {
            this.offsetPerpSize = NTxSize.ofParent(offsetPerp);
        }
        return this;
    }

    public Double getOffsetParallel() {
        return offsetParallel;
    }

    public NtxElement3DLineLabel setOffsetParallel(Double offsetParallel) {
        this.offsetParallel = offsetParallel;
        if (offsetParallel != null) {
            this.offsetParallelSize = NTxSize.ofParent(offsetParallel);
        }
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public NtxElement3DLineLabel setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    public NTxSize getFontSize() {
        return fontSize;
    }

    public NtxElement3DLineLabel setFontSize(NTxSize fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public NtxElement3DLineLabel setFontSize(Double fontSize) {
        if (fontSize != null) {
            this.fontSize = fontSize > 10 ? NTxSize.ofPx(fontSize) : NTxSize.ofPage(fontSize);
        } else {
            this.fontSize = null;
        }
        return this;
    }

    public Boolean getFontBold() {
        return fontBold;
    }

    public NtxElement3DLineLabel setFontBold(Boolean fontBold) {
        this.fontBold = fontBold;
        return this;
    }

    public Boolean getFontItalic() {
        return fontItalic;
    }

    public NtxElement3DLineLabel setFontItalic(Boolean fontItalic) {
        this.fontItalic = fontItalic;
        return this;
    }

    public Boolean getFontUnderline() {
        return fontUnderline;
    }

    public NtxElement3DLineLabel setFontUnderline(Boolean fontUnderline) {
        this.fontUnderline = fontUnderline;
        return this;
    }

    public Boolean getFontStrike() {
        return fontStrike;
    }

    public NtxElement3DLineLabel setFontStrike(Boolean fontStrike) {
        this.fontStrike = fontStrike;
        return this;
    }

    public Paint getForegroundColor() {
        return foregroundColor;
    }

    public NtxElement3DLineLabel setForegroundColor(Paint foregroundColor) {
        this.foregroundColor = foregroundColor;
        return this;
    }

    public Paint getBackgroundColor() {
        return backgroundColor;
    }

    public NtxElement3DLineLabel setBackgroundColor(Paint backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public String getText() {
        return text;
    }

    public NtxElement3DLineLabel setText(String text) {
        this.text = text;
        return this;
    }

    public double getPosition() {
        return position;
    }

    public NtxElement3DLineLabel setPosition(double position) {
        this.position = position;
        return this;
    }

    public double getOrientationAngle() {
        return orientationAngle;
    }

    public NtxElement3DLineLabel setOrientationAngle(double orientationAngle) {
        this.orientationAngle = orientationAngle;
        return this;
    }

    public boolean isOrientation3d() {
        return orientation3d;
    }

    public NtxElement3DLineLabel setOrientation3d(boolean orientation3d) {
        this.orientation3d = orientation3d;
        return this;
    }

    public NTxPoint2D getOffset() {
        return offset2d;
    }

    public NtxElement3DLineLabel setOffset(NTxPoint2D offset) {
        this.offset2d = offset;
        return this;
    }
}
