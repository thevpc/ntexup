package net.thevpc.ntexup.lib.geometry3d.impl;

import net.thevpc.ntexup.lib.geometry3d.NtxElement3DPrimitive;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.NTxRenderState3D;
import net.thevpc.ntexup.api.engine.NTxDependencyLoadedListener;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.lib.geometry3d.NTxElement3DRenderer;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.io.NLibPaths;
import net.thevpc.nuts.io.NServiceLoader;
import net.thevpc.nuts.text.NMsg;

import java.util.*;

public class Element3DUIFactory {
    private Map<Class, NTxElement3DRenderer> map = new HashMap<>();
    private NTxEngine engine;

    public Element3DUIFactory(NTxEngine engine) {
        this.engine=engine;
        update(new NId[0]);
        engine.addDependencyLoadedListener(new NTxDependencyLoadedListener() {
            @Override
            public void onLoadDependencyLoaded(NId[] dependencies) {
                update(dependencies);
            }
        });
    }

    private void update(NId[] dependencies) {
        NServiceLoader<NTxElement3DRenderer> serviceLoader = NServiceLoader.of(NTxElement3DRenderer.class, null,engine.getEngineClassLoader().asClassLoader());
        for (NTxElement3DRenderer element3DPrimitiveBuilder : serviceLoader.loadAll(null)) {
            register(element3DPrimitiveBuilder.forType(), element3DPrimitiveBuilder, dependencies);
        }
    }

    void register(Class c, NTxElement3DRenderer f, NId[] dependencies) {
        if (!map.containsKey(c)) {
            map.put(c, f);
            NId sourceId = NLibPaths.of().resolveId(c).orNull();
            engine.log().log(NMsg.ofC("[%s] loaded %s : %s", sourceId, NMsg.ofStyledPrimary1("3DRenderer"), c));
        }
    }

    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        if (e instanceof NtxElement3DPrimitive) {
            return new NtxElement3DPrimitive[]{(NtxElement3DPrimitive) e};
        }
        NTxElement3DRenderer i = map.get(e.getClass());
        if (i != null) {
            return i.toPrimitives(e, renderState);
        }
        throw new IllegalArgumentException("Not Found Primitive Builder for " + e.getClass());
    }

}
