package net.thevpc.ntexup.api.engine;

import net.thevpc.nuts.artifact.NDefinition;
import net.thevpc.nuts.artifact.NDependency;

import java.util.Objects;

public class ImportDependencyResult {
    private final NDependency dependency;
    private final NDefinition loadedDependency;
    private final boolean loaded;
    private final boolean failed;
    public ImportDependencyResult(NDependency dependency, NDefinition loadedDependency,boolean loaded,boolean failed) {
        this.dependency = dependency;
        this.loadedDependency = loadedDependency;
        this.loaded = loaded;
        this.failed = failed;
    }

    public boolean isFailed() {
        return failed;
    }

    public NDefinition getLoadedDependency() {
        return loadedDependency;
    }

    public NDependency getDependency() {
        return dependency;
    }

    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ImportDependencyResult that = (ImportDependencyResult) o;
        return loaded == that.loaded && Objects.equals(dependency, that.dependency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependency, loaded);
    }
}
