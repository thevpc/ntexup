package net.thevpc.ntexup.extension.shapes2d.container.util;

public class ItemWithPosition<T> {
    private final int x, y, width, height;
    private final T userObject;

    public ItemWithPosition(int x, int y, int width, int height, T userObject) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.userObject = userObject;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public T getUserObject() { return userObject; }

    @Override
    public String toString() {
        return "ItemWithPosition{x=" + x + ", y=" + y + ", w=" + width + ", h=" + height + ", obj=" + userObject + "}";
    }
}
