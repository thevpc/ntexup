package net.thevpc.ntexup.test.perf;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;

import java.util.List;

public final class NTxClassProbe {
    private static long nodes;
    private static long nodesWithClasses;
    private static long classInstances;
    private static long rulesOnPath;
    private static long classRuleScans;
    private static int maxClasses;
    private static int maxRulesOnPath;

    private NTxClassProbe() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: NTxClassProbe <deck.ntx>");
        }
        NTxPerfSupport.initialize();
        NTxEngine engine = NTxPerfSupport.newEngine();
        List<NTxCompiledPage> pages = NTxPerfSupport.loadPages(engine, args[0]);
        for (NTxCompiledPage page : pages) {
            walk(page.compiledPage());
        }
        System.out.printf("pages=%d nodes=%d%n", pages.size(), nodes);
        System.out.printf("nodes with style classes=%d (%.1f%%)%n",
                nodesWithClasses, 100.0 * nodesWithClasses / Math.max(1, nodes));
        System.out.printf("class instances=%d avg/classed-node=%.2f max=%d%n",
                classInstances, classInstances / (double) Math.max(1, nodesWithClasses), maxClasses);
        System.out.printf("rules on path: total=%d max=%d%n", rulesOnPath, maxRulesOnPath);
        System.out.printf("class-fold scans=%d x25 lookups/node=%d%n",
                classRuleScans, classRuleScans * 25);
    }

    private static void walk(NTxNode node) {
        nodes++;
        String[] classes = node.getStyleClasses();
        int classCount = classes == null ? 0 : classes.length;
        maxClasses = Math.max(maxClasses, classCount);
        if (classCount > 0) {
            nodesWithClasses++;
            classInstances += classCount;
        }
        int rules = 0;
        NTxNode parent = net.thevpc.ntexup.api.util.NTxUtils.firstNodeUp(node.parent());
        while (parent != null) {
            rules += parent.rules().length;
            parent = net.thevpc.ntexup.api.util.NTxUtils.firstNodeUp(parent.parent());
        }
        rulesOnPath += rules;
        maxRulesOnPath = Math.max(maxRulesOnPath, rules);
        classRuleScans += (long) classCount * rules;
        List<NTxNode> children = node.children();
        if (children != null) {
            for (NTxNode child : children) {
                walk(child);
            }
        }
    }
}
