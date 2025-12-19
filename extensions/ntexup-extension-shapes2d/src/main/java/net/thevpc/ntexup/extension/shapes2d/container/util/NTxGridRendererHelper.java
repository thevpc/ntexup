package net.thevpc.ntexup.extension.shapes2d.container.util;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxInt2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.eval.NTxValue;

import java.awt.*;
import java.util.List;

public class NTxGridRendererHelper {
    boolean drawBackground = true;
    boolean drawGrid = true;
    java.util.List<NTxNode> children;

    public NTxGridRendererHelper(List<NTxNode> children) {
        this.children = children;
    }

    public NTxBounds2 computeBound(NTxNode p, NTxNodeRendererContext ctx, NTxBounds2 expectedBounds) {
        NTxGridRendererHelper.ComputePositionsResult r = computePositions(p, expectedBounds, ctx);
        for (ItemWithPosition<HPagePartExtInfo> eee : r.effPositions.items()) {
            HPagePartExtInfo ee = eee.getUserObject();
            if (ee.colRow != null) {
                expectedBounds = ee.bounds.expand(expectedBounds);
            }
        }
        return expectedBounds;
    }

    public static class ComputePositionsResult {
        public double[] rowWeights;
        public double[] columnWeights;
        public double[] rowWeightsStack;
        public double[] columWeightsStack;
        GridMap<HPagePartExtInfo> effPositions;
        double xOffset;
        double yOffset;
        double childrenWidth;
        double childrenHeight;
    }

    public void render(NTxNodeRendererContext ctx, NTxBounds2 expectedBounds) {
        if (ctx.isDry()) {
            return;
        }
        NTxGraphics g = ctx.graphics();
        NTxNode p = ctx.node();
        if (drawBackground) {
            if (ctx.applyBackgroundColor(p)) {
                g.fillRect(expectedBounds);
            }
        }

        NTxGridRendererHelper.ComputePositionsResult r = computePositions(p, expectedBounds, ctx);
        for (ItemWithPosition<HPagePartExtInfo> eee : r.effPositions.items()) {
            HPagePartExtInfo ee = eee.getUserObject();
            NTxNodeRendererContext ctx3 = ctx.withParentBounds(ee.bounds);
            if (!ctx.isDry()) {
                if (ctx.isDebug(p)) {
                    g.setColor(ctx.getDebugColor(p));
                    g.setFont(new Font("Verdana", Font.PLAIN, 8));
                    g.drawString(String.valueOf(ee.index), ee.bounds.getCenterX(), ee.bounds.getCenterY());
                }
            }
            ctx3.withNode(ee.node).render();
        }
        drawGrid(p, r, ctx);
        ctx.paintBorderLine(p, expectedBounds);
    }


