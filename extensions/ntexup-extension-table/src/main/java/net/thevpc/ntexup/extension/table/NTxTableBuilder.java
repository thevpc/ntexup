package net.thevpc.ntexup.extension.table;

import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxInt2;
import net.thevpc.ntexup.api.document.elem2d.NTxMargin;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.parser.NTxAllArgumentReader;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NListContainerElement;
import net.thevpc.nuts.util.NOptional;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Table builder for ntexup.
 * Creates a table node that renders tabular data.
 */
public class NTxTableBuilder implements NTxNodeBuilder {

    /**
     * Transparent paint used to neutralize background leakage on cell content
     * nodes: cell/row background rules (e.g. {@code table-row(row: even)})
     * cascade into content descendants through the structural selectors, but
     * only the cell/row itself may paint a visible background. Giving the
     * content an explicit, fully transparent background keeps the generic text
     * renderer from painting a box over the cell background / grid lines.
     */
    private static final NElement NO_BACKGROUND = NElement.ofCustom(new Color(0, 0, 0, 0));

    private final NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("table")
                .parseParam().matchesNamedPair(
                        "header-data",
                        "footer-data",
                        "data"

                ).end()
                .selfBounds2D(this::selfBounds2D)
                .sizeRequirements(this::sizeRequirements)
                .renderComponent(this::renderMain)
        ;
    }

    private static String asStr(NElement e) {
        if (e == null || e.isNull()) {
            return "";
        }
        if (e.isAnyStringOrName()) {
            return e.asStringValue().orElse("");
        }
        return e.toString();
    }

    private void renderMain(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();

        // Build data from arguments: header + body + footer, or plain data
        TableData td = tableData(node);
        List<List<String>> data = td.data;
        List<String> sections = td.sections;

        // Determine number of rows and columns
        int rows = data.size();
        int cols = 0;
        for (List<String> row : data) {
            if (row.size() > cols) {
                cols = row.size();
            }
        }
        if (cols == 0) {
            cols = 1;
        }

        // Measure the table from its content: the table self-sizes instead of
        // filling the whole parent area (like text/flow nodes do), so the
        // rendered box stays compact inside grid/column cells.
        TableMeasure m = measure(rendererContext, node, td, cols);
        double pad = m.pad;
        double contentWidth = m.tableWidth - 2 * pad;
        double contentHeight = m.tableHeight - 2 * pad;

        // Materialize rows and cells as real child nodes so that structural
        // selectors (table-row / table-cell / table-column / table-header /
        // table-footer) can match them.
        List<NTxNode> rowNodes = materializeTable(rendererContext, node, data, sections, cols);

        // Per-cell row/column weights (max per row/column) resize the natural
        // content-driven proportions, exactly like a grid row/column weight
        // list. Global columns-weight / rows-weight lists still win when
        // present (grid convention).
        double[] colWidths = m.colWidths;
        double[] rowHeights = m.rowHeights;
        TableWeights w = computeWeights(rendererContext, rowNodes, rows, cols);
        double sumColWeights = 0;
        for (double cw : w.colWeights) {
            sumColWeights += cw;
        }
        if (w.haveColWeights && sumColWeights > 0 && m.tableWidth > 0) {
            for (int i = 0; i < cols; i++) {
                colWidths[i] = (w.colWeights[i] / sumColWeights) * m.tableWidth;
            }
        }
        double sumRowWeights = 0;
        for (double rw : w.rowWeights) {
            sumRowWeights += rw;
        }
        if (w.haveRowWeights && sumRowWeights > 0 && m.tableHeight > 0) {
            for (int i = 0; i < rows; i++) {
                rowHeights[i] = (w.rowWeights[i] / sumRowWeights) * m.tableHeight;
            }
        }

        NTxGraphics g = rendererContext.graphics();
        // Draw background
        boolean someBG = rendererContext.applyBackgroundColor();
        if (someBG) {
            // Color already set by applyBackgroundColor
            g.fillRect(rendererContext.selfBounds2D().minX(),
                    rendererContext.selfBounds2D().minY(),
                    rendererContext.selfBounds2D().widthX(),
                    rendererContext.selfBounds2D().widthY());
        }

        double x0 = rendererContext.selfBounds2D().minX();
        double y0 = rendererContext.selfBounds2D().minY();

        // Draw row backgrounds, cell backgrounds, then cell content
        // (real child nodes rendered by the generic pipeline).
        double cy = y0;
        for (int r = 0; r < rowNodes.size(); r++) {
            NTxNode rowNode = rowNodes.get(r);
            double rowH = rowHeights[Math.min(r, rowHeights.length - 1)];
            NTxBounds2D rowRect = NTxBounds2D.ofWidth(x0, cy, m.tableWidth, rowH);
            NTxRendererContext rowCtx = rendererContext.resolveNode(rowNode, rowRect);
            rowCtx.paintBackground(rowRect);
            double cx = x0;
            for (NTxNode cellNode : rowNode.children()) {
                double colW = cellColWidth(cellNode, colWidths);
                NTxBounds2D cellRect = NTxBounds2D.ofWidth(cx, cy, colW, rowH);
                NTxRendererContext cellCtx = rowCtx.resolveNode(cellNode, cellRect);
                cellCtx.paintBackground(cellRect);
                List<NTxNode> contentChildren = cellNode.children();
                if (!contentChildren.isEmpty() && colW > 2 * pad && rowH > 2 * pad) {
                    NTxBounds2D innerRect = NTxBounds2D.ofWidth(
                            cx + pad, cy + pad, colW - 2 * pad, rowH - 2 * pad);
                    for (NTxNode contentChild : contentChildren) {
                        renderContentChild(cellCtx, contentChild, innerRect);
                    }
                }
                cx += colW;
            }
            cy += rowH;
        }

        // Draw grid if enabled
        if (rendererContext.requireDrawGrid()) {
            rendererContext.applyGridColor(true);
            // Vertical lines
            double x = rendererContext.selfBounds2D().minX();
            for (int i = 0; i <= cols; i++) {
                g.drawLine(x, rendererContext.selfBounds2D().minY(),
                        x, rendererContext.selfBounds2D().minY() + contentHeight);
                if (i < cols) {
                    x += colWidths[i];
                }
            }
            // Horizontal lines
            double y = rendererContext.selfBounds2D().minY();
            for (int i = 0; i <= rows; i++) {
                g.drawLine(
                        rendererContext.selfBounds2D().minX(), y,
                        rendererContext.selfBounds2D().minX() + contentWidth, y
                );
                if (i < rows) {
                    y += rowHeights[i];
                }
            }
        }

        // Draw contour if enabled
        rendererContext.drawContour();
    }

    /**
     * Self-sizing bounds: the table measures its own content (widest cell text
     * per column, one text line per row) and reports a compact box anchored at
     * the standard top-left position, exactly like text/flow nodes do. Without
     * this, the table would fill its whole parent area and its rows would grow
     * to hundreds of pixels inside grid cells.
     */
    public NTxBounds2D selfBounds2D(NTxRendererContext ctx) {
        NTxNode node = ctx.node();
        TableData td = tableData(node);
        int cols = columnCount(td);
        TableMeasure m = measure(ctx, node, td, cols);
        if (m == null || m.tableWidth <= 0 || m.tableHeight <= 0) {
            return ctx.defaultSelfBounds2D();
        }
        return NTxValueByName.selfBounds2D(new NTxDouble2(m.tableWidth, m.tableHeight), null, ctx);
    }

    /**
     * Size requirements consistent with the self-sizing bounds, so laying
     * containers (grids, columns, pages) can pack rows to the table content.
     */
    public NTxSizeRequirements sizeRequirements(NTxRendererContext ctx) {
        NTxNode node = ctx.node();
        TableData td = tableData(node);
        int cols = columnCount(td);
        TableMeasure m = measure(ctx, node, td, cols);
        if (m == null || m.tableWidth <= 0 || m.tableHeight <= 0) {
            return new NTxSizeRequirements(0, 0, 0, 0, 0, 0);
        }
        return new NTxSizeRequirements(
                0, m.tableWidth, m.tableWidth,
                0, m.tableHeight, m.tableHeight
        );
    }

    private int columnCount(TableData td) {
        int cols = 0;
        for (List<String> row : td.data) {
            if (row.size() > cols) {
                cols = row.size();
            }
        }
        return Math.max(cols, 1);
    }

    /**
     * Extracts the table rows from the node props: either the
     * header-data / data / footer-data combination, or a plain data list.
     */
    private TableData tableData(NTxNode node) {
        List<List<String>> data = new ArrayList<>();
        List<String> sections = new ArrayList<>();
        List<List<String>> header = toRows(NTxValue.ofProp(node, "header-data").asElement().orNull());
        List<List<String>> body = toRows(NTxValue.ofProp(node, "data").asElement().orNull());
        List<List<String>> footer = toRows(NTxValue.ofProp(node, "footer-data").asElement().orNull());
        if (!header.isEmpty() || !body.isEmpty() || !footer.isEmpty()) {
            data.addAll(header);
            for (int i = 0; i < header.size(); i++) {
                sections.add("header");
            }
            data.addAll(body);
            for (int i = 0; i < body.size(); i++) {
                sections.add("body");
            }
            data.addAll(footer);
            for (int i = 0; i < footer.size(); i++) {
                sections.add("footer");
            }
        } else {
            data = body;
            if (data.isEmpty()) {
                data = List.of(
                        List.of("Header1", "Header2"),
                        List.of("Row1Col1", "Row1Col2")
                );
            }
            for (int i = 0; i < data.size(); i++) {
                sections.add("body");
            }
        }
        TableData td = new TableData();
        td.data = data;
        td.sections = sections;
        return td;
    }

    /** Converts a plain (non-named) list-of-lists element to rows of strings. */
    private static List<List<String>> toRows(NElement outer) {
        List<List<String>> result = new ArrayList<>();
        if (outer == null || outer.isNull()) {
            return result;
        }
        if (outer.isListContainer() && !outer.isNamed()) {
            NOptional<NListContainerElement> outerListOpt = outer.asListContainer();
            if (outerListOpt.isPresent()) {
                for (NElement rowElem : outerListOpt.get().children()) {
                    NTxValue rowValue = NTxValue.of(rowElem);
                    NOptional<NElement> rowElemOpt = rowValue.asElement();
                    if (rowElemOpt.isPresent()) {
                        NElement rowElemVal = rowElemOpt.get();
                        if (rowElemVal.isListContainer() && !rowElemVal.isNamed()) {
                            NOptional<NListContainerElement> rowListOpt = rowElemVal.asListContainer();
                            if (rowListOpt.isPresent()) {
                                List<String> rowData = new ArrayList<>();
                                for (NElement cellElem : rowListOpt.get().children()) {
                                    rowData.add(asStr(cellElem));
                                }
                                result.add(rowData);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Measures the table content and derives the natural column widths / row
     * heights / total size. Text is measured at the same clamped font that the
     * renderer will use, so the painted content always fits the box.
     */
    private TableMeasure measure(NTxRendererContext ctx, NTxNode node, TableData td, int cols) {
        List<List<String>> data = td.data;
        int rows = data.size();
        if (rows == 0) {
            return null;
        }
        double pad = 0.0;
        NTxMargin padding = NTxValueByName.getPadding(ctx);
        if (padding != null) {
            pad = padding.getLeft(); // use left as representative
        }
        NTxGraphics g = ctx.graphics().copy();
        try {
            double baseFont = ctx.getFontSize();
            ctx.applyFont();
            FontMetrics fmBig = g.getFontMetrics();
            double lineH0 = Math.max(fmBig.getAscent() + fmBig.getDescent(), 1);
            double useFont = Math.max(6, Math.min(baseFont, lineH0 * 0.55));
            FontMetrics fmSmall = g.getFontMetrics(fmBig.getFont().deriveFont((float) useFont));
            double lineH = Math.max(fmSmall.getAscent() + fmSmall.getDescent(), 1);
            double[] colWidths = new double[cols];
            for (int r = 0; r < rows; r++) {
                List<String> rowData = data.get(r);
                for (int c = 0; c < cols; c++) {
                    String text = c < rowData.size() ? rowData.get(c) : "";
                    if (!text.isEmpty()) {
                        double w = fmSmall.stringWidth(text);
                        if (w > colWidths[c]) {
                            colWidths[c] = w;
                        }
                    }
                }
            }
            double rowHeight = lineH + 2 * pad;
            double tableWidth = 0;
            for (int c = 0; c < cols; c++) {
                colWidths[c] = colWidths[c] + 2 * pad;
                tableWidth += colWidths[c];
            }
            double tableHeight = rows * rowHeight;
            TableMeasure m = new TableMeasure();
            m.colWidths = colWidths;
            m.rowHeights = new double[rows];
            Arrays.fill(m.rowHeights, rowHeight);
            m.rowHeight = rowHeight;
            m.pad = pad;
            m.cellFont = useFont;
            m.tableWidth = tableWidth;
            m.tableHeight = tableHeight;
            return m;
        } finally {
            g.dispose();
        }
    }

    /**
     * Renders one cell content node through the generic pipeline, laid out in
     * a box centered inside the cell. The font size is clamped so the text
     * always fits the row height (a plain table inherits the slide body font,
     * which is far too large for table cells), and horizontal/vertical
     * centering is computed from the real font metrics.
     */
    private void renderContentChild(NTxRendererContext cellCtx, NTxNode contentChild, NTxBounds2D innerRect) {
        double baseFont = cellCtx.getFontSize();
        double useFont = Math.max(6, Math.min(baseFont, innerRect.widthY() * 0.55));
        // Explicit px font size -> magnitude 0, wins the cascade and is used
        // both for measuring and painting, so the drawn text matches the box.
        contentChild.setProperty(NTxProp.ofObject(NTxPropName.FONT_SIZE, NElement.ofLong(Math.round(useFont), "px")));
        NTxRendererContext contentCtx = cellCtx.resolveNode(contentChild, innerRect);
        contentCtx.applyFont();
        NTxGraphics cg = contentCtx.graphics();
        String text = NTxValue.ofProp(contentChild, NTxPropName.VALUE).asString().orElse("");
        FontMetrics fm = cg.getFontMetrics();
        double th = fm.getAscent() + fm.getDescent();
        double tw = 0;
        if (!text.isEmpty()) {
            Rectangle2D tb = cg.getStringBounds(text);
            tw = tb.getWidth();
        }
        double bw = Math.min(tw, innerRect.widthX());
        double bh = Math.min(th, innerRect.widthY());
        double bx = innerRect.minX() + Math.max(0, (innerRect.widthX() - tw) / 2.0);
        double by = innerRect.minY() + Math.max(0, (innerRect.widthY() - th) / 2.0);
        NTxBounds2D contentBox = NTxBounds2D.ofWidth(bx, by, Math.max(1, bw), Math.max(1, bh));
        cellCtx.resolveNode(contentChild, contentBox).render();
    }

    private double cellColWidth(NTxNode cellNode, double[] colWidths) {
        int col = NTxValue.ofProp(cellNode, NTxPropName.COL_INDEX).asInt().orElse(1);
        return colWidths[Math.min(Math.max(col - 1, 0), colWidths.length - 1)];
    }

    private List<NTxNode> materializeTable(NTxRendererContext rendererContext, NTxNode table,
                                           List<List<String>> data, List<String> sections, int cols) {
        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        List<NTxNode> existing = table.children();
        if (!existing.isEmpty() && NTxNodeType.TABLE_ROW.equals(existing.get(0).type())) {
            return existing;
        }
        NTxEngine engine = rendererContext.engine();
        List<NTxNode> out = new ArrayList<>();
        int headerCounter = 0;
        int bodyCounter = 0;
        int footerCounter = 0;
        for (int r = 0; r < data.size(); r++) {
            String section = sections.get(Math.min(r, sections.size() - 1));
            List<String> rowData = data.get(r);
            NTxNode row = engine.newDefaultNode(NTxNodeType.TABLE_ROW);
            row.setSource(table.source());
            row.setProperty(NTxProp.ofString(NTxPropName.SECTION, section));
            row.setProperty(NTxProp.ofInt(NTxPropName.ROW_INDEX, r + 1));
            int sectionRow;
            if ("header".equals(section)) {
                sectionRow = ++headerCounter;
            } else if ("footer".equals(section)) {
                sectionRow = ++footerCounter;
            } else {
                sectionRow = ++bodyCounter;
            }
            row.setProperty(NTxProp.ofInt(NTxPropName.SECTION_ROW, sectionRow));
            if ("body".equals(section)) {
                row.setProperty(NTxProp.ofInt(NTxPropName.BODY_ROW, bodyCounter));
            }
            for (int c = 0; c < cols; c++) {
                String text = c < rowData.size() ? rowData.get(c) : "";
                NTxNode cell = engine.newDefaultNode(NTxNodeType.TABLE_CELL);
                cell.setSource(table.source());
                cell.setProperty(NTxProp.ofString(NTxPropName.SECTION, section));
                cell.setProperty(NTxProp.ofInt(NTxPropName.SECTION_ROW, sectionRow));
                if ("body".equals(section)) {
                    cell.setProperty(NTxProp.ofInt(NTxPropName.BODY_ROW, bodyCounter));
                }
                cell.setProperty(NTxProp.ofInt(NTxPropName.ROW_INDEX, r + 1));
                cell.setProperty(NTxProp.ofInt(NTxPropName.COL_INDEX, c + 1));
                cell.setProperty(NTxProp.ofString(NTxPropName.VALUE, text));
                if (!text.isEmpty()) {
                    // Real child node: cell content renders through the generic
                    // pipeline and inherits the cell/row structural styles via
                    // the enclosing table-row / table-cell context.
                    NTxNode content = engine.newDefaultNode(NTxNodeType.TEXT);
                    content.setSource(table.source());
                    content.setProperty(NTxProp.ofString(NTxPropName.VALUE, text));
                    content.setProperty(NTxProp.ofObject(NTxPropName.BACKGROUND_COLOR, NO_BACKGROUND));
                    cell.append(content);
                }
                row.append(cell);
            }
            table.append(row);
            out.add(row);
        }
        return out;
    }

    /**
     * Computes per-row/column weights: a {@code row-weight} may live on the
     * row node itself (set through {@code table-row(row: n)},
     * {@code table-header(row: n)} or {@code table-footer(row: n)} rules) or on
     * any of its cells, and a {@code column-weight} may live on any cell of the
     * column (set through {@code table-column(col: n)},
     * {@code table-cell(row: r, col: c)} or section selectors) — the row
     * weight is the max of the row's own weight and its cells' weights, the
     * column weight the max of its cells' weights (per-cell equivalent of the
     * grid's {@code rows-weight: [1, 1, 3]} list). Rows/columns with no explicit
     * weight default to 1, so every row and column — header, body and footer —
     * participates in the proportional sizing. Global {@code columns-weight} /
     * {@code rows-weight} lists on the table node still override the per-cell
     * values when present (grid convention).
     */
    private TableWeights computeWeights(NTxRendererContext rendererContext, List<NTxNode> rowNodes, int rows, int cols) {
        TableWeights w = new TableWeights();
        w.colWeights = new double[cols];
        w.rowWeights = new double[rows];
        if (rowNodes.isEmpty() || rows == 0 || cols == 0) {
            Arrays.fill(w.colWeights, 1.0);
            Arrays.fill(w.rowWeights, 1.0);
            return w;
        }
        for (int r = 0; r < rows; r++) {
            NTxNode rowNode = r < rowNodes.size() ? rowNodes.get(r) : null;
            if (rowNode == null) {
                continue;
            }
            // the row node itself may carry a row-weight (table-row(row: n) /
            // table-header(row: n) / table-footer(row: n) rules)
            NTxRendererContext rowCtx = rendererContext.resolveNode(rowNode, NTxBounds2D.ofWidth(0, 0, 1, 1));
            double rowNodeWeight = NTxValueByName.getRowWeight(rowCtx);
            if (rowNodeWeight > 0) {
                w.haveRowWeights = true;
                w.rowWeights[r] = Math.max(w.rowWeights[r], rowNodeWeight);
            }
            int c = 0;
            for (NTxNode cell : rowNode.children()) {
                if (c >= cols) {
                    break;
                }
                NTxRendererContext cellCtx = rendererContext.resolveNode(cell, NTxBounds2D.ofWidth(0, 0, 1, 1));
                double rw = NTxValueByName.getRowWeight(cellCtx);
                double cw = NTxValueByName.getColWeight(cellCtx);
                if (rw > 0) {
                    w.haveRowWeights = true;
                    w.rowWeights[r] = Math.max(w.rowWeights[r], rw);
                }
                if (cw > 0) {
                    w.haveColWeights = true;
                    w.colWeights[c] = Math.max(w.colWeights[c], cw);
                }
                c++;
            }
        }
        // Global weight lists on the table node win when present (grid convention)
        double[] globalColWeights = NTxValueByName.getColumnsWeight(rendererContext);
        if (globalColWeights != null && globalColWeights.length > 0) {
            w.haveColWeights = true;
            for (int i = 0; i < cols; i++) {
                double gw = globalColWeights[i % globalColWeights.length];
                w.colWeights[i] = gw > 0 ? gw : 1.0;
            }
        }
        double[] globalRowWeights = NTxValueByName.getRowsWeight(rendererContext);
        if (globalRowWeights != null && globalRowWeights.length > 0) {
            w.haveRowWeights = true;
            for (int i = 0; i < rows; i++) {
                double gw = globalRowWeights[i % globalRowWeights.length];
                w.rowWeights[i] = gw > 0 ? gw : 1.0;
            }
        }
        for (int i = 0; i < cols; i++) {
            if (w.colWeights[i] <= 0) {
                w.colWeights[i] = 1.0;
            }
        }
        for (int i = 0; i < rows; i++) {
            if (w.rowWeights[i] <= 0) {
                w.rowWeights[i] = 1.0;
            }
        }
        return w;
    }

    /** Rows of a table plus the section label ("header"/"body"/"footer") of each row. */
    private static class TableData {
        List<List<String>> data;
        List<String> sections;
    }

    /** Per-cell-derived row/column weights of a table. */
    private static class TableWeights {
        double[] colWeights;
        double[] rowWeights;
        boolean haveColWeights;
        boolean haveRowWeights;
    }

    /** Content-driven geometry of a measured table. */
    private static class TableMeasure {
        double[] colWidths;
        double[] rowHeights;
        double rowHeight;
        double pad;
        double cellFont;
        double tableWidth;
        double tableHeight;
    }
}
