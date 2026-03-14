package net.thevpc.ntexup.extension.shapes3d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.builders.NtxElement3DNodeParserFactory;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.nuts.spi.NComponent;

import java.util.List;

public interface NtxElement3DNodeParser extends NComponent {
    List<String> getId3d();
    NtxElement3D createElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory);
}
