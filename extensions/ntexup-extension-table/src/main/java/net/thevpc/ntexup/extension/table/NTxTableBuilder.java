package net.thevpc.ntexup.extension.table;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxInt2;
import net.thevpc.ntexup.api.document.elem2d.NTxMargin;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Table builder for ntexup.
 * Creates a table node that renders tabular data.
 */
public class NTxTableBuilder implements NTxNodeBuilder {

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
                .renderComponent(this::renderMain)
        ;
    }

    private String asStr(NElement e) {
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

        // Helper to convert NTxValue to List<List<String>>
        java.util.function.Function<NTxValue, List<List<String>>> convertToTableData = value -> {
            List<List<String>> result = new ArrayList<>();
            NOptional<NElement> outerOpt = value.asElement();
            if (outerOpt.isPresent()) {
                NElement outer = outerOpt.get();
                if (outer.isListContainer() && !outer.isNamed()) {
                    NOptional<NListContainerElement> outerListOpt = outer.asListContainer();
                    if (outerListOpt.isPresent()) {
                        NListContainerElement outerList = outerListOpt.get();
                        List<NElement> rows = outerList.children();
                        for (NElement rowElem : rows) {
                            NTxValue rowValue = NTxValue.of(rowElem);
                            NOptional<NElement> rowElemOpt = rowValue.asElement();
                            if (rowElemOpt.isPresent()) {
                                NElement rowElemVal = rowElemOpt.get();
                                if (rowElemVal.isListContainer() && !rowElemVal.isNamed()) {
                                    NOptional<NListContainerElement> rowListOpt = rowElemVal.asListContainer();
                                    if (rowListOpt.isPresent()) {
                                        NListContainerElement rowList = rowListOpt.get();
                                        List<NElement> cells = rowList.children();
                                        List<String> rowData = new ArrayList<>();
                                        for (NElement cellElem : cells) {
                                            NTxValue cellValue = NTxValue.of(cellElem);
                                            rowData.add(asStr(cellValue.asElement().orNull()));
                                        }
                                        result.add(rowData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return result;
        };

        // Build data from arguments: headerData + bodyData + footerData, or data
        List<List<String>> data = new ArrayList<>();

        // Try headerData, bodyData, footerData
        List<List<String>> header = convertToTableData.apply(NTxValue.ofProp(node, "header-data"));
        List<List<String>> body = convertToTableData.apply(NTxValue.ofProp(node, "data"));
        List<List<String>> footer = convertToTableData.apply(NTxValue.ofProp(node, "footer-data"));

        if (!header.isEmpty() || !body.isEmpty() || !footer.isEmpty()) {
            data.addAll(header);
            data.addAll(body);
            data.addAll(footer);
        } else {
            // Try data
            data = convertToTableData.apply(NTxValue.ofProp(node, "data"));
            // If still empty, use a default
            if (data.isEmpty()) {
                data = List.of(
                        List.of("Header1", "Header2"),
                        List.of("Row1Col1", "Row1Col2")
                );
            }
        }

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

        // Get padding
        double pad = 0.0;
        NTxMargin padding = NTxValueByName.getPadding(rendererContext);
        if (padding != null) {
            pad = padding.getLeft(); // use left as representative
        }
        double contentWidth = rendererContext.selfBounds2D().widthX() - 2 * pad;
        double contentHeight = rendererContext.selfBounds2D().widthY() - 2 * pad;

        // Compute column widths
        double[] colWidths = new double[cols];
        NTxValue colWeightVal = NTxValue.ofProp(node, "columns-weight");
        NOptional<NElement> colWeightElemOpt = colWeightVal.asElement();
        if (colWeightElemOpt.isPresent()) {
            NElement colWeightElem = colWeightElemOpt.get();
            if (colWeightElem.isListContainer() && !colWeightElem.isNamed()) {
                NOptional<NListContainerElement> colWeightListOpt = colWeightElem.asListContainer();
                if (colWeightListOpt.isPresent()) {
                    NListContainerElement colWeightList = colWeightListOpt.get();
                    List<NElement> weightElems = colWeightList.children();
                    double totalWeight = 0;
                    List<Double> weightValues = new ArrayList<>();
                    for (NElement wElem : weightElems) {
                        NTxValue wVal = NTxValue.of(wElem);
                        NOptional<Double> wOpt = wVal.asDouble();
                        if (wOpt.isPresent()) {
                            double w = wOpt.get();
                            weightValues.add(w);
                            totalWeight += w;
                        } else {
                            // try to parse as number from string
                            NOptional<String> sOpt = wVal.asStringOrName();
                            if (sOpt.isPresent()) {
                                try {
                                    double w = Double.parseDouble(sOpt.get());
                                    weightValues.add(w);
                                    totalWeight += w;
                                } catch (NumberFormatException e) {
                                    weightValues.add(1.0);
                                    totalWeight += 1.0;
                                }
                            } else {
                                weightValues.add(1.0);
                                totalWeight += 1.0;
                            }
                        }
                    }
                    if (totalWeight > 0) {
                        for (int i = 0; i < cols; i++) {
                            double w = i < weightValues.size() ? weightValues.get(i) : 1.0;
                            colWidths[i] = (w / totalWeight) * contentWidth;
                        }
                    } else {
                        // fallback to equal
                        for (int i = 0; i < cols; i++) {
                            colWidths[i] = contentWidth / cols;
                        }
                    }
                } else {
                    // fallback to equal
                    for (int i = 0; i < cols; i++) {
                        colWidths[i] = contentWidth / cols;
                    }
                }
            } else {
                // fallback to equal
                for (int i = 0; i < cols; i++) {
                    colWidths[i] = contentWidth / cols;
                }
            }
        } else {
            // equal widths
            for (int i = 0; i < cols; i++) {
                colWidths[i] = contentWidth / cols;
            }
        }

        // Compute row heights (equal for now)
        double[] rowHeights = new double[rows];
        Arrays.fill(rowHeights, contentHeight / rows);

        // Draw background
        boolean someBG = rendererContext.applyBackgroundColor();
        NTxGraphics g = rendererContext.graphics();
        if (someBG) {
            // Color already set by applyBackgroundColor
            g.fillRect(rendererContext.selfBounds2D().minX(),
                    rendererContext.selfBounds2D().minY(),
                    rendererContext.selfBounds2D().widthX(),
                    rendererContext.selfBounds2D().widthY());
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

        // Draw text for each cell
        rendererContext.applyFont();
        double textX = rendererContext.selfBounds2D().minX();
        double textY = rendererContext.selfBounds2D().minY();
        for (int row = 0; row < rows; row++) {
            textX = rendererContext.selfBounds2D().minX();
            List<String> rowData = data.get(row);
            for (int col = 0; col < cols; col++) {
                String text = "";
                if (col < rowData.size()) {
                    text = rowData.get(col);
                }
                // Calculate text position (left-aligned with padding)
                double xPos = textX + pad;
                double yPos = textY + rendererContext.getFontSize(); // approximate baseline
                g.drawString(text, xPos, yPos);
                textX += colWidths[col];
            }
            textY += rowHeights[row];
        }

        // Draw contour if enabled
        rendererContext.drawContour();
    }
}
