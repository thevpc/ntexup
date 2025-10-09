package net.thevpc.ntexup.extension.animatedgif;

import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.renderer.NTxImageTypeRendererFactory;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxGraphicsImageDrawer;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;

public class NTxImageTypeRendererFactoryForGif implements NTxImageTypeRendererFactory {
    private Map<String, FutureTask<NPath>> pendingCache = new HashMap<>();

    @Override
    public NScoredCallable<NTxGraphicsImageDrawer> resolveRenderer(NPath path, NTxImageOptions options, NTxGraphics graphics) {
        if (!options.isDisableAnimation()) {
            if (path.getName().toLowerCase().endsWith(".gif")) {
                return NScoredCallable.ofValid(
                        () -> {
                            byte[] b = path.readBytes();
                            return new GifNTxImageDrawer(b, pendingCache);
                        }
                );
            }
        }
        return NScoredCallable.ofInvalid(() -> NMsg.ofC("not supported %s", path));
    }
}