    private ComputePositionsResult computePositions(NTxNode node, NTxBounds2 expectedBounds, NTxNodeRendererContext ctx) {
        int cols = ctx.getColumns(node);
        int rows = ctx.getRows(node);
        if (cols < 0) {
            cols = -1;
        }
        if (rows < 0) {
            rows = -1;
        }
        if (cols <= 0 && rows <= 0) {
            int cc = (int) Math.floor(Math.sqrt(children.size()));
            cols = cc;
            rows = cc;
        } else if (cols < 0 && rows > 0) {
            cols = (int) Math.floor(children.size() / rows);
        } else if (cols > 0 && rows < 0) {
            rows = (int) Math.floor(children.size() / cols);
        }
        if (cols <= 0 && rows <= 0) {
            cols=1;
            rows=-1;
        }

        GridMap<HPagePartExtInfo> effPositions = new GridMap<>(cols, rows);
        for (int i = 0; i < children.size(); i++) {
            NTxNode cc = children.get(i);
            HPagePartExtInfo e = new HPagePartExtInfo();
            e.node = cc;
            NTxNodeRendererContext chctx = ctx.withChild(cc, ctx.defaultSelfBounds());
            e.colspan = NTxValueByName.getColSpan(cc, chctx);
            e.rowspan = NTxValueByName.getRowSpan(cc, chctx);
            e.colweight = NTxValueByName.getColWeight(cc, chctx);
            e.rowweight = NTxValueByName.getRowWeight(cc, chctx);
            e.index = i;
            effPositions.add(e.colspan, e.rowspan, e);
        }
        rows=effPositions.rows();
        cols=effPositions.columns();
        double[] rowWeights = new double[rows];
        double[] columnWeights = new double[cols];
        double[] rowWeightsStack = new double[rows];
        double[] columWeightsStack = new double[cols];
        double minRowWeights = 0;
        double minColWeights = 0;
        for (ItemWithPosition<HPagePartExtInfo> item : effPositions.items()) {
            int x = item.getX();
            int y = item.getY();
            int w = item.getWidth();
            int h = item.getHeight();
            double factor = 1.0 / w / h;
            HPagePartExtInfo uo = item.getUserObject();
            double nw = uo.colweight;
            if (minColWeights <= 0) {
                minColWeights = nw;
            } else {
                minColWeights = Math.min(minColWeights, nw);
            }
            for (int xi = 0; xi < w; xi++) {
                columnWeights[xi + x] = Math.max(columnWeights[xi + x], nw * factor / uo.colspan);
            }
            nw = uo.rowweight;
            if (minRowWeights <= 0) {
                minRowWeights = nw;
            } else {
                minRowWeights = Math.min(minRowWeights, nw);
            }
            for (int yi = 0; yi < h; yi++) {
                rowWeights[yi + y] = Math.max(rowWeights[yi + y], nw * factor / uo.rowspan);
            }
        }
        if (minRowWeights <= 0) {
            minRowWeights = 1;
        }
        if (minColWeights <= 0) {
            minColWeights = 1;
        }
        double sumColumnWeights = 0;
        for (int i = 0, columnWeightsLength = columnWeights.length; i < columnWeightsLength; i++) {
            if (columnWeights[i] <= 0) {
                columnWeights[i] = 1;
            }
            sumColumnWeights += columnWeights[i];
        }

        double sumRowWeights = 0;
        for (int i = 0, rowWeightsLength = rowWeights.length; i < rowWeightsLength; i++) {
            if (rowWeights[i] <= 0) {
                rowWeights[i] = 1;
            }
            sumRowWeights += rowWeights[i];
        }

        for (int i = 0, columnWeightsLength = columnWeights.length; i < columnWeightsLength; i++) {
            columnWeights[i] = columnWeights[i] * 100 / sumColumnWeights;
            if (i == 0) {
                columWeightsStack[i] = 0;
            } else {
                columWeightsStack[i] = columWeightsStack[i - 1] + columnWeights[i - 1];
            }
        }

        for (int i = 0, rowWeightsLength = rowWeights.length; i < rowWeightsLength; i++) {
            rowWeights[i] = rowWeights[i] * 100 / sumRowWeights;
            if (i == 0) {
                rowWeightsStack[i] = 0;
            } else {
                rowWeightsStack[i] = rowWeightsStack[i - 1] + rowWeights[i - 1];
            }
        }


        rows = effPositions.rows();
        cols = effPositions.columns();

        double childrenWidth = expectedBounds.getWidth();
        double childrenHeight = expectedBounds.getHeight();
        double xOffset = expectedBounds.getX();
        double yOffset = expectedBounds.getY();

        for (ItemWithPosition<HPagePartExtInfo> eee : effPositions.items()) {
            HPagePartExtInfo ee = eee.getUserObject();
            int ix = eee.getX();
            int iy = eee.getY();
            ee.colRow = new NTxInt2(ix, iy);
            ee.width = 0;
            for (int i = 0; i < ee.colspan; i++) {
                ee.width += columnWeights[ix + i];
            }
            ee.height = 0;
            for (int i = 0; i < ee.rowspan; i++) {
                ee.height += rowWeights[iy + i];
            }

            double jx = columWeightsStack[ix] / 100.0 * childrenWidth + xOffset;
            double jy = rowWeightsStack[iy] / 100.0 * childrenHeight + yOffset;
            ee.bounds = new NTxBounds2(jx, jy, ee.width / 100.0 * childrenWidth, ee.height / 100.0 * childrenHeight);
            ee.sizeRequirements = ctx.engine().getRenderer(ee.node.type()).get().sizeRequirements(ctx.withChild(ee.node, ee.bounds));
        }

        //re-evaluate paintable zone
        NTxDouble2 posAnchor = NTxValue.of(ctx.computePropertyValue(node, NTxPropName.ORIGIN).orNull()).asDouble2OrHAlign().orElse(null);

        if (posAnchor != null) {
            double[] preferredRowsWeight = new double[effPositions.count()];
            double minPref = 0;
            double maxPref = 0;
            for (ItemWithPosition<HPagePartExtInfo> eee : effPositions.items()) {
                HPagePartExtInfo ee = eee.getUserObject();
                double newY = ee.sizeRequirements.minY;
                if (newY > 0) {
                    preferredRowsWeight[ee.colRow.getY()] = newY;
                    if (minPref == 0 || minPref > newY) {
                        minPref = newY;
                    }
                    if (maxPref == 0 || maxPref < newY) {
                        maxPref = newY;
                    }
                }
            }
            if (minPref > 0) {
                double sumY = 0;
                for (ItemWithPosition<HPagePartExtInfo> eee : effPositions.items()) {
                    HPagePartExtInfo ee = eee.getUserObject();
                    int y = ee.colRow.getY();
                    if (preferredRowsWeight[y] == 0) {
                        preferredRowsWeight[y] = minPref;
                    }
                    sumY += preferredRowsWeight[y];
                }

                double extraOffset = childrenHeight - sumY;
                childrenHeight = sumY;
                xOffset += posAnchor.getX() / 100.0 * extraOffset;
                yOffset += posAnchor.getY() / 100.0 * extraOffset;
//                switch (posAnchor) {
//                    case CENTER:
//                    case LEFT:
//                    case RIGHT: {
//                        yOffset += extraOffset / 2;
//                        break;
//                    }
//                    case BOTTOM:
//                    case BOTTOM_LEFT:
//                    case BOTTOM_RIGHT: {
//                        yOffset += extraOffset;
//                        break;
//                    }
//                    case TOP:
//                    case TOP_LEFT:
//                    case TOP_RIGHT: {
//                        //do nothing
//                        break;
//                    }
//                }
            }
        }
        ComputePositionsResult r = new ComputePositionsResult();
        r.effPositions = effPositions;
        r.xOffset = xOffset;
        r.yOffset = yOffset;
        r.childrenWidth = childrenWidth;
        r.childrenHeight = childrenHeight;
        r.rowWeights = rowWeights;
        r.columnWeights = columnWeights;
        r.rowWeightsStack = rowWeightsStack;
        r.columWeightsStack = columWeightsStack;

        for (ItemWithPosition<HPagePartExtInfo> eee : r.effPositions.items()) {
            int ix = eee.getX();
            int iy = eee.getY();
            HPagePartExtInfo ee = eee.getUserObject();
            double ww = r.childrenWidth * (ee.width / 100);
            double hh = r.childrenHeight * (ee.height / 100);
            double jx = columWeightsStack[ix] / 100.0 * r.childrenWidth + r.xOffset;
            double jy = rowWeightsStack[iy] / 100.0 * r.childrenHeight + r.yOffset;
            ee.bounds = new NTxBounds2(jx, jy, ww, hh);
        }

        return r;
    }


