package net.thevpc.ntexup.test.perf;

import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;

import java.util.Arrays;
import java.util.List;

public final class NTxRenderAll {
    private NTxRenderAll() {
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: NTxRenderAll <deck.ntx> <width> <height>");
        }
        NTxPerfSupport.initialize();
        NTxEngine engine = NTxPerfSupport.newEngine();
        List<NTxCompiledPage> pages = NTxPerfSupport.loadPages(engine, args[0]);
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        long[] cold = new long[pages.size()];
        long[] warm = new long[pages.size()];
        long coldTotal = 0;
        long warmTotal = 0;

        System.out.printf("STARTUP pages=%d%n", pages.size());
        for (int i = 0; i < pages.size(); i++) {
            NTxCompiledPage page = pages.get(i);
            page.compiledPage();
            long start = System.nanoTime();
            NTxPerfSupport.render(engine, page, width, height, false);
            cold[i] = System.nanoTime() - start;
            start = System.nanoTime();
            NTxPerfSupport.render(engine, page, width, height, true);
            warm[i] = System.nanoTime() - start;
            coldTotal += cold[i];
            warmTotal += warm[i];
            page.compiledPage().invalidateRenderCache();
            start = System.nanoTime();
            NTxPerfSupport.render(engine, page, width, height, false);
            long rerender = System.nanoTime() - start;
            System.out.printf("page %3d cold=%6.1fms warm=%6.1fms re=%6.1fms%n",
                    i + 1, cold[i] / 1e6, warm[i] / 1e6, rerender / 1e6);
        }

        System.out.printf("TOTAL %dx%d cold=%.0fms warm=%.0fms%n",
                width, height, coldTotal / 1e6, warmTotal / 1e6);
        printPercentiles(cold);
        printPercentiles(warm);
    }

    private static void printPercentiles(long[] times) {
        if (times.length == 0) {
            System.out.println("p50=n/a p90=n/a p99=n/a max=n/a");
            return;
        }
        long[] sorted = times.clone();
        Arrays.sort(sorted);
        System.out.printf("p50=%.1fms p90=%.1fms p99=%.1fms max=%.1fms%n",
                percentile(sorted, 0.50) / 1e6, percentile(sorted, 0.90) / 1e6,
                percentile(sorted, 0.99) / 1e6, sorted[sorted.length - 1] / 1e6);
    }

    private static long percentile(long[] sorted, double percentile) {
        int index = Math.min(sorted.length - 1, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[Math.max(0, index)];
    }
}
