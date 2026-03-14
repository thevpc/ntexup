package net.thevpc.ntexup.lib.geometry3d;

import java.awt.*;

public interface NtxFace {
    boolean isDrawContour();

    boolean isVisible();

    Paint getBackground();
    Paint getLineColor();

    Stroke getStroke();

    boolean isFillBackground();
}