    public static class HPagePartExtInfo {
        NTxInt2 colRow;
        double width;
        double height;
        NTxNode node;
        NTxSizeRequirements sizeRequirements;
        int colspan;
        int rowspan;
        double colweight;
        double rowweight;
        int index;
        NTxNodeRendererContext ctx;

        NTxBounds2 bounds;
    }

    public static class WeightInfo {
        double[] colsWeight;
        double[] colStart;
        double[] rowsWeight;
        double[] rowStart;

        public double width(int colIndex, int colspan) {
            double width = 0;
            for (int i = 0; i < colspan; i++) {
                width += colsWeight[colIndex + i];
            }
            return width;
        }

        public double height(int rowIndex, int rowspan) {
            double height = 0;
            for (int i = 0; i < rowspan; i++) {
                height += rowsWeight[rowIndex + i];
            }
            return height;
        }
    }

    private static double[] revalidateWeights(int cols, double[] colsWeight) {
        double[] colsWeight2 = new double[cols];

        double cwSum = 0;
        int validColumns = 0;
        for (int i = 0; i < colsWeight2.length; i++) {
            int ii = colsWeight.length == 0 ? 0 : i % colsWeight.length;
            if (ii < colsWeight.length) {
                if (colsWeight[ii] > 0 && Double.isFinite(colsWeight[ii])) {
                    colsWeight2[i] = colsWeight[ii];
                    cwSum += colsWeight2[i];
                    validColumns++;
                }
            }
        }
        if (validColumns < colsWeight2.length) {
            double r = validColumns == 0 ? (100 / colsWeight2.length) : cwSum / validColumns;
            cwSum = 0;
            for (int i = 0; i < colsWeight2.length; i++) {
                if (colsWeight2[i] == 0) {
                    colsWeight2[i] = r;
                }
                cwSum += colsWeight2[i];
            }
        }
        if (cwSum > 0) {
            for (int i = 0; i < colsWeight2.length; i++) {
                colsWeight2[i] = colsWeight2[i] * 100 / cwSum;
            }
        }
        return colsWeight2;
    }


