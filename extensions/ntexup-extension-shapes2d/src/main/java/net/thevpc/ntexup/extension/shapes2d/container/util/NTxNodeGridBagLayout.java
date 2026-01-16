package net.thevpc.ntexup.extension.shapes2d.container.util;

/*
 * Inspired from Java's GridBagLayout!!
 *
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxSizeD;
import net.thevpc.ntexup.api.document.elem2d.primitives.NTxEditableBounds2;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.Collectors;


public class NTxNodeGridBagLayout {

    static final int EMPIRICMULTIPLIER = 2;
    private static final int MINSIZE = 1;
    private static final int PREFERREDSIZE = 2;

    private java.util.List<NTxNodeGridNodeExt> children;

    private NTxNodeGridBagLayoutInfo layoutInfo;
    private int columnWidths[];
    private int rowHeights[];
    private double columnWeights[];
    private double rowWeights[];
    private NTxNodeGridNodeExt componentAdjusting;
    private Insets parentInsets;
    private NTxBounds2D parentBounds;
    boolean rightToLeft = false;

    public NTxNodeGridBagLayout(Insets parentInsets, boolean rightToLeft, NTxBounds2D parentBounds, NTxNodeGridNode[] children) {
        this.parentInsets = parentInsets;
        this.children = Arrays.asList(children).stream().map(x -> new NTxNodeGridNodeExt(x)).collect(Collectors.toList());
        this.rightToLeft = rightToLeft;
        this.parentBounds = parentBounds;
    }

    public NTxNodeGridNode[] children() {
        return children.stream().map(x->x.base).toArray(NTxNodeGridNode[]::new);
    }

    public NTxDouble2 getLayoutOrigin() {
        double x = 0;
        double y = 0;
        if (layoutInfo != null) {
            x = layoutInfo.startx;
            y = layoutInfo.starty;
        }
        return new NTxDouble2(x, y);
    }

    public int[][] getLayoutDimensions() {
        if (layoutInfo == null)
            return new int[2][0];

        int dim[][] = new int[2][];
        dim[0] = new int[layoutInfo.width];
        dim[1] = new int[layoutInfo.height];

        System.arraycopy(layoutInfo.minWidth, 0, dim[0], 0, layoutInfo.width);
        System.arraycopy(layoutInfo.minHeight, 0, dim[1], 0, layoutInfo.height);

        return dim;
    }

    public double[][] getLayoutWeights() {
        if (layoutInfo == null)
            return new double[2][0];

        double weights[][] = new double[2][];
        weights[0] = new double[layoutInfo.width];
        weights[1] = new double[layoutInfo.height];

        System.arraycopy(layoutInfo.weightX, 0, weights[0], 0, layoutInfo.width);
        System.arraycopy(layoutInfo.weightY, 0, weights[1], 0, layoutInfo.height);

        return weights;
    }

    public Point location(int x, int y) {
        Point loc = new Point(0, 0);
        int i;
        double d;

        if (layoutInfo == null)
            return loc;

        d = layoutInfo.startx;
        if (!rightToLeft) {
            for (i = 0; i < layoutInfo.width; i++) {
                d += layoutInfo.minWidth[i];
                if (d > x)
                    break;
            }
        } else {
            for (i = layoutInfo.width - 1; i >= 0; i--) {
                if (d > x)
                    break;
                d += layoutInfo.minWidth[i];
            }
            i++;
        }
        loc.x = i;

        d = layoutInfo.starty;
        for (i = 0; i < layoutInfo.height; i++) {
            d += layoutInfo.minHeight[i];
            if (d > y)
                break;
        }
        loc.y = i;

        return loc;
    }

    private long[] preInitMaximumArraySizes() {
        NTxNodeGridNode[] components = this.children.toArray(new NTxNodeGridNode[0]);
        NTxNodeGridNode comp;
        NTxNodeGridNode constraints;
        int curX, curY;
        int curWidth, curHeight;
        int preMaximumArrayXIndex = 0;
        int preMaximumArrayYIndex = 0;
        long[] returnArray = new long[2];

        for (int compId = 0; compId < components.length; compId++) {
            comp = components[compId];
            if (!comp.visible) {
                continue;
            }

            constraints = comp;
            curX = constraints.gridx;
            curY = constraints.gridy;
            curWidth = constraints.gridwidth;
            curHeight = constraints.gridheight;

            // -1==RELATIVE, means that column|row equals to previously added component,
            // since each next Component with gridx|gridy == RELATIVE starts from
            // previous position, so we should start from previous component which
            // already used in maximumArray[X|Y]Index calculation. We could just increase
            // maximum by 1 to handle situation when component with gridx=-1 was added.
            if (curX < 0) {
                curX = ++preMaximumArrayYIndex;
            }
            if (curY < 0) {
                curY = ++preMaximumArrayXIndex;
            }
            // gridwidth|gridheight may be equal to RELATIVE (-1) or REMAINDER (0)
            // in any case using 1 instead of 0 or -1 should be sufficient to for
            // correct maximumArraySizes calculation
            if (curWidth <= 0) {
                curWidth = 1;
            }
            if (curHeight <= 0) {
                curHeight = 1;
            }

            preMaximumArrayXIndex = Math.max(curY + curHeight, preMaximumArrayXIndex);
            preMaximumArrayYIndex = Math.max(curX + curWidth, preMaximumArrayYIndex);
        } //for (components) loop
        // Must specify index++ to allocate well-working arrays.
        /* fix for 4623196.
         * now return long array instead of Point
         */
        returnArray[0] = preMaximumArrayXIndex;
        returnArray[1] = preMaximumArrayYIndex;
        return returnArray;
    } //PreInitMaximumSizes

    private NTxNodeGridBagLayoutInfo getLayoutInfo(int sizeflag) {
        NTxNodeGridBagLayoutInfo r;
//        NTxNodeGridNodeExt comp;
        NTxNodeGridNodeExt constraints;
        NTxSizeD d;
        NTxNodeGridNodeExt[] components = children.toArray(new NTxNodeGridNodeExt[0]);
        // Code below will address index curX+curWidth in the case of yMaxArray, weightY
        // ( respectively curY+curHeight for xMaxArray, weightX ) where
        //  curX in 0 to preInitMaximumArraySizes.y
        // Thus, the maximum index that could
        // be calculated in the following code is curX+curX.
        // EmpericMultier equals 2 because of this.

        int layoutWidth, layoutHeight;
        int[] xMaxArray;
        int[] yMaxArray;
        int compindex, i, k, px, py, nextSize;
        double pixels_diff;
        int curX = 0; // constraints.gridx
        int curY = 0; // constraints.gridy
        int curWidth = 1;  // constraints.gridwidth
        int curHeight = 1;  // constraints.gridheight
        int curRow, curCol;
        double weight_diff, weight;
        int maximumArrayXIndex = 0;
        int maximumArrayYIndex = 0;
        Anchor anchor = Anchor.NONE;

        /*
         * Pass #1
         *
         * Figure out the dimensions of the layout grid (use a value of 1 for
         * zero or negative widths and heights).
         */

        layoutWidth = layoutHeight = 0;
        curRow = curCol = -1;
        long[] arraySizes = preInitMaximumArraySizes();

        /* fix for 4623196.
         * If user try to create a very big grid we can
         * get NegativeArraySizeException because of integer value
         * overflow (EMPIRICMULTIPLIER*gridSize might be more then Integer.MAX_VALUE).
         * We need to detect this situation and try to create a
         * grid with Integer.MAX_VALUE size instead.
         */
        maximumArrayXIndex = (EMPIRICMULTIPLIER * arraySizes[0] > Integer.MAX_VALUE) ? Integer.MAX_VALUE : EMPIRICMULTIPLIER * (int) arraySizes[0];
        maximumArrayYIndex = (EMPIRICMULTIPLIER * arraySizes[1] > Integer.MAX_VALUE) ? Integer.MAX_VALUE : EMPIRICMULTIPLIER * (int) arraySizes[1];

        if (rowHeights != null) {
            maximumArrayXIndex = Math.max(maximumArrayXIndex, rowHeights.length);
        }
        if (columnWidths != null) {
            maximumArrayYIndex = Math.max(maximumArrayYIndex, columnWidths.length);
        }

        xMaxArray = new int[maximumArrayXIndex];
        yMaxArray = new int[maximumArrayYIndex];

        boolean hasBaseline = false;
        for (compindex = 0; compindex < components.length; compindex++) {
            constraints = components[compindex];
            if (!constraints.visible)
                continue;
            curX = constraints.gridx;
            curY = constraints.gridy;
            curWidth = constraints.gridwidth;
            if (curWidth <= 0)
                curWidth = 1;
            curHeight = constraints.gridheight;
            if (curHeight <= 0)
                curHeight = 1;

            /* If x or y is negative, then use relative positioning: */
            if (curX < 0 && curY < 0) {
                if (curRow >= 0)
                    curY = curRow;
                else if (curCol >= 0)
                    curX = curCol;
                else
                    curY = 0;
            }
            if (curX < 0) {
                px = 0;
                for (i = curY; i < (curY + curHeight); i++) {
                    px = Math.max(px, xMaxArray[i]);
                }

                curX = px - curX - 1;
                if (curX < 0)
                    curX = 0;
            } else if (curY < 0) {
                py = 0;
                for (i = curX; i < (curX + curWidth); i++) {
                    py = Math.max(py, yMaxArray[i]);
                }
                curY = py - curY - 1;
                if (curY < 0)
                    curY = 0;
            }

            /* Adjust the grid width and height
             *  fix for 5005945: unneccessary loops removed
             */
            px = curX + curWidth;
            if (layoutWidth < px) {
                layoutWidth = px;
            }
            py = curY + curHeight;
            if (layoutHeight < py) {
                layoutHeight = py;
            }

            /* Adjust xMaxArray and yMaxArray */
            for (i = curX; i < (curX + curWidth); i++) {
                yMaxArray[i] = py;
            }
            for (i = curY; i < (curY + curHeight); i++) {
                xMaxArray[i] = px;
            }


            /* Cache the current slave's size. */
            if (sizeflag == PREFERREDSIZE)
                d = constraints.preferredSize;
            else
                d = constraints.minimumSize;
            if (d == null) {
                d = new NTxSizeD(0, 0);
            }
            constraints.minWidth = d.getWidth();
            constraints.minHeight = d.getHeight();
            if (calculateBaseline(constraints, d)) {
                hasBaseline = true;
            }

            /* Zero width and height must mean that this is the last item (or
             * else something is wrong). */
            if (constraints.gridheight == 0 && constraints.gridwidth == 0)
                curRow = curCol = -1;

            /* Zero width starts a new row */
            if (constraints.gridheight == 0 && curRow < 0)
                curCol = curX + curWidth;

                /* Zero height starts a new column */
            else if (constraints.gridwidth == 0 && curCol < 0)
                curRow = curY + curHeight;
        } //for (components) loop


        /*
         * Apply minimum row/column dimensions
         */
        if (columnWidths != null && layoutWidth < columnWidths.length)
            layoutWidth = columnWidths.length;
        if (rowHeights != null && layoutHeight < rowHeights.length)
            layoutHeight = rowHeights.length;

        r = new NTxNodeGridBagLayoutInfo(layoutWidth, layoutHeight);

        /*
         * Pass #2
         *
         * Negative values for gridX are filled in with the current x value.
         * Negative values for gridY are filled in with the current y value.
         * Negative or zero values for gridWidth and gridHeight end the current
         *  row or column, respectively.
         */

        curRow = curCol = -1;

        Arrays.fill(xMaxArray, 0);
        Arrays.fill(yMaxArray, 0);

        double[] maxAscent = null;
        double[] maxDescent = null;
        short[] baselineType = null;

        if (hasBaseline) {
            r.maxAscent = maxAscent = new double[layoutHeight];
            r.maxDescent = maxDescent = new double[layoutHeight];
            r.baselineType = baselineType = new short[layoutHeight];
            r.hasBaseline = true;
        }


        for (compindex = 0; compindex < components.length; compindex++) {
            constraints = components[compindex];
            if (!constraints.visible)
                continue;
            curX = constraints.gridx;
            curY = constraints.gridy;
            curWidth = constraints.gridwidth;
            curHeight = constraints.gridheight;

            /* If x or y is negative, then use relative positioning: */
            if (curX < 0 && curY < 0) {
                if (curRow >= 0)
                    curY = curRow;
                else if (curCol >= 0)
                    curX = curCol;
                else
                    curY = 0;
            }

            if (curX < 0) {
                if (curHeight <= 0) {
                    curHeight += r.height - curY;
                    if (curHeight < 1)
                        curHeight = 1;
                }

                px = 0;
                for (i = curY; i < (curY + curHeight); i++)
                    px = Math.max(px, xMaxArray[i]);

                curX = px - curX - 1;
                if (curX < 0)
                    curX = 0;
            } else if (curY < 0) {
                if (curWidth <= 0) {
                    curWidth += r.width - curX;
                    if (curWidth < 1)
                        curWidth = 1;
                }

                py = 0;
                for (i = curX; i < (curX + curWidth); i++) {
                    py = Math.max(py, yMaxArray[i]);
                }

                curY = py - curY - 1;
                if (curY < 0)
                    curY = 0;
            }

            if (curWidth <= 0) {
                curWidth += r.width - curX;
                if (curWidth < 1)
                    curWidth = 1;
            }

            if (curHeight <= 0) {
                curHeight += r.height - curY;
                if (curHeight < 1)
                    curHeight = 1;
            }

            px = curX + curWidth;
            py = curY + curHeight;

            for (i = curX; i < (curX + curWidth); i++) {
                yMaxArray[i] = py;
            }
            for (i = curY; i < (curY + curHeight); i++) {
                xMaxArray[i] = px;
            }

            /* Make negative sizes start a new row/column */
            if (constraints.gridheight == 0 && constraints.gridwidth == 0)
                curRow = curCol = -1;
            if (constraints.gridheight == 0 && curRow < 0)
                curCol = curX + curWidth;
            else if (constraints.gridwidth == 0 && curCol < 0)
                curRow = curY + curHeight;

            /* Assign the new values to the gridbag slave */
            constraints.tempX = curX;
            constraints.tempY = curY;
            constraints.tempWidth = curWidth;
            constraints.tempHeight = curHeight;

            anchor = constraints.anchor;
            if (hasBaseline) {
                switch (anchor) {
                    case BASELINE:
                    case BASELINE_LEADING:
                    case BASELINE_TRAILING:
                        if (constraints.ascent >= 0) {
                            if (curHeight == 1) {
                                maxAscent[curY] =
                                        Math.max(maxAscent[curY],
                                                constraints.ascent);
                                maxDescent[curY] =
                                        Math.max(maxDescent[curY],
                                                constraints.descent);
                            } else {
                                if (constraints.baselineResizeBehavior ==
                                        Component.BaselineResizeBehavior.
                                                CONSTANT_DESCENT) {
                                    maxDescent[curY + curHeight - 1] =
                                            Math.max(maxDescent[curY + curHeight
                                                            - 1],
                                                    constraints.descent);
                                } else {
                                    maxAscent[curY] = Math.max(maxAscent[curY],
                                            constraints.ascent);
                                }
                            }
                            if (constraints.baselineResizeBehavior ==
                                    Component.BaselineResizeBehavior.CONSTANT_DESCENT) {
                                baselineType[curY + curHeight - 1] |=
                                        (1 << constraints.
                                                baselineResizeBehavior.ordinal());
                            } else {
                                baselineType[curY] |= (1 << constraints.
                                        baselineResizeBehavior.ordinal());
                            }
                        }
                        break;
                    case ABOVE_BASELINE:
                    case ABOVE_BASELINE_LEADING:
                    case ABOVE_BASELINE_TRAILING:
                        // Component positioned above the baseline.
                        // To make the bottom edge of the component aligned
                        // with the baseline the bottom inset is
                        // added to the descent, the rest to the ascent.
                        pixels_diff = constraints.minHeight +
                                constraints.insets.top +
                                constraints.ipady;
                        maxAscent[curY] = Math.max(maxAscent[curY],
                                pixels_diff);
                        maxDescent[curY] = Math.max(maxDescent[curY],
                                constraints.insets.bottom);
                        break;
                    case BELOW_BASELINE:
                    case BELOW_BASELINE_LEADING:
                    case BELOW_BASELINE_TRAILING:
                        // Component positioned below the baseline.
                        // To make the top edge of the component aligned
                        // with the baseline the top inset is
                        // added to the ascent, the rest to the descent.
                        pixels_diff = constraints.minHeight +
                                constraints.insets.bottom + constraints.ipady;
                        maxDescent[curY] = Math.max(maxDescent[curY],
                                pixels_diff);
                        maxAscent[curY] = Math.max(maxAscent[curY],
                                constraints.insets.top);
                        break;
                }
            }
        }

        r.weightX = new double[maximumArrayYIndex];
        r.weightY = new double[maximumArrayXIndex];
        r.minWidth = new int[maximumArrayYIndex];
        r.minHeight = new int[maximumArrayXIndex];


        /*
         * Apply minimum row/column dimensions and weights
         */
        if (columnWidths != null)
            System.arraycopy(columnWidths, 0, r.minWidth, 0, columnWidths.length);
        if (rowHeights != null)
            System.arraycopy(rowHeights, 0, r.minHeight, 0, rowHeights.length);
        if (columnWeights != null)
            System.arraycopy(columnWeights, 0, r.weightX, 0, Math.min(r.weightX.length, columnWeights.length));
        if (rowWeights != null)
            System.arraycopy(rowWeights, 0, r.weightY, 0, Math.min(r.weightY.length, rowWeights.length));

        /*
         * Pass #3
         *
         * Distribute the minimun widths and weights:
         */

        nextSize = Integer.MAX_VALUE;

        for (i = 1;
             i != Integer.MAX_VALUE;
             i = nextSize, nextSize = Integer.MAX_VALUE) {
            for (compindex = 0; compindex < components.length; compindex++) {
                constraints = components[compindex];
                if (!constraints.visible)
                    continue;

                if (constraints.tempWidth == i) {
                    px = constraints.tempX + constraints.tempWidth; /* right column */

                    /*
                     * Figure out if we should use this slave\'s weight.  If the weight
                     * is less than the total weight spanned by the width of the cell,
                     * then discard the weight.  Otherwise split the difference
                     * according to the existing weights.
                     */

                    weight_diff = constraints.weightx;
                    for (k = constraints.tempX; k < px; k++)
                        weight_diff -= r.weightX[k];
                    if (weight_diff > 0.0) {
                        weight = 0.0;
                        for (k = constraints.tempX; k < px; k++)
                            weight += r.weightX[k];
                        for (k = constraints.tempX; weight > 0.0 && k < px; k++) {
                            double wt = r.weightX[k];
                            double dx = (wt * weight_diff) / weight;
                            r.weightX[k] += dx;
                            weight_diff -= dx;
                            weight -= wt;
                        }
                        /* Assign the remainder to the rightmost cell */
                        r.weightX[px - 1] += weight_diff;
                    }

                    /*
                     * Calculate the minWidth array values.
                     * First, figure out how wide the current slave needs to be.
                     * Then, see if it will fit within the current minWidth values.
                     * If it will not fit, add the difference according to the
                     * weightX array.
                     */

                    pixels_diff =
                            constraints.minWidth + constraints.ipadx +
                                    constraints.insets.left + constraints.insets.right;

                    for (k = constraints.tempX; k < px; k++)
                        pixels_diff -= r.minWidth[k];
                    if (pixels_diff > 0) {
                        weight = 0.0;
                        for (k = constraints.tempX; k < px; k++)
                            weight += r.weightX[k];
                        for (k = constraints.tempX; weight > 0.0 && k < px; k++) {
                            double wt = r.weightX[k];
                            double dx =  ((wt * ((double) pixels_diff)) / weight);
                            r.minWidth[k] += dx;
                            pixels_diff -= dx;
                            weight -= wt;
                        }
                        /* Any leftovers go into the rightmost cell */
                        r.minWidth[px - 1] += pixels_diff;
                    }
                } else if (constraints.tempWidth > i && constraints.tempWidth < nextSize)
                    nextSize = constraints.tempWidth;


                if (constraints.tempHeight == i) {
                    py = constraints.tempY + constraints.tempHeight; /* bottom row */

                    /*
                     * Figure out if we should use this slave's weight.  If the weight
                     * is less than the total weight spanned by the height of the cell,
                     * then discard the weight.  Otherwise split it the difference
                     * according to the existing weights.
                     */

                    weight_diff = constraints.weighty;
                    for (k = constraints.tempY; k < py; k++)
                        weight_diff -= r.weightY[k];
                    if (weight_diff > 0.0) {
                        weight = 0.0;
                        for (k = constraints.tempY; k < py; k++)
                            weight += r.weightY[k];
                        for (k = constraints.tempY; weight > 0.0 && k < py; k++) {
                            double wt = r.weightY[k];
                            double dy = (wt * weight_diff) / weight;
                            r.weightY[k] += dy;
                            weight_diff -= dy;
                            weight -= wt;
                        }
                        /* Assign the remainder to the bottom cell */
                        r.weightY[py - 1] += weight_diff;
                    }

                    /*
                     * Calculate the minHeight array values.
                     * First, figure out how tall the current slave needs to be.
                     * Then, see if it will fit within the current minHeight values.
                     * If it will not fit, add the difference according to the
                     * weightY array.
                     */

                    pixels_diff = -1;
                    if (hasBaseline) {
                        switch (constraints.anchor) {
                            case BASELINE:
                            case BASELINE_LEADING:
                            case BASELINE_TRAILING:
                                if (constraints.ascent >= 0) {
                                    if (constraints.tempHeight == 1) {
                                        pixels_diff =
                                                maxAscent[constraints.tempY] +
                                                        maxDescent[constraints.tempY];
                                    } else if (constraints.baselineResizeBehavior !=
                                            Component.BaselineResizeBehavior.
                                                    CONSTANT_DESCENT) {
                                        pixels_diff =
                                                maxAscent[constraints.tempY] +
                                                        constraints.descent;
                                    } else {
                                        pixels_diff = constraints.ascent +
                                                maxDescent[constraints.tempY +
                                                        constraints.tempHeight - 1];
                                    }
                                }
                                break;
                            case ABOVE_BASELINE:
                            case ABOVE_BASELINE_LEADING:
                            case ABOVE_BASELINE_TRAILING:
                                pixels_diff = constraints.insets.top +
                                        constraints.minHeight +
                                        constraints.ipady +
                                        maxDescent[constraints.tempY];
                                break;
                            case BELOW_BASELINE:
                            case BELOW_BASELINE_LEADING:
                            case BELOW_BASELINE_TRAILING:
                                pixels_diff = maxAscent[constraints.tempY] +
                                        constraints.minHeight +
                                        constraints.insets.bottom +
                                        constraints.ipady;
                                break;
                        }
                    }
                    if (pixels_diff == -1) {
                        pixels_diff =
                                constraints.minHeight + constraints.ipady +
                                        constraints.insets.top +
                                        constraints.insets.bottom;
                    }
                    for (k = constraints.tempY; k < py; k++)
                        pixels_diff -= r.minHeight[k];
                    if (pixels_diff > 0) {
                        weight = 0.0;
                        for (k = constraints.tempY; k < py; k++)
                            weight += r.weightY[k];
                        for (k = constraints.tempY; weight > 0.0 && k < py; k++) {
                            double wt = r.weightY[k];
                            double dy =  ((wt * ((double) pixels_diff)) / weight);
                            r.minHeight[k] += dy;
                            pixels_diff -= dy;
                            weight -= wt;
                        }
                        /* Any leftovers go into the bottom cell */
                        r.minHeight[py - 1] += pixels_diff;
                    }
                } else if (constraints.tempHeight > i &&
                        constraints.tempHeight < nextSize)
                    nextSize = constraints.tempHeight;
            }
        }
        return r;

    } //getLayoutInfo()

    /**
     * Calculate the baseline for the specified component.
     * If {@code c} is positioned along it's baseline, the baseline is
     * obtained and the {@code constraints} ascent, descent and
     * baseline resize behavior are set from the component; and true is
     * returned. Otherwise false is returned.
     */
    private boolean calculateBaseline(NTxNodeGridNodeExt constraints,
                                      NTxSizeD size) {
        Anchor anchor = constraints.anchor;
        if (anchor == Anchor.BASELINE ||
                anchor == Anchor.BASELINE_LEADING ||
                anchor == Anchor.BASELINE_TRAILING) {
            // Apply the padding to the component, then ask for the baseline.
            double w = size.getWidth() + constraints.ipadx;
            double h = size.getHeight() + constraints.ipady;
            constraints.ascent = constraints.baseline;//getBaseline(w, h);
            if (constraints.ascent >= 0) {
                // Component has a baseline
                double baseline = constraints.ascent;
                // Adjust the ascent and descent to include the insets.
                constraints.descent = h - constraints.ascent +
                        constraints.insets.bottom;
                constraints.ascent += constraints.insets.top;
                constraints.centerPadding = 0;
                if (constraints.baselineResizeBehavior == Component.
                        BaselineResizeBehavior.CENTER_OFFSET) {
                    constraints.centerOffset = baseline - h / 2;
                    if (h % 2 != 0) {
                        constraints.centerOffset--;
                        constraints.centerPadding = 1;
                    }
                }
            }
            return true;
        } else {
            constraints.ascent = -1;
            return false;
        }
    }

    private void adjustForGravity(NTxNodeGridNodeExt constraints,
                                  NTxEditableBounds2 r) {
        double diffx, diffy;
        double cellY = r.y;
        double cellHeight = r.height;

        if (!rightToLeft) {
            r.x += constraints.insets.left;
        } else {
            r.x -= r.width - constraints.insets.right;
        }
        r.width -= (constraints.insets.left + constraints.insets.right);
        r.y += constraints.insets.top;
        r.height -= (constraints.insets.top + constraints.insets.bottom);

        diffx = 0;
        if ((constraints.fill != Fill.HORIZONTAL &&
                constraints.fill != Fill.BOTH)
                && (r.width > (constraints.minWidth + constraints.ipadx))) {
            diffx = r.width - (constraints.minWidth + constraints.ipadx);
            r.width = constraints.minWidth + constraints.ipadx;
        }

        diffy = 0;
        if ((constraints.fill != Fill.VERTICAL &&
                constraints.fill != Fill.BOTH)
                && (r.height > (constraints.minHeight + constraints.ipady))) {
            diffy = r.height - (constraints.minHeight + constraints.ipady);
            r.height = constraints.minHeight + constraints.ipady;
        }

        switch (constraints.anchor) {
            case BASELINE:
                r.x += diffx / 2;
                alignOnBaseline(constraints, r, cellY, cellHeight);
                break;
            case BASELINE_LEADING:
                if (rightToLeft) {
                    r.x += diffx;
                }
                alignOnBaseline(constraints, r, cellY, cellHeight);
                break;
            case BASELINE_TRAILING:
                if (!rightToLeft) {
                    r.x += diffx;
                }
                alignOnBaseline(constraints, r, cellY, cellHeight);
                break;
            case ABOVE_BASELINE:
                r.x += diffx / 2;
                alignAboveBaseline(constraints, r, cellY, cellHeight);
                break;
            case ABOVE_BASELINE_LEADING:
                if (rightToLeft) {
                    r.x += diffx;
                }
                alignAboveBaseline(constraints, r, cellY, cellHeight);
                break;
            case ABOVE_BASELINE_TRAILING:
                if (!rightToLeft) {
                    r.x += diffx;
                }
                alignAboveBaseline(constraints, r, cellY, cellHeight);
                break;
            case BELOW_BASELINE:
                r.x += diffx / 2;
                alignBelowBaseline(constraints, r, cellY, cellHeight);
                break;
            case BELOW_BASELINE_LEADING:
                if (rightToLeft) {
                    r.x += diffx;
                }
                alignBelowBaseline(constraints, r, cellY, cellHeight);
                break;
            case BELOW_BASELINE_TRAILING:
                if (!rightToLeft) {
                    r.x += diffx;
                }
                alignBelowBaseline(constraints, r, cellY, cellHeight);
                break;
            case CENTER:
                r.x += diffx / 2;
                r.y += diffy / 2;
                break;
            case PAGE_START:
            case NORTH:
                r.x += diffx / 2;
                break;
            case NORTHEAST:
                r.x += diffx;
                break;
            case EAST:
                r.x += diffx;
                r.y += diffy / 2;
                break;
            case SOUTHEAST:
                r.x += diffx;
                r.y += diffy;
                break;
            case PAGE_END:
            case SOUTH:
                r.x += diffx / 2;
                r.y += diffy;
                break;
            case SOUTHWEST:
                r.y += diffy;
                break;
            case WEST:
                r.y += diffy / 2;
                break;
            case NORTHWEST:
                break;
            case LINE_START:
                if (rightToLeft) {
                    r.x += diffx;
                }
                r.y += diffy / 2;
                break;
            case LINE_END:
                if (!rightToLeft) {
                    r.x += diffx;
                }
                r.y += diffy / 2;
                break;
            case FIRST_LINE_START:
                if (rightToLeft) {
                    r.x += diffx;
                }
                break;
            case FIRST_LINE_END:
                if (!rightToLeft) {
                    r.x += diffx;
                }
                break;
            case LAST_LINE_START:
                if (rightToLeft) {
                    r.x += diffx;
                }
                r.y += diffy;
                break;
            case LAST_LINE_END:
                if (!rightToLeft) {
                    r.x += diffx;
                }
                r.y += diffy;
                break;
            default:
                throw new IllegalArgumentException("illegal anchor value");
        }
    }

    /**
     * Positions on the baseline.
     *
     * @param cellY      the location of the row, does not include insets
     * @param cellHeight the height of the row, does not take into account
     *                   insets
     * @param r          available bounds for the component, is padded by insets and
     *                   ipady
     */
    private void alignOnBaseline(NTxNodeGridNodeExt cons, NTxEditableBounds2 r,
                                 double cellY, double cellHeight) {
        if (cons.ascent >= 0) {
            if (cons.baselineResizeBehavior == Component.
                    BaselineResizeBehavior.CONSTANT_DESCENT) {
                // Anchor to the bottom.
                // Baseline is at (cellY + cellHeight - maxDescent).
                // Bottom of component (maxY) is at baseline + descent
                // of component. We need to subtract the bottom inset here
                // as the descent in the constraints object includes the
                // bottom inset.
                double maxY = cellY + cellHeight -
                        layoutInfo.maxDescent[cons.tempY + cons.tempHeight - 1] +
                        cons.descent - cons.insets.bottom;
                if (!cons.isVerticallyResizable()) {
                    // Component not resizable, calculate y location
                    // from maxY - height.
                    r.y = maxY - cons.minHeight;
                    r.height = cons.minHeight;
                } else {
                    // Component is resizable. As brb is constant descent,
                    // can expand component to fill region above baseline.
                    // Subtract out the top inset so that components insets
                    // are honored.
                    r.height = maxY - cellY - cons.insets.top;
                }
            } else {
                // BRB is not constant_descent
                double baseline; // baseline for the row, relative to cellY
                // Component baseline, includes insets.top
                double ascent = cons.ascent;
                if (layoutInfo.hasConstantDescent(cons.tempY)) {
                    // Mixed ascent/descent in same row, calculate position
                    // off maxDescent
                    baseline = cellHeight - layoutInfo.maxDescent[cons.tempY];
                } else {
                    // Only ascents/unknown in this row, anchor to top
                    baseline = layoutInfo.maxAscent[cons.tempY];
                }
                if (cons.baselineResizeBehavior == Component.
                        BaselineResizeBehavior.OTHER) {
                    // BRB is other, which means we can only determine
                    // the baseline by asking for it again giving the
                    // size we plan on using for the component.
                    boolean fits = false;
                    ascent = componentAdjusting.baseline;//.getBaseline(r.width, r.height);
                    if (ascent >= 0) {
                        // Component has a baseline, pad with top inset
                        // (this follows from calculateBaseline which
                        // does the same).
                        ascent += cons.insets.top;
                    }
                    if (ascent >= 0 && ascent <= baseline) {
                        // Components baseline fits within rows baseline.
                        // Make sure the descent fits within the space as well.
                        if (baseline + (r.height - ascent - cons.insets.top) <=
                                cellHeight - cons.insets.bottom) {
                            // It fits, we're good.
                            fits = true;
                        } else if (cons.isVerticallyResizable()) {
                            // Doesn't fit, but it's resizable.  Try
                            // again assuming we'll get ascent again.
                            int ascent2 = componentAdjusting.baseline;//.getBaseline(r.width, cellHeight - cons.insets.bottom -baseline + ascent);
                            if (ascent2 >= 0) {
                                ascent2 += cons.insets.top;
                            }
                            if (ascent2 >= 0 && ascent2 <= ascent) {
                                // It'll fit
                                r.height = cellHeight - cons.insets.bottom -
                                        baseline + ascent;
                                ascent = ascent2;
                                fits = true;
                            }
                        }
                    }
                    if (!fits) {
                        // Doesn't fit, use min size and original ascent
                        ascent = cons.ascent;
                        r.width = cons.minWidth;
                        r.height = cons.minHeight;
                    }
                }
                // Reset the components y location based on
                // components ascent and baseline for row. Because ascent
                // includes the baseline
                r.y = cellY + baseline - ascent + cons.insets.top;
                if (cons.isVerticallyResizable()) {
                    switch (cons.baselineResizeBehavior) {
                        case CONSTANT_ASCENT:
                            r.height = Math.max(cons.minHeight, cellY + cellHeight -
                                    r.y - cons.insets.bottom);
                            break;
                        case CENTER_OFFSET: {
                            double upper = r.y - cellY - cons.insets.top;
                            double lower = cellY + cellHeight - r.y -
                                    cons.minHeight - cons.insets.bottom;
                            double delta = Math.min(upper, lower);
                            delta += delta;
                            if (delta > 0 &&
                                    (cons.minHeight + cons.centerPadding +
                                            delta) / 2 + cons.centerOffset != baseline) {
                                // Off by 1
                                delta--;
                            }
                            r.height = cons.minHeight + delta;
                            r.y = cellY + baseline -
                                    (r.height + cons.centerPadding) / 2 -
                                    cons.centerOffset;
                        }
                        break;
                        case OTHER:
                            // Handled above
                            break;
                        default:
                            break;
                    }
                }
            }
        } else {
            centerVertically(cons, r, cellHeight);
        }
    }

    /**
     * Positions the specified component above the baseline. That is
     * the bottom edge of the component will be aligned along the baseline.
     * If the row does not have a baseline, this centers the component.
     */
    private void alignAboveBaseline(NTxNodeGridNodeExt cons, NTxEditableBounds2 r,
                                    double cellY, double cellHeight) {
        if (layoutInfo.hasBaseline(cons.tempY)) {
            double maxY; // Baseline for the row
            if (layoutInfo.hasConstantDescent(cons.tempY)) {
                // Prefer descent
                maxY = cellY + cellHeight - layoutInfo.maxDescent[cons.tempY];
            } else {
                // Prefer ascent
                maxY = cellY + layoutInfo.maxAscent[cons.tempY];
            }
            if (cons.isVerticallyResizable()) {
                // Component is resizable. Top edge is offset by top
                // inset, bottom edge on baseline.
                r.y = cellY + cons.insets.top;
                r.height = maxY - r.y;
            } else {
                // Not resizable.
                r.height = cons.minHeight + cons.ipady;
                r.y = maxY - r.height;
            }
        } else {
            centerVertically(cons, r, cellHeight);
        }
    }

    /**
     * Positions below the baseline.
     */
    private void alignBelowBaseline(NTxNodeGridNodeExt cons, NTxEditableBounds2 r,
                                    double cellY, double cellHeight) {
        if (layoutInfo.hasBaseline(cons.tempY)) {
            if (layoutInfo.hasConstantDescent(cons.tempY)) {
                // Prefer descent
                r.y = cellY + cellHeight - layoutInfo.maxDescent[cons.tempY];
            } else {
                // Prefer ascent
                r.y = cellY + layoutInfo.maxAscent[cons.tempY];
            }
            if (cons.isVerticallyResizable()) {
                r.height = cellY + cellHeight - r.y - cons.insets.bottom;
            }
        } else {
            centerVertically(cons, r, cellHeight);
        }
    }

    private void centerVertically(NTxNodeGridNodeExt cons, NTxEditableBounds2 r,
                                  double cellHeight) {
        if (!cons.isVerticallyResizable()) {
            r.y += Math.max(0, (cellHeight - cons.insets.top -
                    cons.insets.bottom - cons.minHeight -
                    cons.ipady) / 2);
        }
    }

    private NTxSizeD getMinSize(NTxNodeGridBagLayoutInfo info) {
        double width = 0;
        double height = 0;
        int i, t;
        Insets insets = parentInsets;

        t = 0;
        for (i = 0; i < info.width; i++)
            t += info.minWidth[i];
        width = t + insets.left + insets.right;

        t = 0;
        for (i = 0; i < info.height; i++)
            t += info.minHeight[i];
        height = t + insets.top + insets.bottom;

        return new NTxSizeD(width, height);
    }


    public void doLayout() {
        NTxNodeGridNodeExt comp;
        int compindex;
        NTxNodeGridNodeExt constraints;
        Insets insets = parentInsets;
        NTxNodeGridNodeExt[] components = children.toArray(new NTxNodeGridNodeExt[0]);
        NTxSizeD d;
        NTxEditableBounds2 r = new NTxEditableBounds2(0.0, 0.0, 0.0, 0.0);
        int i;
        double diffw, diffh;
        double weight;
        NTxNodeGridBagLayoutInfo info;


        /*
         * If the parent has no slaves anymore, then don't do anything
         * at all:  just leave the parent's size as-is.
         */
        if (components.length == 0 &&
                (columnWidths == null || columnWidths.length == 0) &&
                (rowHeights == null || rowHeights.length == 0)) {
            return;
        }

        /*
         * Pass #1: scan all the slaves to figure out the total amount
         * of space needed.
         */

        info = getLayoutInfo(PREFERREDSIZE);
        d = getMinSize(info);

        if (parentBounds.getWidth() < d.getWidth() || parentBounds.getHeight() < d.getHeight()) {
            info = getLayoutInfo(MINSIZE);
            d = getMinSize(info);
        }

        layoutInfo = info;
        r.width = d.getWidth();
        r.height = d.getHeight();

        /*
         * DEBUG
         *
         * DumpLayoutInfo(info);
         * for (compindex = 0 ; compindex < components.length ; compindex++) {
         * comp = components[compindex];
         * if (!comp.isVisible())
         *      continue;
         * constraints = lookupConstraints(comp);
         * DumpConstraints(constraints);
         * }
         * System.out.println("minSize " + r.width + " " + r.height);
         */

        /*
         * If the current dimensions of the window don't match the desired
         * dimensions, then adjust the minWidth and minHeight arrays
         * according to the weights.
         */

        diffw = parentBounds.getWidth() - r.width;
        if (diffw != 0) {
            weight = 0.0;
            for (i = 0; i < info.width; i++)
                weight += info.weightX[i];
            if (weight > 0.0) {
                for (i = 0; i < info.width; i++) {
                    double dx =  ((((double) diffw) * info.weightX[i]) / weight);
                    info.minWidth[i] += dx;
                    r.width += dx;
                    if (info.minWidth[i] < 0) {
                        r.width -= info.minWidth[i];
                        info.minWidth[i] = 0;
                    }
                }
            }
            diffw = parentBounds.getWidth() - r.width;
        } else {
            diffw = 0;
        }

        diffh = parentBounds.getHeight() - r.height;
        if (diffh != 0) {
            weight = 0.0;
            for (i = 0; i < info.height; i++)
                weight += info.weightY[i];
            if (weight > 0.0) {
                for (i = 0; i < info.height; i++) {
                    double dy = ((diffh * info.weightY[i]) / weight);
                    info.minHeight[i] += dy;
                    r.height += dy;
                    if (info.minHeight[i] < 0) {
                        r.height -= info.minHeight[i];
                        info.minHeight[i] = 0;
                    }
                }
            }
            diffh = parentBounds.getHeight() - r.height;
        } else {
            diffh = 0;
        }

        /*
         * DEBUG
         *
         * System.out.println("Re-adjusted:");
         * DumpLayoutInfo(info);
         */

        /*
         * Now do the actual layout of the slaves using the layout information
         * that has been collected.
         */

        info.startx = diffw / 2 + insets.left;
        info.starty = diffh / 2 + insets.top;

        for (compindex = 0; compindex < components.length; compindex++) {
            comp = components[compindex];
            if (!comp.visible) {
                continue;
            }
            constraints = comp;

            if (!rightToLeft) {
                r.x = info.startx;
                for (i = 0; i < constraints.tempX; i++)
                    r.x += info.minWidth[i];
            } else {
                r.x = parentBounds.getWidth() - (diffw / 2 + insets.right);
                for (i = 0; i < constraints.tempX; i++)
                    r.x -= info.minWidth[i];
            }

            r.y = info.starty;
            for (i = 0; i < constraints.tempY; i++)
                r.y += info.minHeight[i];

            r.width = 0.0;
            for (i = constraints.tempX;
                 i < (constraints.tempX + constraints.tempWidth);
                 i++) {
                r.width += info.minWidth[i];
            }

            r.height = 0.0;
            for (i = constraints.tempY;
                 i < (constraints.tempY + constraints.tempHeight);
                 i++) {
                r.height += info.minHeight[i];
            }

            componentAdjusting = comp;
            adjustForGravity(constraints, r);

            /* fix for 4408108 - components were being created outside of the container */
            /* fix for 4969409 "-" replaced by "+"  */
            if (r.x < 0) {
                r.width += r.x;
                r.x = 0.0;
            }

            if (r.y < 0) {
                r.height += r.y;
                r.y = 0.0;
            }

            /*
             * If the window is too small to be interesting then
             * unmap it.  Otherwise configure it and then make sure
             * it's mapped.
             */

            if ((r.width <= 0) || (r.height <= 0)) {
                comp.bounds = new NTxBounds2D(parentBounds.getX(), parentBounds.getY(), 0, 0);
            } else {
                comp.bounds = new NTxBounds2D(parentBounds.getX() + r.x, parentBounds.getY() + r.y, r.width, r.height);
            }
            comp.propagate();
        }
    }

    public enum Fill {
        VERTICAL, BOTH, HORIZONTAL, NONE

    }

    public enum Anchor {
        BASELINE, BASELINE_LEADING, BASELINE_TRAILING, ABOVE_BASELINE, ABOVE_BASELINE_LEADING, ABOVE_BASELINE_TRAILING, BELOW_BASELINE, BELOW_BASELINE_LEADING, BELOW_BASELINE_TRAILING, NONE, CENTER, PAGE_START, NORTH, NORTHEAST, EAST,
        SOUTHEAST, PAGE_END, SOUTH, SOUTHWEST, LAST_LINE_END, LAST_LINE_START, FIRST_LINE_END, FIRST_LINE_START, LINE_END, LINE_START, NORTHWEST, WEST;
    }

    private static class NTxNodeGridNodeExt extends NTxNodeGridNode implements Cloneable {
        NTxNodeGridNode base;
        int tempX;
        int tempY;
        int tempWidth;
        int tempHeight;
        double minWidth;
        double minHeight;
        transient double ascent;
        transient double descent;
        transient double centerPadding;
        transient double centerOffset;

        public NTxNodeGridNodeExt(NTxNodeGridNode o) {
            super(o);
            this.base = o;
        }

        public NTxNodeGridNodeExt copy() {
            return (NTxNodeGridNodeExt) clone();
        }

        /**
         * Creates a copy of this grid bag constraint.
         *
         * @return a copy of this grid bag constraint
         */
        protected Object clone() {
            return super.clone();
        }

        public void propagate() {
            base.bounds = bounds;
            base.baselineResizeBehavior = baselineResizeBehavior;
        }
    }

    public static class NTxNodeGridNode implements Cloneable {
        public static final int RELATIVE = -1;
        public static final int REMAINDER = 0;
        public int baseline = -1;
        public Component.BaselineResizeBehavior baselineResizeBehavior = Component.BaselineResizeBehavior.OTHER;
        Object node;
        boolean visible = true;
        NTxSizeD preferredSize;
        NTxSizeD minimumSize;
        NTxBounds2D bounds;
        int index;


        public int gridx;
        public int gridy;
        public int gridwidth;
        public int gridheight;
        public double weightx;
        public double weighty;
        public Anchor anchor = Anchor.NONE;
        public Fill fill = Fill.NONE;
        public Insets insets;
        public int ipadx;
        public int ipady;

        public NTxNodeGridNode(NTxNodeGridNode o) {
            this.baseline = o.baseline;
            this.baselineResizeBehavior = o.baselineResizeBehavior;
            this.node = o.node;
            this.visible = o.visible;
            this.preferredSize = o.preferredSize;
            this.minimumSize = o.minimumSize;
            this.bounds = o.bounds;
            this.index = o.index;
            this.gridx = o.gridx;
            this.gridy = o.gridy;
            this.gridwidth = o.gridwidth;
            this.gridheight = o.gridheight;
            this.weightx = o.weightx;
            this.weighty = o.weighty;
            this.anchor = o.anchor;
            this.fill = o.fill;
            this.insets = o.insets;
            this.ipadx = o.ipadx;
            this.ipady = o.ipady;
        }

        public NTxSizeD getPreferredSize() {
            return preferredSize;
        }

        public NTxNodeGridNode setPreferredSize(NTxSizeD preferredSize) {
            this.preferredSize = preferredSize;
            return this;
        }

        public NTxSizeD getMinimumSize() {
            return minimumSize;
        }

        public NTxNodeGridNode setMinimumSize(NTxSizeD minimumSize) {
            this.minimumSize = minimumSize;
            return this;
        }

        public NTxBounds2D getBounds() {
            return bounds;
        }

        public NTxNodeGridNode setBounds(NTxBounds2D bounds) {
            this.bounds = bounds;
            return this;
        }

        public int getGridx() {
            return gridx;
        }

        public NTxNodeGridNode setGridx(int gridx) {
            this.gridx = gridx;
            return this;
        }

        public int getGridy() {
            return gridy;
        }

        public NTxNodeGridNode setGridy(int gridy) {
            this.gridy = gridy;
            return this;
        }

        public int getGridwidth() {
            return gridwidth;
        }

        public NTxNodeGridNode setGridwidth(int gridwidth) {
            this.gridwidth = gridwidth;
            return this;
        }

        public int getGridheight() {
            return gridheight;
        }

        public NTxNodeGridNode setGridheight(int gridheight) {
            this.gridheight = gridheight;
            return this;
        }

        public double getWeightx() {
            return weightx;
        }

        public NTxNodeGridNode setWeightx(double weightx) {
            this.weightx = weightx;
            return this;
        }

        public double getWeighty() {
            return weighty;
        }

        public NTxNodeGridNode setWeighty(double weighty) {
            this.weighty = weighty;
            return this;
        }

        public Anchor getAnchor() {
            return anchor;
        }

        public NTxNodeGridNode setAnchor(Anchor anchor) {
            this.anchor = anchor;
            return this;
        }

        public Fill getFill() {
            return fill;
        }

        public NTxNodeGridNode setFill(Fill fill) {
            this.fill = fill;
            return this;
        }

        public Insets getInsets() {
            return insets;
        }

        public NTxNodeGridNode setInsets(Insets insets) {
            this.insets = insets;
            return this;
        }

        public int getIpadx() {
            return ipadx;
        }

        public NTxNodeGridNode setIpadx(int ipadx) {
            this.ipadx = ipadx;
            return this;
        }

        public int getIpady() {
            return ipady;
        }

        public NTxNodeGridNode setIpady(int ipady) {
            this.ipady = ipady;
            return this;
        }

        public boolean isVisible() {
            return visible;
        }

        public NTxNodeGridNode setVisible(boolean visible) {
            this.visible = visible;
            return this;
        }

        public Object getNode() {
            return node;
        }

        public NTxNodeGridNode setNode(Object node) {
            this.node = node;
            return this;
        }


        public NTxNodeGridNode(Object node) {
            this.node = node;
            gridx = RELATIVE;
            gridy = RELATIVE;
            gridwidth = 1;
            gridheight = 1;

            weightx = 0;
            weighty = 0;
            anchor = Anchor.CENTER;
            fill = Fill.NONE;

            insets = new Insets(0, 0, 0, 0);
            ipadx = 0;
            ipady = 0;
        }

        public NTxNodeGridNode() {
            gridx = RELATIVE;
            gridy = RELATIVE;
            gridwidth = 1;
            gridheight = 1;

            weightx = 0;
            weighty = 0;
            anchor = Anchor.CENTER;
            fill = Fill.NONE;

            insets = new Insets(0, 0, 0, 0);
            ipadx = 0;
            ipady = 0;
        }

        public NTxNodeGridNode(int gridx, int gridy,
                               int gridwidth, int gridheight,
                               double weightx, double weighty,
                               Anchor anchor, Fill fill,
                               Insets insets, int ipadx, int ipady) {
            this.gridx = gridx;
            this.gridy = gridy;
            this.gridwidth = gridwidth;
            this.gridheight = gridheight;
            this.fill = fill;
            this.ipadx = ipadx;
            this.ipady = ipady;
            this.insets = insets;
            this.anchor = anchor;
            this.weightx = weightx;
            this.weighty = weighty;
        }

        public NTxNodeGridNode copy() {
            return (NTxNodeGridNode) clone();
        }

        /**
         * Creates a copy of this grid bag constraint.
         *
         * @return a copy of this grid bag constraint
         */
        protected Object clone() {
            try {
                NTxNodeGridNode c = (NTxNodeGridNode) super.clone();
                c.insets = (Insets) insets.clone();
                return c;
            } catch (CloneNotSupportedException e) {
                // this shouldn't happen, since we are Cloneable
                throw new InternalError(e);
            }
        }

        boolean isVerticallyResizable() {
            return (fill == Fill.BOTH || fill == Fill.VERTICAL);
        }

        public NTxNodeGridNode pos(int x, int y) {
            setGridx(x);
            setGridy(y);
            return this;
        }

        public NTxNodeGridNode span(int x, int y) {
            setGridwidth(x);
            setGridheight(y);
            return this;
        }

        public NTxNodeGridNode weight(int x, int y) {
            setWeightx(x);
            setWeighty(y);
            return this;
        }

        @Override
        public String toString() {
            return "NTxNodeGridNode{" +
                    ", node=" + node +
                    ", index=" + index +
                    ", bounds=" + bounds +
                    ", pos=(" + gridx +","+gridy+")"+
                    ", span=(" + gridwidth +","+gridheight+")"+
                    ", weight=(" + weightx +","+weighty+")"+
                    ", anchor=" + anchor +
                    ", fill=" + fill +
                    ", insets=" + _Insets_ToString(insets) +
                    ", ipadx=" + ipadx +
                    ", ipady=" + ipady +
                    ", preferredSize=" + preferredSize +
                    ", minimumSize=" + minimumSize +
                    ", baseline=" + baseline +
                    ", baselineResizeBehavior=" + baselineResizeBehavior +
                    ", visible=" + visible +
                    '}';
        }
        private String _Insets_ToString(Insets a){
            if(a==null){
                return "null";
            }
            return "("+a.top+","+a.left+","+a.bottom+","+a.right+")";
        }
    }

    static class NTxNodeGridBagLayoutInfo {
        int width, height;          /* number of  cells: horizontal and vertical */
        double startx, starty;         /* starting point for layout */
        int[] minWidth;             /* largest minWidth in each column */
        int[] minHeight;            /* largest minHeight in each row */
        double[] weightX;           /* largest weight in each column */
        double[] weightY;           /* largest weight in each row */
        boolean hasBaseline;        /* Whether or not baseline layout has been
         * requested and one of the components
         * has a valid baseline. */
        // These are only valid if hasBaseline is true and are indexed by
        // row.
        short[] baselineType;       /* The type of baseline for a particular
         * row.  A mix of the BaselineResizeBehavior
         * constants (1 << ordinal()) */
        double[] maxAscent;            /* Max ascent (baseline). */
        double[] maxDescent;           /* Max descent (height - baseline) */

        /**
         * Creates an instance of GridBagLayoutInfo representing {@code GridBagLayout}
         * grid cells with it's own parameters.
         *
         * @param width  the columns
         * @param height the rows
         * @since 6.0
         */
        NTxNodeGridBagLayoutInfo(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Returns true if the specified row has any component aligned on the
         * baseline with a baseline resize behavior of CONSTANT_DESCENT.
         */
        boolean hasConstantDescent(int row) {
            return ((baselineType[row] & (1 << Component.BaselineResizeBehavior.
                    CONSTANT_DESCENT.ordinal())) != 0);
        }

        /**
         * Returns true if there is a baseline for the specified row.
         */
        boolean hasBaseline(int row) {
            return (hasBaseline && baselineType[row] != 0);
        }
    }

}



