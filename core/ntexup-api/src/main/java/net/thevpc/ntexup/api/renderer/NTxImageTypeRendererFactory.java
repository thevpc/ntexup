package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.io.NPath;

public interface NTxImageTypeRendererFactory {
    NScoredCallable<NTxGraphicsImageDrawer> resolveRenderer(NPath path, NTxImageOptions options, NTxGraphics graphics);
}
