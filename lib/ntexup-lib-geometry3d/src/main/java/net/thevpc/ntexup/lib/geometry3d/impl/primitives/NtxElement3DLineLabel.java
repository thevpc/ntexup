package net.thevpc.ntexup.lib.geometry3d.impl.primitives;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.nuts.util.NColor;

import java.awt.*;

public class NtxElement3DLineLabel {
    private String text;
    private double position;
    private double orientationAngle;
    private boolean orientation3d;
    private NTxPoint2D offset;
    private String fontFamily;
    private Double fontSize;
    private Boolean fontBold;
    private Boolean fontItalic;
    private Boolean fontUnderline;
    private Boolean fontStrike;
    private Paint foregroundColor;
    private Paint backgroundColor;


    public String getFontFamily() {
        return fontFamily;
    }

    public NtxElement3DLineLabel setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    public Double getFontSize() {
        return fontSize;
    }

    public NtxElement3DLineLabel setFontSize(Double fontSize) {
        this.fontSize = fontSize;
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
        return offset;
    }

    public NtxElement3DLineLabel setOffset(NTxPoint2D offset) {
        this.offset = offset;
        return this;
    }
}
