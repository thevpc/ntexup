package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.document.NTxPropCalculator;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.parser.resources.NTxSourceNew;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain main-based tests for the class-inheritance + table-substructure
 * styling extension (class- definitions, @() usage combination, and
 * table-row / table-cell / table-column selectors).
 */
public class TestStyleSystem {

    private static final NTxSource SRC = new NTxSourceNew();
    private static DefaultNTxEngine engine;
    private static NTxPropCalculator calc;
    private static int passed = 0;

    static DefaultNTxEngine engine() {
        if (engine == null) {
            engine = new DefaultNTxEngine();
        }
        return engine;
    }

    static NTxPropCalculator calc() {
        if (calc == null) {
            calc = new NTxPropCalculator(engine());
        }
        return calc;
    }

    public static void main(String[] args) {
        try {
            Nuts.openWorkspace().share();

            testSingleBaseInheritance();
            testMultiBaseLastWins();
            testDeepChain();
            testUsageSiteOrder();
            testInstancePropWins();
            testNearestScope();
            testSpecificityBeatsClass();
            testRecencyTieBreak();
            testDotSelectorRemoval();
            testAllKeyword();
            testTableSelectors();
            testTableResolution();
            testE2eClasses();
            testE2eAllKeyword();
            testE2eLoad();
            testEmptySelectorIsNone();
            testScopedClassResolution();
            testLegacyDotDoesNotLeak();
            testAndSelectors();
            testTableHeaderAndKeyedFactories();
            testStructuralContentInheritance();
            testNestedTableInnerShadowsOuter();
            testE2eTableStylesGrammar();
            testE2eNeverFailLegacy();
            testE2eClassExtendsKeyword();

            System.out.println("ALL TESTS PASSED (" + passed + ")");
        } catch (AssertionError e) {
            System.out.println("FAILED: " + e.getMessage());
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    static void assertEq(String label, Object expected, Object actual) {
        passed++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(label + " : expected=" + expected + " actual=" + actual);
        }
    }

    static void assertTrue(String label, boolean b) {
        passed++;
        if (!b) {
            throw new AssertionError(label);
        }
    }

    static String propOf(NTxNode n, String prop) {
        NTxProp p = calc().computeProperty(n, new String[]{prop}).orNull();
        if (p == null || p.getValue() == null || p.getValue().isNull()) {
            return null;
        }
        return p.getValue().asStringValue().orElse(p.getValue().toString());
    }

    static String colorOf(NTxNode n) {
        return propOf(n, NTxPropName.COLOR);
    }

    static void collectByType(NTxNode n, String type, List<NTxNode> out) {
        if (type.equals(n.type())) {
            out.add(n);
        }
        for (NTxNode c : n.children()) {
            collectByType(c, type, out);
        }
    }

    static void def(NTxNode container, String name, List<String> bases, NTxProp... props) {
        container.addRule(DefaultNTxStyleRule.ofClassDef(container, SRC, name, bases, props));
    }

    static void typeRule(NTxNode container, String type, NTxProp... props) {
        container.addRule(DefaultNTxStyleRule.of(container, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.of(type, (NTxLogger) null).get()), props));
    }

    static void anyRule(NTxNode container, NTxProp... props) {
        container.addRule(DefaultNTxStyleRule.of(container, SRC, DefaultNTxNodeSelector.ofAny(), props));
    }

    static NTxNode textNode() {
        DefaultNTxNode n = new DefaultNTxNode(NTxNodeType.TEXT);
        n.setSource(SRC);
        return n;
    }

    static NTxNode rowNode(String section, Integer bodyRow, int absIndex) {
        NTxNode n = engine().newDefaultNode(NTxNodeType.TABLE_ROW);
        n.setSource(SRC);
        n.setProperty(NTxProp.ofString(NTxPropName.SECTION, section));
        if (bodyRow != null) {
            n.setProperty(NTxProp.ofInt(NTxPropName.BODY_ROW, bodyRow));
        }
        n.setProperty(NTxProp.ofInt(NTxPropName.ROW_INDEX, absIndex));
        return n;
    }

    static NTxNode cellNode(int row, int col) {
        NTxNode n = engine().newDefaultNode(NTxNodeType.TABLE_CELL);
        n.setSource(SRC);
        n.setProperty(NTxProp.ofInt(NTxPropName.ROW_INDEX, row));
        n.setProperty(NTxProp.ofInt(NTxPropName.COL_INDEX, col));
        return n;
    }

