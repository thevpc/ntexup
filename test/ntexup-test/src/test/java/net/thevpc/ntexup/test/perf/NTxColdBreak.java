package net.thevpc.ntexup.test.perf;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

import java.util.List;

public final class NTxColdBreak {
    private NTxColdBreak() {
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: NTxColdBreak <deck.ntx> <width> <height>");
        }
        System.setProperty("java.awt.headless", "true");

        long start = System.nanoTime();
        Nuts.require();
        report("Nuts.require", start);

        start = System.nanoTime();
        NTxEngine engine = new DefaultNTxEngine();
        report("engine ctor", start);

        start = System.nanoTime();
        NTxCompiledDocument document = engine.loadDocument(NPath.of(args[0]));
        report("loadDocument (parse)", start);

        start = System.nanoTime();
        document.document();
        report("document() (compile + imports)", start);

        start = System.nanoTime();
        List<NTxCompiledPage> pages = document.pages();
        report("pages() drain (" + pages.size() + ")", start);

        if (!pages.isEmpty()) {
            start = System.nanoTime();
            NTxPerfSupport.render(engine, pages.getFirst(),
                    Integer.parseInt(args[1]), Integer.parseInt(args[2]), false);
            report("first page render", start);
        }
    }

    private static void report(String phase, long start) {
        System.out.printf("PHASE %s = %.0f ms%n", phase, (System.nanoTime() - start) / 1e6);
    }
}
