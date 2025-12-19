package net.thevpc.ntexup.api.engine;

import net.thevpc.nuts.artifact.NId;

public interface NTxDependencyLoadedListener {
    void onLoadDependencyLoaded(NId[] dependencies);
}