    // ------------------------------------------------------------------
    // class inheritance tests
    // ------------------------------------------------------------------

    static void testSingleBaseInheritance() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "base-cell", null, NTxProp.ofString(NTxPropName.COLOR, "red"));
        def(root, "header-cell", List.of("base-cell"), NTxProp.ofString(NTxPropName.FONT_SIZE, "12"));
        NTxNode n = textNode();
        root.addChild(n);
        n.addStyleClass("header-cell");
        assertEq("single-base: inherited color", "red", colorOf(n));
    }

    static void testMultiBaseLastWins() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "red-base", null, NTxProp.ofString(NTxPropName.COLOR, "red"));
        def(root, "blue-base", null, NTxProp.ofString(NTxPropName.COLOR, "blue"));
        def(root, "both", List.of("red-base", "blue-base"));
        NTxNode n = textNode();
        root.addChild(n);
        n.addStyleClass("both");
        assertEq("multi-base: last listed base wins", "blue", colorOf(n));
    }

    static void testDeepChain() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "a", null, NTxProp.ofString(NTxPropName.COLOR, "red"));
        def(root, "b", List.of("a"), NTxProp.ofString(NTxPropName.FONT_SIZE, "12"));
        def(root, "c", List.of("b"), NTxProp.ofString(NTxPropName.FONT_BOLD, "true"));
        NTxNode n = textNode();
        root.addChild(n);
        n.addStyleClass("c");
        assertEq("deep chain: flatten resolves color", "red", colorOf(n));
    }

    static void testUsageSiteOrder() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "x", null, NTxProp.ofString(NTxPropName.COLOR, "red"));
        def(root, "y", null, NTxProp.ofString(NTxPropName.COLOR, "blue"));
        NTxNode n1 = textNode();
        root.addChild(n1);
        n1.addStyleClass("x");
        n1.addStyleClass("y");
        assertEq("usage order x,y -> y wins", "blue", colorOf(n1));
        NTxNode n2 = textNode();
        root.addChild(n2);
        n2.addStyleClass("y");
        n2.addStyleClass("x");
        assertEq("usage order y,x -> x wins", "red", colorOf(n2));
    }

    static void testInstancePropWins() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "x", null, NTxProp.ofString(NTxPropName.COLOR, "red"));
        NTxNode n = textNode();
        root.addChild(n);
        n.addStyleClass("x");
        n.setProperty(NTxProp.ofString(NTxPropName.COLOR, "green"));
        assertEq("instance prop wins over class", "green", colorOf(n));
    }

    static void testNearestScope() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "title", null, NTxProp.ofString(NTxPropName.COLOR, "red"));
        DefaultNTxNode nested = new DefaultNTxNode(NTxNodeType.GROUP);
        root.addChild(nested);
        def(nested, "title", null, NTxProp.ofString(NTxPropName.COLOR, "blue"));

        NTxNode inside = textNode();
        nested.addChild(inside);
        inside.addStyleClass("title");
        assertEq("nearest styles{} wins (nested)", "blue", colorOf(inside));

        NTxNode outside = textNode();
        root.addChild(outside);
        outside.addStyleClass("title");
        assertEq("fallback to outer styles{}", "red", colorOf(outside));
    }

    static void testSpecificityBeatsClass() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "x", null, NTxProp.ofString(NTxPropName.COLOR, "red"));
        typeRule(root, "text", NTxProp.ofString(NTxPropName.COLOR, "green"));
        NTxNode n = textNode();
        root.addChild(n);
        n.addStyleClass("x");
        assertEq("explicit type selector beats class fold", "green", colorOf(n));
    }

    static void testRecencyTieBreak() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        anyRule(root, NTxProp.ofString(NTxPropName.COLOR, "first"));
        anyRule(root, NTxProp.ofString(NTxPropName.COLOR, "second"));
        NTxNode n = textNode();
        root.addChild(n);
        assertEq("same specificity: later entry wins", "second", colorOf(n));
    }

    // ------------------------------------------------------------------
    // selector tests
    // ------------------------------------------------------------------

    static void testDotSelectorRemoval() {
        NOptional<NTxStyleRuleSelectorItem> r = NTxStyleRuleSelectorItem.of(".H1", (NTxLogger) null);
        assertTrue("dot selector is rejected", r.isEmpty());
    }

    static void testAllKeyword() {
        assertEq("bare 'all' == ANY", DefaultNTxNodeSelector.ANY_ITEM,
                NTxStyleRuleSelectorItem.of("all", (NTxLogger) null).get());
        assertEq("'all' parses as selector", false,
                NTxStyleRuleSelectorItem.of("all", (NTxLogger) null).isEmpty());
    }

    static void testTableSelectors() {
        // header row, 2 body rows, 1 footer row ; full data row indices 1..4
        NTxNode header = rowNode("header", null, 1);
        NTxNode body1 = rowNode("body", 1, 2);
        NTxNode body2 = rowNode("body", 2, 3);
        NTxNode footer = rowNode("footer", null, 4);

        NTxStyleRuleSelectorItem h = NTxStyleRuleSelectorItem.ofTableRow("header");
        NTxStyleRuleSelectorItem even = NTxStyleRuleSelectorItem.ofTableRow("even");
        NTxStyleRuleSelectorItem odd = NTxStyleRuleSelectorItem.ofTableRow("odd");

        assertTrue("table-row(header) matches header", h.acceptNode(header));
        assertTrue("table-row(header) not body", !h.acceptNode(body1));
        assertTrue("table-row(even) not header", !even.acceptNode(header));
        assertTrue("table-row(odd) matches first body row", odd.acceptNode(body1));
        assertTrue("table-row(even) matches second body row (header not counted)", even.acceptNode(body2));
        assertTrue("table-row(odd) not second body row", !odd.acceptNode(body2));
        assertTrue("zebra excludes footer", !even.acceptNode(footer) && !odd.acceptNode(footer));

        NTxNode c11 = cellNode(1, 1);
        NTxNode c12 = cellNode(1, 2);
        NTxNode c21 = cellNode(2, 1);
        NTxStyleRuleSelectorItem exact = NTxStyleRuleSelectorItem.ofTableCell(1, 1);
        NTxStyleRuleSelectorItem rowOnly = NTxStyleRuleSelectorItem.ofTableCell(1, null);
        NTxStyleRuleSelectorItem colOnly = NTxStyleRuleSelectorItem.ofTableCell(null, 1);
        NTxStyleRuleSelectorItem column1 = NTxStyleRuleSelectorItem.ofTableColumn(1);
        assertTrue("table-cell(row:,col:) exact match", exact.acceptNode(c11));
        assertTrue("table-cell(row:,col:) not other cell", !exact.acceptNode(c12));
        assertTrue("table-cell(row:) matches all cols of row", rowOnly.acceptNode(c11) && rowOnly.acceptNode(c12));
        assertTrue("table-cell(row:) not other rows", !rowOnly.acceptNode(c21));
        assertTrue("table-cell(col:) matches all rows of col", colOnly.acceptNode(c11) && colOnly.acceptNode(c21));
        assertTrue("table-column(n) matches coln cells", column1.acceptNode(c11) && column1.acceptNode(c21));
        assertTrue("table-column(n) not other cols", !column1.acceptNode(c12));
    }

    static void testTableResolution() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        def(root, "zebra-row", null, NTxProp.ofString(NTxPropName.COLOR, "gray"));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableRow("header")),
                NTxProp.ofString(NTxPropName.COLOR, "header-red")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableColumn(1)),
                NTxProp.ofString(NTxPropName.COLOR, "col1-purple")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableColumn(2)),
                NTxProp.ofString(NTxPropName.COLOR, "col2-blue")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableCell(2, 2)),
                NTxProp.ofString(NTxPropName.COLOR, "cell22-green")));

        for (int tr = 1; tr <= 3; tr++) {
            NTxNode row = rowNode(tr == 1 ? "header" : "body", tr == 1 ? null : tr - 1, tr);
            root.addChild(row);
            for (int c = 1; c <= 2; c++) {
                row.addChild(cellNode(tr, c));
            }
        }

        applyClass(row(root, 1), "zebra-row");
        applyClass(row(root, 2), "zebra-row");
        applyClass(row(root, 3), "zebra-row");

        // structural table-row(header) beats the class fold at equal distance on the header row
        assertEq("header row -> table-row(header) beats class", "header-red", colorOf(row(root, 1)));
        // body rows have no competing structural rule -> class fold wins
        assertEq("body row -> class fold", "gray", colorOf(row(root, 2)));
        assertEq("body row -> class fold", "gray", colorOf(row(root, 3)));

        // header cell col1 : table-row(header) does not match cells, table-column(1) wins
        assertEq("header cell col1 -> col1 rule", "col1-purple", colorOf(cell(root, 1, 1)));
        // header cell col2 -> col2 rule
        assertEq("header cell col2 -> col2 rule", "col2-blue", colorOf(cell(root, 1, 2)));
        // body cell 2/2 : explicit table-cell(row:,col:) beats table-column(2)
        assertEq("explicit table-cell beats table-column", "cell22-green", colorOf(cell(root, 2, 2)));
        assertEq("cell 2/1 -> col1 rule", "col1-purple", colorOf(cell(root, 2, 1)));
    }

    static void testStructuralContentInheritance() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableRow("header")),
                NTxProp.ofString(NTxPropName.COLOR, "h-red")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableRow("even")),
                NTxProp.ofString(NTxPropName.BACKGROUND_COLOR, "zebra")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableColumn(3)),
                NTxProp.ofString(NTxPropName.COLOR, "col3-violet")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableCell(1, 1)),
                NTxProp.ofString(NTxPropName.COLOR, "c11-teal")));

        NTxNode table = engine().newDefaultNode(NTxNodeType.TABLE);
        table.setSource(SRC);
        root.addChild(table);
        NTxNode hrow = rowNode("header", null, 1);
        table.addChild(hrow);
        NTxNode hc1 = cellNode(1, 1);
        NTxNode hc2 = cellNode(1, 2);
        NTxNode hc3 = cellNode(1, 3);
        hrow.addChild(hc1);
        hrow.addChild(hc2);
        hrow.addChild(hc3);
        NTxNode ht1 = textNode();
        NTxNode ht2 = textNode();
        NTxNode ht3 = textNode();
        hc1.addChild(ht1);
        hc2.addChild(ht2);
        hc3.addChild(ht3);
        // body rows : one odd (body-row 1) and one even (body-row 2)
        NTxNode brow1 = rowNode("body", 1, 2);
        NTxNode brow2 = rowNode("body", 2, 3);
        table.addChild(brow1);
        table.addChild(brow2);
        NTxNode bc11 = cellNode(2, 1), bc13 = cellNode(2, 3);
        NTxNode bc21 = cellNode(3, 1), bc23 = cellNode(3, 3);
        brow1.addChild(bc11); brow1.addChild(cellNode(2, 2)); brow1.addChild(bc13);
        brow2.addChild(bc21); brow2.addChild(cellNode(3, 2)); brow2.addChild(bc23);
        NTxNode bt11 = textNode(); NTxNode bt13 = textNode();
        NTxNode bt21 = textNode(); NTxNode bt23 = textNode();
        bc11.addChild(bt11); bc13.addChild(bt13);
        bc21.addChild(bt21); bc23.addChild(bt23);

        // row-level: header color reaches the header cell AND its content
        assertEq("header row itself", "h-red", colorOf(hrow));
        assertEq("header cell(1,2) inherits row color", "h-red", colorOf(hc2));
        assertEq("header cell(1,1) cell rule beats row rule", "c11-teal", colorOf(hc1));
        assertEq("header cell(1,2) content inherits row color", "h-red", colorOf(ht2));
        assertEq("header cell(1,1) content: cell rule beats row rule", "c11-teal", colorOf(ht1));
        assertEq("header cell(1,3) content: col3 rule wins tie", "col3-violet", colorOf(ht3));
        assertEq("body cell content not header colored", "#1A1A1A", colorOf(bt11));
        assertEq("body cell content not header colored", "#1A1A1A", colorOf(bt11));

        // zebra: body-row parity reaches content of even body rows only
        assertEq("even body row bg", "zebra", propOf(brow2, NTxPropName.BACKGROUND_COLOR));
        assertEq("even body row content bg", "zebra", propOf(bt21, NTxPropName.BACKGROUND_COLOR));
        assertEq("odd body row content bg untouched", null, propOf(bt11, NTxPropName.BACKGROUND_COLOR));
        assertEq("header content bg untouched", null, propOf(ht1, NTxPropName.BACKGROUND_COLOR));

        // column-level: col3 color reaches every col3 cell and its content
        assertEq("col3 cell", "col3-violet", colorOf(hc3));
        assertEq("col3 header content", "col3-violet", colorOf(ht3));
        assertEq("col3 body content", "col3-violet", colorOf(bt23));
        assertEq("col1 body content not col3 colored", "#1A1A1A", colorOf(bt11));

        // cell-level exact: (1,1) rule wins for the cell and its content
        assertEq("cell(1,1) wins over col3", "c11-teal", colorOf(hc1));
        assertEq("cell(1,1) content wins", "c11-teal", colorOf(ht1));

        // content outside any table is never matched by structural selectors
        NTxNode plain = textNode();
        root.addChild(plain);
        assertEq("plain text outside table unmatched", "#1A1A1A", colorOf(plain));
        assertEq("plain text outside table unmatched (bg)", null, propOf(plain, NTxPropName.BACKGROUND_COLOR));
    }

    static void testNestedTableInnerShadowsOuter() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableCell(3, 4)),
                NTxProp.ofString(NTxPropName.COLOR, "outer-blue")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableCell(1, 2)),
                NTxProp.ofString(NTxPropName.COLOR, "inner-green")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC,
                DefaultNTxNodeSelector.of(NTxStyleRuleSelectorItem.ofTableRow("even")),
                NTxProp.ofString(NTxPropName.COLOR, "inner-row-paint")));

        // outer table, outer cell at flattened (3,4)
        NTxNode outerTable = engine().newDefaultNode(NTxNodeType.TABLE);
        outerTable.setSource(SRC);
        root.addChild(outerTable);
        NTxNode outerRow = rowNode("body", 1, 1);
        outerTable.addChild(outerRow);
        NTxNode outerCell = cellNode(3, 4);
        outerRow.addChild(outerCell);
        NTxNode outerText = textNode();
        outerCell.addChild(outerText);

        // inner table inside the outer cell, inner cell at (1,2)
        NTxNode innerTable = engine().newDefaultNode(NTxNodeType.TABLE);
        innerTable.setSource(SRC);
        outerCell.addChild(innerTable);
        NTxNode innerRow = rowNode("body", 2, 1);
        innerTable.addChild(innerRow);
        NTxNode innerCell = cellNode(1, 2);
        innerRow.addChild(innerCell);
        NTxNode innerText = textNode();
        innerCell.addChild(innerText);

        assertEq("outer cell(3,4) matches its own rule", "outer-blue", colorOf(outerCell));
        assertEq("outer cell content inherits outer rule", "outer-blue", colorOf(outerText));
        assertEq("inner cell(1,2) matches inner rule", "inner-green", colorOf(innerCell));
        // innermost context shadows the outer one: the (3,4) rule never reaches inside
        assertEq("inner content sees only inner cell context", "inner-green", colorOf(innerText));
        // zebra color of the inner row (bodyRow 2 = even) reaches the inner content;
        // the exact cell(1,2) rule wins over the row rule by higher specificity
        assertEq("inner row is even", "inner-row-paint", colorOf(innerRow));
        assertEq("inner content: cell rule beats row rule", "inner-green", colorOf(innerText));
        // a row node is never matched by table-cell / table-column rules
        assertEq("outer row not matched by cell rule", "#1A1A1A", colorOf(outerRow));
    }

    static NTxNode row(NTxNode root, int i) {
        return root.children().get(i - 1);
    }

    static NTxNode cell(NTxNode root, int i, int j) {
        return row(root, i).children().get(j - 1);
    }

    static void applyClass(NTxNode n, String cls) {
        n.addStyleClass(cls);
    }

    static void testE2eClasses() {
        String doc = "" +
                "styles{\n" +
                "   class-a: { color: red }\n" +
                "   class-b(a): { font-size: 12 }\n" +
                "   class-c(b): { font-bold }\n" +
                "}\n" +
                "page {\n" +
                "   @(c) text(\"t1\")\n" +
                "   @(b) text(\"t2\")\n" +
                "}\n";
        CaptureLogger cap = new CaptureLogger();
        DefaultNTxEngine e = new DefaultNTxEngine();
        e.addLog(cap);
        e.loadDocument(new ByteArrayInputStream(doc.getBytes()));
        assertTrue("e2e class definitions parse clean: " + errorMsgs(cap),
                errorMsgs(cap).isEmpty());
    }

    static void testE2eAllKeyword() {
        String doc = "" +
                "styles{\n" +
                "   all: { color: \"#222222\" }\n" +
                "}\n" +
                "text(\"t3\")\n";
        CaptureLogger cap = new CaptureLogger();
        DefaultNTxEngine e = new DefaultNTxEngine();
        e.addLog(cap);
        e.loadDocument(new ByteArrayInputStream(doc.getBytes()));
        assertTrue("e2e all rule parse clean: " + errorMsgs(cap), errorMsgs(cap).isEmpty());
    }

    static List<String> errorMsgs(CaptureLogger cap) {
        List<String> err = new ArrayList<>();
        for (String m : cap.msgs) {
            if (m.contains("invalid style rule") || m.contains("unable to resolve style selector")
                    || m.contains("invalid base class") || m.contains("compile document failed")) {
                err.add(m);
            }
        }
        return err;
    }

    static void testEmptySelectorIsNone() {
        DefaultNTxNodeSelector s = DefaultNTxNodeSelector.of();
        assertTrue("empty selector must not match everything", !s.acceptNode(textNode()));
        DefaultNTxNodeSelector s2 = DefaultNTxNodeSelector.of((NTxStyleRuleSelectorItem) null);
        assertTrue("null-item selector must not match everything", !s2.acceptNode(textNode()));
        DefaultNTxNodeSelector s3 = DefaultNTxNodeSelector.of(new NTxStyleRuleSelectorItem[0]);
        assertTrue("empty item array must not match everything", !s3.acceptNode(textNode()));
    }

    static void testScopedClassResolution() {
        String doc = "" +
                "page {\n" +
                "   styles{\n" +
                "      class-title: { position:(50,32), origin:center, font-size:8%P, color:\"#0f172a\" }\n" +
                "   }\n" +
                "   @(title) text(\"T\")\n" +
                "}\n";
        DefaultNTxEngine e = new DefaultNTxEngine();
        capture(e);
        NTxCompiledDocument d = e.loadDocument(new ByteArrayInputStream(doc.getBytes()));
        NTxNode page = d.pages().get(0).compiledPage();
        NTxNode title = page.children().get(0);
        assertEq("scoped class-title position", "(50,32)",
                String.valueOf(e.computeProperty(title, new String[]{"position"}).map(NTxProp::getValue).orNull()));
        assertEq("scoped class-title font-size", "8%P",
                String.valueOf(e.computeProperty(title, new String[]{"font-size"}).map(NTxProp::getValue).orNull()));
        assertEq("scoped class-title color", "\"#0f172a\"",
                String.valueOf(e.computeProperty(title, new String[]{"color"}).map(NTxProp::getValue).orNull()));
    }

    static void testLegacyDotDoesNotLeak() {
        String doc = "" +
                "page {\n" +
                "   styles{\n" +
                "      (.subtitle): { position:(50,44), color:\"#c0ffee\", font-size:3%P }\n" +
                "      class-title: { position:(50,32), color:\"#000000\" }\n" +
                "   }\n" +
                "   @(title) text(\"T\")\n" +
                "}\n";
        DefaultNTxEngine e = new DefaultNTxEngine();
        capture(e);
        NTxCompiledDocument d = e.loadDocument(new ByteArrayInputStream(doc.getBytes()));
        NTxNode page = d.pages().get(0).compiledPage();
        NTxNode title = page.children().get(0);
        assertEq("legacy dot must not shadow class-title position", "(50,32)",
                String.valueOf(e.computeProperty(title, new String[]{"position"}).map(NTxProp::getValue).orNull()));
        assertEq("legacy dot must not leak color", "\"#000000\"",
                String.valueOf(e.computeProperty(title, new String[]{"color"}).map(NTxProp::getValue).orNull()));
        String fs = propOf2(e, title, "font-size");
        assertTrue("legacy dot must not leak font-size, was " + fs, !Objects.equals("3%P", fs));
    }

    static String propOf2(NTxEngine e, NTxNode n, String prop) {
        NTxProp p = e.computeProperty(n, new String[]{prop}).orNull();
        if (p == null || p.getValue() == null || p.getValue().isNull()) {
            return null;
        }
        return p.getValue().asStringValue().orElse(p.getValue().toString());
    }

    static void testAndSelectors() {
        DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP);

        NTxStyleRuleSelectorItem tableType = NTxStyleRuleSelectorItem.of(new String[]{"table"}, new String[0]);
        NTxStyleRuleSelectorItem useImportant = NTxStyleRuleSelectorItem.ofClassUse("important");
        DefaultNTxNodeSelector tableOnly = DefaultNTxNodeSelector.of(tableType);
        DefaultNTxNodeSelector tableAndImportant = DefaultNTxNodeSelector.of(tableType, useImportant);

        NTxNode tableA = engine().newDefaultNode(NTxNodeType.TABLE);
        tableA.addStyleClass("important");
        root.addChild(tableA);
        NTxNode tableB = engine().newDefaultNode(NTxNodeType.TABLE);
        root.addChild(tableB);
        NTxNode textA = textNode();
        textA.addStyleClass("important");
        root.addChild(textA);

        root.addRule(DefaultNTxStyleRule.of(root, SRC, tableOnly, NTxProp.ofString(NTxPropName.COLOR, "blue")));
        root.addRule(DefaultNTxStyleRule.of(root, SRC, tableAndImportant, NTxProp.ofString(NTxPropName.COLOR, "violet")));

        assertTrue("table(class-important) matches table with class", tableAndImportant.acceptNode(tableA));
        assertTrue("table(class-important) rejects table without class (AND)", !tableAndImportant.acceptNode(tableB));
        assertTrue("table(class-important) rejects non-table with class (AND)", !tableAndImportant.acceptNode(textA));
        assertTrue("type table matches any table", tableOnly.acceptNode(tableA) && tableOnly.acceptNode(tableB));
        assertTrue("type table rejects text", !tableOnly.acceptNode(textA));

        assertEq("2-constraint selector beats 1-constraint", "violet", colorOf(tableA));
        assertEq("1-constraint selector applies to plain table", "blue", colorOf(tableB));
        String tc = colorOf(textA);
        assertTrue("text with class unaffected by table rules, was " + tc,
                !"blue".equals(tc) && !"violet".equals(tc));
    }

    static void testTableHeaderAndKeyedFactories() {
        NTxNode header = rowNode("header", null, 1);
        NTxNode body1 = rowNode("body", 1, 2);
        NTxNode footer = rowNode("footer", null, 4);

        NTxStyleRuleSelectorItem th = NTxStyleRuleSelectorItem.of("table-header", null).get();
        assertTrue("table-header matches header row", th.acceptNode(header));
        assertTrue("table-header rejects body row", !th.acceptNode(body1));
        assertTrue("table-header rejects footer", !th.acceptNode(footer));

        NTxStyleRuleSelectorItem even = NTxStyleRuleSelectorItem.of("table-row(row: even)", null).get();
        assertTrue("table-row(row: even) matches even body row", even.acceptNode(rowNode("body", 2, 3)));
        assertTrue("table-row(row: even) rejects odd", !even.acceptNode(body1));

        NTxStyleRuleSelectorItem col = NTxStyleRuleSelectorItem.of("table-column(col: 2)", null).get();
        assertTrue("table-column(col: 2) matches col2 cells", col.acceptNode(cellNode(2, 2)));
        assertTrue("table-column(col: 2) rejects col1", !col.acceptNode(cellNode(2, 1)));

        NTxStyleRuleSelectorItem cell = NTxStyleRuleSelectorItem.of("table-cell(row: 2, col: 2)", null).get();
        assertTrue("table-cell(row:2,col:2) matches", cell.acceptNode(cellNode(2, 2)));
        assertTrue("table-cell(row:2,col:2) rejects (1,1)", !cell.acceptNode(cellNode(1, 1)));

        NTxNode evenImp = rowNode("body", 2, 3);
        evenImp.addStyleClass("important");
        NTxNode oddImp = rowNode("body", 1, 2);
        oddImp.addStyleClass("important");
        DefaultNTxNodeSelector evenAndImportant = DefaultNTxNodeSelector.of(even, NTxStyleRuleSelectorItem.ofClassUse("important"));
        assertTrue("table-row(row: even) AND class-important matches even+class", evenAndImportant.acceptNode(evenImp));
        assertTrue("table-row(row: even) AND class-important rejects even no-class", !evenAndImportant.acceptNode(body1));
        assertTrue("table-row(row: even) AND class-important rejects odd+class", !evenAndImportant.acceptNode(oddImp));

        CaptureLogger cap = new CaptureLogger();
        NTxStyleRuleSelectorItem legacy = NTxStyleRuleSelectorItem.of("table-row(header)", cap).get();
        assertTrue("legacy table-row(header) resolves best-effort", legacy.acceptNode(header));
        assertTrue("legacy table-row(header) warns", String.join("\n", cap.msgs).contains("deprecated"));
    }

    static void testE2eTableStylesGrammar() {
        String doc = "" +
                "styles{\n" +
                "   class-important: { color: red }\n" +
                "   class-b(extends: important): { font-bold }\n" +
                "   table-header: { background-color: \"#1e293b\", color: \"#ffffff\" }\n" +
                "   table-row(row: even): { background-color: \"#eef2ff\", color: \"#1e293b\" }\n" +
                "   table-row(row: odd): { background-color: \"#ffffff\" }\n" +
                "   table-row(row: even, class-important): { background-color: \"#fef3c7\" }\n" +
                "   table-column(col: 1): { font-bold }\n" +
                "   table-cell(row: 1, col: 1): { background-color: \"#7c3aed\", color: \"#ffffff\" }\n" +
                "   table(class-important): { background-color: \"#fef3c7\" }\n" +
                "}\n";
        DefaultNTxEngine e = new DefaultNTxEngine();
        e.loadDocument(new ByteArrayInputStream(doc.getBytes()));
    }

    static void testE2eNeverFailLegacy() {
        String doc = "" +
                "styles{\n" +
                "   table-row(header): { color: red }\n" +
                "   table-row(bogus): { color: blue }\n" +
                "}\n";
        DefaultNTxEngine e = new DefaultNTxEngine();
        NTxCompiledDocument d = e.loadDocument(new ByteArrayInputStream(doc.getBytes()));
        assertTrue("never-fail: mixed legacy+bogus table selectors compile", d != null);
    }

    static void testE2eClassExtendsKeyword() {
        String doc = "" +
                "styles{\n" +
                "   class-base: { color: \"#123456\", font-bold }\n" +
                "   class-child(extends: base): { color: \"#abcdef\" }\n" +
                "}\n" +
                "page{\n" +
                "   @(child) text(\"t\")\n" +
                "}\n";
        DefaultNTxEngine e = new DefaultNTxEngine();
        NTxCompiledDocument d = e.loadDocument(new ByteArrayInputStream(doc.getBytes()));
        NTxNode page = d.pages().get(0).compiledPage();
        NTxNode text = page.children().get(0);
        assertEq("extends: base applies flattened props", "#abcdef",
                e.computeProperty(text, new String[]{"color"}).map(NTxProp::getValue).map(v -> v.asStringValue().orNull()).orNull());
        NTxValue v = NTxValue.of(e.computeProperty(text, new String[]{"font-bold"}).map(NTxProp::getValue).orNull());
        assertTrue("extends: base inherits font-bold: " + v.asElement().orNull(), v.asBoolean().orElse(false));
        assertTrue("extends: keyword document compiled", d != null);
    }

    static void capture(DefaultNTxEngine e) {
        CaptureLogger cap = new CaptureLogger();
        e.addLog(cap);
    }

    static void testE2eLoad() {
        String oldDotDoc = "styles{ .H1: { font-size: 20%P } }\ntext(\"x\")\n";
        DefaultNTxEngine e = new DefaultNTxEngine();
        e.loadDocument(new ByteArrayInputStream(oldDotDoc.getBytes()));
        String hint = hintForLegacyDot(".H1");
        assertTrue("legacy dot removal hint mentions class-H1: " + hint, hint.contains("class-H1"));
        hintForLegacyDot("text.H1");
        passed++;
    }

    static String hintForLegacyDot(String sel) {
        CaptureLogger cap = new CaptureLogger();
        NTxStyleRuleSelectorItem.of(sel, cap);
        return String.join("\n", cap.msgs);
    }

    static final class CaptureLogger implements NTxLogger {
        final List<String> msgs = new ArrayList<>();

        @Override
        public void log(NMsg m) {
            msgs.add(m == null ? "" : m.toString());
        }

        @Override
        public void log(NTxMsg m) {
            msgs.add(m == null ? "" : m.toString());
        }

        @Override
        public void log(NMsg m, NTxSource s) {
            msgs.add(m + " @" + s);
        }
    }
}