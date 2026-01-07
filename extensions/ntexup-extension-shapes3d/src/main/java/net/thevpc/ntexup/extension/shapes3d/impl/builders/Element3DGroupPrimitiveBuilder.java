package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.extension.shapes3d.api.NTxMatrix3D;
import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3DPrimitive;
import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxRenderState3D;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DGroup;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.extension.shapes3d.api.NTxElement3DRenderer;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NTx3DUtils;

import java.util.ArrayList;
import java.util.List;

public class Element3DGroupPrimitiveBuilder implements NTxElement3DRenderer, NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("group3d")
                .parseParam().matchesAny().end()
                .renderComponent(this::render);
    }

    public void render(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        // do nothing in 2D
    }

    @Override
    public Class<? extends NtxElement3D> forType() {
        return NtxElement3DGroup.class;
    }

    @Override
    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        NtxElement3DGroup ee = (NtxElement3DGroup) e;
        List<NtxElement3D> elements = ee.getElements();
        List<NtxElement3DPrimitive> all = new ArrayList<>();
        for (NtxElement3D element : elements) {
            for (NtxElement3DPrimitive tt : renderState.toPrimitives(element)) {
                NTxMatrix3D t = e.getTransform().multiply(e.getTransform());
                NTx3DUtils.copyPropsIfEmptyTarget(e,tt,null);
                tt.setTransform(t);
                all.add(tt);
            }
        }
        return all.toArray(new NtxElement3DPrimitive[0]);
    }
}
