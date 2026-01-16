package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.engine.NTxDependencyLoadedListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.thevpc.nuts.artifact.NId;

public abstract class NTxServiceListImpl<T> implements NTxDependencyLoadedListener {
    private Class<T> serviceType;
    private Set<Class<T>> alreadyLoaded = new HashSet<>();
    private Set<Class<T>> userLoaded = new HashSet<>();
    protected DefaultNTxEngine engine;

    public NTxServiceListImpl(Class<T> serviceType, DefaultNTxEngine engine) {
        this.engine = engine;
        this.serviceType = serviceType;
        engine.addDependencyLoadedListener(this);
    }

    public void build(NId[] dependencies){
        List<T> newServices = engine.loadServices(serviceType);
        for (T newService : newServices) {
            if (!alreadyLoaded.contains(newService.getClass())) {
                alreadyLoaded.add(serviceType);
                onNewService(newService, dependencies);
            }
        }
    }

    @Override
    public void onLoadDependencyLoaded(NId[] dependencies) {
        build(dependencies);
    }

    protected abstract void onNewService(T newService, NId[] dependencies);
}