    private void drawGrid(NTxNode p, NTxGridRendererHelper.ComputePositionsResult r, NTxNodeRendererContext ctx) {
        if (ctx.isDry()) {
            return;
        }
        NTxGraphics g = ctx.graphics();
        if (ctx.requireDrawGrid(p)) {
            if (ctx.applyGridColor(p, true)) {
                for (int i = 0; i < r.columnWeights.length; i++) {
                    g.drawLine(
                            (int) (r.columWeightsStack[i] / 100 * r.childrenWidth + r.xOffset),
                            (int) r.yOffset,
                            (int) (r.columWeightsStack[i] / 100 * r.childrenWidth + r.xOffset),
                            (int) (r.yOffset + r.childrenHeight)
                    );
                }
                g.drawLine(
                        (int) (r.childrenWidth + r.xOffset),
                        (int) r.yOffset,
                        (int) (r.childrenWidth + r.xOffset),
                        (int) (r.yOffset + r.childrenHeight)
                );
                for (int i = 0; i < r.rowWeights.length; i++) {
                    g.drawLine(
                            (int) r.xOffset,
                            (int) (r.rowWeightsStack[i] / 100 * r.childrenHeight + r.yOffset),
                            (int) (r.xOffset + r.childrenWidth),
                            (int) (r.rowWeightsStack[i] / 100 * r.childrenHeight + r.yOffset)
                    );
                }
                g.drawLine(
                        (int) r.xOffset,
                        (int) (r.childrenHeight + r.yOffset),
                        (int) (r.xOffset + r.childrenWidth),
                        (int) (r.childrenHeight + r.yOffset)
                );
            }
        }
    }


    private double[] revalidateWeightStart(double[] colsWeight) {
        double[] colsWeight2 = new double[colsWeight.length];
        for (int i = 0; i < colsWeight.length; i++) {
            if (i == 0) {
                colsWeight2[i] = 0;
            } else {
                colsWeight2[i] = colsWeight[i - 1] + colsWeight2[i - 1];
            }
        }
        return colsWeight2;
    }


}
