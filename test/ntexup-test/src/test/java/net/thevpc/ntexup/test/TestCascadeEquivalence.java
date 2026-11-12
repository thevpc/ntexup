package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.DefaultNTxNodeSelector;
import net.thevpc.ntexup.api.document.style.DefaultNTxStyleRule;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleAndMagnitude;
import net.thevpc.ntexup.api.document.style.NTxStyleMagnitude;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.document.style.NTxStyleRuleSelectorItem;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.document.NTxPropCalculator;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.parser.resources.NTxSourceNew;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TestCascadeEquivalence {

    private static final NTxSource SOURCE = new NTxSourceNew();

    @BeforeAll
    static void initializeNuts() {
        Nuts.require();
    }

    @Test
    void cachedResolutionMatchesLegacyAndInvalidatesAfterMutation() {
        NTxEngine engine = new DefaultNTxEngine();
        NTxPropCalculator calculator = new NTxPropCalculator(engine);
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        root.addRule(DefaultNTxStyleRule.of(root, SOURCE, DefaultNTxNodeSelector.ofAny(),
                NTxProp.ofString("color", "red")));
        DefaultNTxNode target = new DefaultNTxNode(NTxNodeType.TEXT);
        root.addChild(target);

        assertEquivalent(calculator, target, "color");
        assertEquals("red", valueOf(calculator, target, "color"));
        assertPluralMatchesLegacySingles(calculator, target);

        root.addRule(DefaultNTxStyleRule.of(root, SOURCE, DefaultNTxNodeSelector.ofAny(),
                NTxProp.ofString("color", "blue")));
        assertEquals("blue", valueOf(calculator, target, "color"));
        target.setProperty(NTxProp.ofString("color", "green"));
        assertEquals("green", valueOf(calculator, target, "color"));

        DefaultNTxStyleRule mutableRule = DefaultNTxStyleRule.of(null, SOURCE,
                DefaultNTxNodeSelector.ofAny(), NTxProp.ofString("cascade-mutable", "before"));
        root.addRule(mutableRule);
        assertEquals("before", valueOf(calculator, target, "cascade-mutable"));
        mutableRule.styles().set(NTxProp.ofString("cascade-mutable", "after"));
        assertEquals("after", valueOf(calculator, target, "cascade-mutable"));

        root.addRule(DefaultNTxStyleRule.ofClassDef(root, SOURCE, "special", List.of(),
                NTxProp.ofString("cascade-test", "class-value")));
        target.addStyleClass("special");
        assertEquals("class-value", valueOf(calculator, target, "cascade-test"));
        target.setStyleClasses(new String[0]);
        assertEquals(null, valueOf(calculator, target, "cascade-test"));
    }

    @Test
    void aliasesKeepLastSynonymWinsWithinEachRule() {
        NTxEngine engine = new DefaultNTxEngine();
        NTxPropCalculator calculator = new NTxPropCalculator(engine);
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        root.addRule(DefaultNTxStyleRule.of(root, SOURCE, DefaultNTxNodeSelector.ofAny(),
                NTxProp.ofString("a", "first"),
                NTxProp.ofString("b", "second")));
        DefaultNTxNode target = new DefaultNTxNode(NTxNodeType.TEXT);
        root.addChild(target);

        assertEquivalent(calculator, target, "a", "b");
        assertEquals("second", valueOf(calculator, target, "a", "b"));
    }

    @Test
    void benchmarkDecksMatchLegacyWhenPathsAreProvided() {
        String d1 = System.getProperty("ntexup.test.d1");
        String d2 = System.getProperty("ntexup.test.d2");
        assumeTrue(d1 != null && d2 != null,
                "Set -Dntexup.test.d1 and -Dntexup.test.d2 to run full-deck equivalence");
        compareDeck(d1);
        compareDeck(d2);
    }

    private static void compareDeck(String path) {
        NTxEngine engine = new DefaultNTxEngine();
        List<NTxCompiledPage> pages = engine.loadDocument(net.thevpc.nuts.io.NPath.of(path)).pages();
        Set<String> propertyNames = new LinkedHashSet<>();
        for (NTxStyleRule rule : engine.getDefaultStyles()) {
            rule.styles().values().forEach(p -> propertyNames.add(p.getName()));
        }
        for (NTxCompiledPage page : pages) {
            collectPropertyNames(page.compiledPage(), propertyNames);
        }

        NTxPropCalculator calculator = new NTxPropCalculator(engine);
        List<NTxNode> nodes = new ArrayList<>();
        for (NTxCompiledPage page : pages) {
            collectNodes(page.compiledPage(), nodes);
        }
        int comparisons = 0;
        for (NTxNode node : nodes) {
            for (String propertyName : propertyNames) {
                assertEquivalent(calculator, node, propertyName);
                comparisons++;
            }
            assertPluralMatchesLegacySingles(calculator, node);
        }
        assertTrue(comparisons > 0, "No property comparisons were performed for " + path);
        System.out.printf("Cascade equivalence: %s nodes=%d properties=%d comparisons=%d%n",
                path, nodes.size(), propertyNames.size(), comparisons);
    }

    private static void collectNodes(NTxNode node, List<NTxNode> result) {
        result.add(node);
        for (NTxNode child : node.children()) {
            collectNodes(child, result);
        }
    }

    private static void collectPropertyNames(NTxNode node, Set<String> result) {
        node.getProperties().forEach(p -> result.add(p.getName()));
        for (NTxStyleRule rule : node.rules()) {
            rule.styles().values().forEach(p -> result.add(p.getName()));
        }
        for (NTxNode child : node.children()) {
            collectPropertyNames(child, result);
        }
    }

    private static String valueOf(NTxPropCalculator calculator, NTxNode node, String... names) {
        return calculator.computeProperty(node, names)
                .map(p -> p.getValue().asStringValue().orElse(p.getValue().toString()))
                .orNull();
    }

    private static void assertEquivalent(NTxPropCalculator calculator, NTxNode node, String... names) {
        String previous = System.getProperty("ntexup.style.legacy");
        try {
            System.clearProperty("ntexup.style.legacy");
            var cached = calculator.computePropertyMagnitude(node, names);
            System.setProperty("ntexup.style.legacy", "true");
            var legacy = calculator.computePropertyMagnitude(node, names);
            assertEquals(legacy.isPresent(), cached.isPresent(), "presence mismatch for " + List.of(names));
            if (legacy.isPresent()) {
                NTxStyleAndMagnitude a = legacy.get();
                NTxStyleAndMagnitude b = cached.get();
                assertEquals(a.getStyle(), b.getStyle(), "style mismatch for " + List.of(names));
                assertEquals(0, a.getMagnetude().compareTo(b.getMagnetude()),
                        "magnitude mismatch for " + List.of(names));
            }
        } finally {
            if (previous == null) {
                System.clearProperty("ntexup.style.legacy");
            } else {
                System.setProperty("ntexup.style.legacy", previous);
            }
        }
    }

    private static void assertPluralMatchesLegacySingles(NTxPropCalculator calculator, NTxNode node) {
        List<NTxStyleAndMagnitude> plural = calculator.computePropertiesMagnitude(node);
        for (NTxStyleAndMagnitude cached : plural) {
            String previous = System.getProperty("ntexup.style.legacy");
            try {
                System.setProperty("ntexup.style.legacy", "true");
                var legacy = calculator.computePropertyMagnitude(node, new String[]{cached.getStyle().getName()});
                assertTrue(legacy.isPresent(), "Missing legacy property " + cached.getStyle().getName());
                assertEquals(cached.getStyle(), legacy.get().getStyle(),
                        "Plural style mismatch for " + cached.getStyle().getName());
                NTxStyleMagnitude expected = legacy.get().getMagnetude();
                assertEquals(0, expected.compareTo(cached.getMagnetude()),
                        "Plural magnitude mismatch for " + cached.getStyle().getName());
            } finally {
                if (previous == null) {
                    System.clearProperty("ntexup.style.legacy");
                } else {
                    System.setProperty("ntexup.style.legacy", previous);
                }
            }
        }
    }
}
