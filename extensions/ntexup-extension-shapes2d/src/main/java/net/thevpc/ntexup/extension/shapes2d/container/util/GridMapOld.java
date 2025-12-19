package net.thevpc.ntexup.extension.shapes2d.container.util;

import net.thevpc.ntexup.api.document.elem2d.NTxInt2;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class GridMapOld<T> {
    private int gridWidth = 0;
    private int gridHeight = 0;
    private BitSet occupied = new BitSet();
    private final List<ItemWithPosition<?>> items = new ArrayList<>();

    private int idx(int x, int y) {
        return y * gridWidth + x;
    }

    /** Ensure the grid is at least this big. */
    public void ensureSize(int requiredWidth, int requiredHeight) {
        if (requiredWidth <= gridWidth && requiredHeight <= gridHeight) return;

        int newWidth = Math.max(gridWidth, requiredWidth);
        int newHeight = Math.max(gridHeight, requiredHeight);

        // create new BitSet
        BitSet newOccupied = new BitSet(newWidth * newHeight);

        // copy old data row by row
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                if (occupied.get(y * gridWidth + x)) {
                    newOccupied.set(y * newWidth + x);
                }
            }
        }

        this.gridWidth = newWidth;
        this.gridHeight = newHeight;
        this.occupied = newOccupied;
    }

    /** Add an item with dimensions and user payload */
    public ItemWithPosition<T> add(int w, int h, T userObject) {
        NTxInt2 pos = firstFreePosition(w, h);
        if (pos == null) {
            // if no free pos, expand grid by growing downward
            int newHeight = gridHeight + h;
            ensureSize(Math.max(gridWidth, w), newHeight);
            pos = new NTxInt2(0, gridHeight - h); // append at bottom-left
        }
        ensureSize(pos.getX() + w, pos.getY() + h);
        occupy(pos.getX(), pos.getY(), w, h, true);

        ItemWithPosition<T> item = new ItemWithPosition<>(pos.getX(), pos.getY(), w, h, userObject);
        @SuppressWarnings("unchecked")
        ItemWithPosition<?> rawItem = (ItemWithPosition<?>) item;
        items.add(rawItem);

        return item;
    }

    public NTxInt2 firstFreePosition(int w, int h) {
        for (int y = 0; y <= gridHeight - h; y++) {
            for (int x = 0; x <= gridWidth - w; x++) {
                if (isFirstFreePosition(x, y, w, h)) {
                    return new NTxInt2(x, y);
                }
            }
        }
        return null;
    }

    public boolean isFirstFreePosition(int x, int y, int w, int h) {
        if (x < 0 || y < 0) return false;
        if (x + w > gridWidth || y + h > gridHeight) return false;

        for (int yy = y; yy < y + h; yy++) {
            int start = yy * gridWidth + x;
            int end = start + w;
            if (occupied.get(start, end).cardinality() > 0) {
                return false;
            }
        }
        return true;
    }

    private void occupy(int x, int y, int w, int h, boolean state) {
        for (int yy = y; yy < y + h; yy++) {
            int start = yy * gridWidth + x;
            int end = start + w;
            if (state) {
                occupied.set(start, end);
            } else {
                occupied.clear(start, end);
            }
        }
    }

    public List<ItemWithPosition<?>> list() {
        return new ArrayList<>(items);
    }

    public void dump() {
        for (int y = 0; y < gridHeight; y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < gridWidth; x++) {
                sb.append(occupied.get(idx(x, y)) ? 'X' : '.');
            }
            System.out.println(sb);
        }
    }
    /** Total number of columns currently in the grid */
    public int columns() {
        return gridWidth;
    }

    /** Total number of rows currently in the grid */
    public int rows() {
        return gridHeight;
    }
    public static class ItemWithPosition<T> {
        public final int x, y, w, h;
        public final T userObject;

        public ItemWithPosition(int x, int y, int w, int h, T userObject) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.userObject = userObject;
        }

        @Override
        public String toString() {
            return String.format("Item[x=%d,y=%d,w=%d,h=%d,obj=%s]",
                    x, y, w, h, userObject);
        }
    }
}
