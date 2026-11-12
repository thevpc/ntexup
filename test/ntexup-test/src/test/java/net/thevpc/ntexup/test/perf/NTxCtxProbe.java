package net.thevpc.ntexup.test.perf;

import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.eval.NTxResolutionContextImpl;

import java.util.List;

public final class NTxCtxProbe {
    private NTxCtxProbe() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: NTxCtxProbe <deck.ntx>");
        }
        NTxPerfSupport.initialize();
        NTxEngine engine = NTxPerfSupport.newEngine();
        List<NTxCompiledPage> pages = NTxPerfSupport.loadPages(engine, args[0]);
        int maxVariables = 0;
        int maxDefinitions = 0;
        int maxFunctions = 0;
        long totalVariables = 0;
        long totalDefinitions = 0;
        long totalFunctions = 0;
        for (NTxCompiledPage page : pages) {
            NTxResolutionContextImpl context = (NTxResolutionContextImpl) page.pageContext();
            int variables = context.getVars() == null ? 0 : context.getVars().size();
            int definitions = context.getDefinitions() == null ? 0 : context.getDefinitions().size();
            int functions = context.getFunctions() == null ? 0 : context.getFunctions().size();
            totalVariables += variables;
            totalDefinitions += definitions;
            totalFunctions += functions;
            maxVariables = Math.max(maxVariables, variables);
            maxDefinitions = Math.max(maxDefinitions, definitions);
            maxFunctions = Math.max(maxFunctions, functions);
        }
        int pageCount = pages.size();
        System.out.printf("pages=%d%n", pageCount);
        System.out.printf("vars  avg=%.1f max=%d%n",
                average(totalVariables, pageCount), maxVariables);
        System.out.printf("defs  avg=%.1f max=%d%n",
                average(totalDefinitions, pageCount), maxDefinitions);
        System.out.printf("funcs avg=%.1f max=%d%n",
                average(totalFunctions, pageCount), maxFunctions);
    }

    private static double average(long total, int count) {
        return count == 0 ? 0 : total / (double) count;
    }
}
