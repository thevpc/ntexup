package net.thevpc.ntexup.test.perf;

import jdk.jfr.Recording;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;

import java.nio.file.Path;
import java.util.List;

public final class NTxWarmOnly {
    private NTxWarmOnly() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: NTxWarmOnly <deck.ntx> <width> <height> [recording.jfr]");
        }
        NTxPerfSupport.initialize();
        NTxEngine engine = NTxPerfSupport.newEngine();
        List<NTxCompiledPage> pages = NTxPerfSupport.loadPages(engine, args[0]);
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        for (NTxCompiledPage page : pages) {
            page.compiledPage();
            NTxPerfSupport.render(engine, page, width, height, false);
        }
        for (NTxCompiledPage page : pages) {
            NTxPerfSupport.render(engine, page, width, height, true);
        }

        Path recordingPath = args.length > 3 ? Path.of(args[3]) : Path.of("warm.jfr");
        long elapsed;
        try (Recording recording = new Recording()) {
            recording.enable("jdk.ExecutionSample").with("period", "1 ms");
            recording.start();
            long start = System.nanoTime();
            for (NTxCompiledPage page : pages) {
                NTxPerfSupport.render(engine, page, width, height, true);
            }
            elapsed = System.nanoTime() - start;
            recording.stop();
            recording.dump(recordingPath);
        }
        System.out.printf("WARM useCache=true total=%.0fms pages=%d avg=%.1fms recording=%s%n",
                elapsed / 1e6, pages.size(), pages.isEmpty() ? 0 : elapsed / 1e6 / pages.size(), recordingPath);
    }
}
