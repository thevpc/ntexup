package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxMatrix3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3DPrimitive;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.NTxRenderState3D;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.lib.geometry3d.NTxElement3DRenderer;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Element3DGroupPrimitiveBuilder implements NTxElement3DRenderer, NTxNodeBuilder, NtxElement3DNodeParser {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .ids("group3d","group")
                .parseParam().matchesAny().end()
                .renderComponent((rendererContext) -> render(rendererContext));
    }

    @Override
    public List<String> getId3d() {
        return Arrays.asList("group3d","group");
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NtxElement3DGroup r = new NtxElement3DGroup();
        for (NTxNode child : node.children()) {
            NtxElement3D cc = createElement3D(child, rendererContext, b, mapper,parserFactory);
            r.add(cc);
        }
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        return r;
    }

    public void render(NTxNodeRendererContext rendererContext) {
        // do nothing in 2D
    }

    @Override
    public Class<? extends NtxElement3D> forType() {
        return NtxElement3DGroup.class;
    }

    @Override
    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        NtxElement3DGroup grp = (NtxElement3DGroup) e;
        List<NtxElement3D> elements = grp.getElements();
        List<NtxElement3DPrimitive> all = new ArrayList<>();
        for (NtxElement3D element : elements) {
            for (NtxElement3DPrimitive tt : renderState.toPrimitives(element)) {
                NTxMatrix3D t = grp.getTransform().multiply(element.getTransform());
                NTx3DUtils.copyPropsIfEmptyTarget(element,tt,null);
                NTx3DUtils.copyPropsIfEmptyTarget(grp,tt,null);
                tt.setTransform(t);
                all.add(tt);
            }
        }
        return all.toArray(new NtxElement3DPrimitive[0]);
    }
}
