package net.thevpc.ntexup.api.source;

import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NOptional;

public interface NTxSource {
    NOptional<NPath> path();
    void save();
    boolean changed();
    String shortName();
}
