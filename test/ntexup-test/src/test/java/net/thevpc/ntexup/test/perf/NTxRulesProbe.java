package net.thevpc.ntexup.test.perf;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;

import java.util.List;

public final class NTxRulesProbe {
    private static long nodes;
    private static long nodesWithClasses;
    private static long classInstances;
    private static long rulesOnPath;
    private static int maxDepth;
    private static int maxRulesOnPath;

    private NTxRulesProbe() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: NTxRulesProbe <deck.ntx>");
        }
        NTxPerfSupport.initialize();
        NTxEngine engine = NTxPerfSupport.newEngine();
        List<NTxCompiledPage> pages = NTxPerfSupport.loadPages(engine, args[0]);
        for (NTxCompiledPage page : pages) {
            walk(page.compiledPage(), 0);
        }
        System.out.printf("pages=%d nodes=%d nodesWithClasses=%d classInstances=%d%n",
                pages.size(), nodes, nodesWithClasses, classInstances);
        System.out.printf("maxDepth=%d totalRulesOnPaths=%d maxRulesOnPath=%d%n",
                maxDepth, rulesOnPath, maxRulesOnPath);
    }

    private static void walk(NTxNode node, int depth) {
        nodes++;
        maxDepth = Math.max(maxDepth, depth);
        String[] classes = node.getStyleClasses();
        if (classes != null && classes.length > 0) {
            nodesWithClasses++;
            classInstances += classes.length;
        }
        int rules = 0;
        NTxNode parent = net.thevpc.ntexup.api.util.NTxUtils.firstNodeUp(node.parent());
        while (parent != null) {
            rules += parent.rules().length;
            parent = net.thevpc.ntexup.api.util.NTxUtils.firstNodeUp(parent.parent());
        }
        rulesOnPath += rules;
        maxRulesOnPath = Math.max(maxRulesOnPath, rules);
        List<NTxNode> children = node.children();
        if (children != null) {
            for (NTxNode child : children) {
                walk(child, depth + 1);
            }
        }
    }
}
