package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NtxFontInfo;

public class NTxValueSizeCache {
    public NTxElemNumber2 position;
    public NTxElemNumber2 origin;
    public boolean preserveRatio;
    public NtxFontInfo fontInfo;
    public NTxDouble2 componentSize;
    public NTxMargin margin;
    public NTxSizeRef parentWithMarginRef;
    public NTxBounds2D parentBoundsWithMargin;
}
