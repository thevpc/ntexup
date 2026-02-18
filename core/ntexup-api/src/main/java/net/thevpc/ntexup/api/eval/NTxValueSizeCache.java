package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NtxFontInfo;

public class NTxValueSizeCache {
    public NTxElemNumber2 position;
    public NTxElemNumber2 origin;
    public NTxElemNumber2 innerPosition;
    public NTxElemNumber2 innerOrigin;
    public boolean preserveRatio;
    public NtxFontInfo fontInfo;
    public NTxDouble2 allowedComponentSize;
    public NTxDouble2 referenceComponentSize;
//    public NTxDouble2 componentSize;
//    public NTxBounds2D componentBounds;
    public NTxMargin margin;
    public NTxSizeRef parentWithMarginRef;
    public NTxBounds2D parentBoundsWithMargin;
}
