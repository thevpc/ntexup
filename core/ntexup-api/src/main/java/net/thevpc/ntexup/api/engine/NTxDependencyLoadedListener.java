package net.thevpc.ntexup.api.engine;

import net.thevpc.nuts.artifact.NDefinition;

public interface NTxDependencyLoadedListener {
    void onLoadDependencyLoaded(NDefinition[] dependencies);
}
