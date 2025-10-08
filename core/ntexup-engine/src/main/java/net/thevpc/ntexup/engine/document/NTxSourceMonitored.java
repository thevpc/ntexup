package net.thevpc.ntexup.engine.document;

import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.ntexup.engine.parser.resources.NTxSourceFactory;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.*;
import java.util.stream.Collectors;

public class NTxSourceMonitored implements NTxSource, NTxSourceMonitor {
    private Set<NTxSource> resources = new HashSet<>();

    @Override
    public NOptional<NPath> path() {
        return NOptional.ofNamedEmpty("path");
    }

    public NTxSourceMonitored() {
    }

    @Override
    public NOptional<NTxSource> find(String path) {
        for (NTxSource resource : resources) {
            NPath p = resource.path().orNull();
            if(p!=null){
                if(p.toString().equals(path)){
                    return NOptional.of(resource);
                }
            }
        }
        return NOptional.ofNamedEmpty(NMsg.ofC("resource %s",path));
    }

    @Override
    public void save() {
        for (NTxSource resource : resources) {
            resource.save();
        }
    }

    @Override
    public boolean changed() {
        for (NTxSource r : resources) {
            if (r.changed()) {
                return true;
            }
        }
        return false;
    }

    public String shortName() {
        return resources.stream().map(x -> x.shortName()).collect(Collectors.joining(","));
    }

    public void clear() {
        for (NTxSource r : resources.toArray(new NTxSource[0])) {
            remove(r);
        }
    }

    public void remove(NTxSource r) {
        resources.remove(r);
    }

    public void add(NTxSource r) {
        if (r != null) {
            resources.add(r);
        }
    }

    @Override
    public void add(NPath r) {
        if (r != null) {
            NTxSource r1 = NTxSourceFactory.of(r);
            r1.save();
            add(r1);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NTxSourceMonitored that = (NTxSourceMonitored) o;
        return Objects.equals(resources, that.resources);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resources);
    }
}
