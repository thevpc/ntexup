package net.thevpc.ntexup.lib.geometry2d;

import java.util.List;

/**
 * lis of regions that cannot be merged (different colors?...)
 */
public interface NTxRegion2DGroup extends NTxRegion2D {
    List<NTxRegion2D> children();
}
