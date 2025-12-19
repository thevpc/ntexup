package net.thevpc.ntexup.extension.shapes2d.container.util;

import net.thevpc.ntexup.api.document.elem2d.NTxInt2;

import java.util.*;

public class GridMap<T> {
    private final int initialCols, initialRows;
    private int currentCols, currentRows;
    private int lastCol=-1;
    private int lastRow=-1;
    private BitSet bitSet;
    private List<ItemWithPosition<T>> items;

    public GridMap(int initialCols, int initialRows) {
        if (initialCols <= 0 && initialRows <= 0) {
            throw new IllegalArgumentException("Both initialCols and initialRows cannot be <= 0");
        }

        this.initialCols = initialCols;
        this.initialRows = initialRows;
        this.currentCols = Math.max(1, initialCols);
        this.currentRows = Math.max(1, initialRows);
        this.bitSet = new BitSet(currentCols * currentRows);
        this.items = new ArrayList<>();
    }

    public NTxInt2 add(int w, int h, T userObject) {
        NTxInt2 position = firstFreePosition(w, h);
        lastCol=position.getX();
        lastRow=position.getY();
        // Mark the cells as occupied
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int x = position.getX() + dx;
                int y = position.getY() + dy;
                int index = y * currentCols + x;
                bitSet.set(index);
            }
        }

        items.add(new ItemWithPosition<>(position.getX(), position.getY(), w, h, userObject));
        return position;
    }

    private NTxInt2 firstFreePosition(int w, int h) {
        // Simple approach: search existing grid first, then expand if needed

        // Search current grid
        for (int y = 0; y <= currentRows - 1; y++) {
            for (int x = 0; x <= currentCols - 1; x++) {
                if (isFirstFreePosition(x, y, w, h, w > 1, h > 1)) {
                    return new NTxInt2(x, y);
                }
            }
        }
        // Not found in current grid, need to expand
        // For items larger than current grid, expand to fit
        if (w > currentCols || h > currentRows) {
            expandToFit(w, h);
            return firstFreePosition(w, h); // Try again
        }

        // For items that fit grid size but no free space, expand by 1 row or col
        if (initialCols > 0 && (initialRows <= 0 || initialRows > 0)) {
            // Expand vertically (add a new row)
            expandGrid(currentCols, currentRows + 1);
        } else {
            // Expand horizontally (add a new column)
            expandGrid(currentCols + 1, currentRows);
        }

        return firstFreePosition(w, h); // Try again with expanded grid
    }


    private void expandToFit(int w, int h) {
        int newCols = Math.max(currentCols, w);
        int newRows = Math.max(currentRows, h);
        expandGrid(newCols, newRows);
    }

    private void expandGrid(int newCols, int newRows) {
        if (newCols == currentCols && newRows == currentRows) return;

        BitSet newBitSet = new BitSet(newCols * newRows);

        // Copy existing data
        for (int y = 0; y < currentRows; y++) {
            for (int x = 0; x < currentCols; x++) {
                int oldIndex = y * currentCols + x;
                int newIndex = y * newCols + x;
                if (bitSet.get(oldIndex)) {
                    newBitSet.set(newIndex);
                }
            }
        }

        this.currentCols = newCols;
        this.currentRows = newRows;
        this.bitSet = newBitSet;
    }

    private boolean isFirstFreePosition(int x, int y, int w, int h, boolean xexpand, boolean yexpand) {
        if(lastCol>=0 && lastRow>=0){
            int currIndex = (y) * currentCols + (x);
            int lastIndex = (lastRow) * currentCols + lastCol;
            if(currIndex<=lastIndex){
                return false;
            }
        }
        if (!xexpand && !yexpand) {
            if (x + w > currentCols || y + h > currentRows) {
                return false;
            }

            for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                    int index = (y + dy) * currentCols + (x + dx);
                    if (bitSet.get(index)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            if (x + w > currentCols && !xexpand) {
                return false;
            }
            if (y + h > currentRows && !xexpand) {
                return false;
            }
            int newMaxX = currentCols;
            int newMaxY = currentRows;
            for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                    int yi = y + dy;
                    int xi = x + dx;
                    if (xi >= currentCols && xexpand) {
                        newMaxX = xi + 1;
                    }
                    if (yi >= currentRows && yexpand) {
                        newMaxY = yi + 1;
                    }
                    if (xi < currentCols && yi < currentRows) {
                        int index = yi * currentCols + xi;
                        if (bitSet.get(index)) {
                            return false;
                        }
                    }
                }
            }
            if (newMaxX != currentCols || newMaxY != currentRows) {
                expandGrid(newMaxX, newMaxY);
            }
            return true;
        }
    }

    public List<ItemWithPosition<T>> items() {
        return new ArrayList<>(items);
    }

    public int columns() {
        return currentCols;
    }

    public int rows() {
        return currentRows;
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < currentRows; y++) {
            for (int x = 0; x < currentCols; x++) {
                int index = y * currentCols + x;
                sb.append(bitSet.get(index) ? 'X' : '.');
                if (x < currentCols - 1) sb.append(' ');
            }
            if (y < currentRows - 1) sb.append('\n');
        }
        return sb.toString();
    }

    public String dumpWithObjects() {
        // Create a 2D array to hold the string representations
        String[][] grid = new String[currentRows][currentCols];

        // Fill with objects
        for (ItemWithPosition<T> item : items) {
            String objStr = item.getUserObject().toString();
            for (int dy = 0; dy < item.getHeight(); dy++) {
                for (int dx = 0; dx < item.getWidth(); dx++) {
                    int x = item.getX() + dx;
                    int y = item.getY() + dy;
                    if (x < currentCols && y < currentRows) {
                        grid[y][x] = objStr;
                    }
                }
            }
        }

        // Calculate column widths
        int[] colWidths = new int[currentCols];
        for (int x = 0; x < currentCols; x++) {
            for (int y = 0; y < currentRows; y++) {
                if (grid[y][x] != null) {
                    colWidths[x] = Math.max(colWidths[x], grid[y][x].length());
                }
            }
        }

        // Build the output string
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < currentRows; y++) {
            for (int x = 0; x < currentCols; x++) {
                String cell = grid[y][x];
                if (cell == null) cell = "";

                // Pad to column width
                sb.append(String.format("%-" + colWidths[x] + "s", cell));
                if (x < currentCols - 1) sb.append(" ");
            }
            if (y < currentRows - 1) sb.append("\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return dump();
    }

    public int count() {
        return items.size();
    }
}

