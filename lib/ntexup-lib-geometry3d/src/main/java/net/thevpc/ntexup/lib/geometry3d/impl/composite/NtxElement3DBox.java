package net.thevpc.ntexup.lib.geometry3d.impl.composite;

import net.thevpc.ntexup.lib.geometry3d.impl.AbstractNTxElement3D;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxFace;

public class NtxElement3DBox extends AbstractNTxElement3D {
    private NTxPoint3D position;
    private double sizeX;
    private double sizeY;
    private double sizeZ;
    private NtxFace top;
    private NtxFace bottom;
    private NtxFace left;
    private NtxFace right;
    private NtxFace front;
    private NtxFace back;

    public NtxElement3DBox(NTxPoint3D position, double sizeX, double sizeY, double sizeZ) {
        this.position = position;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public NtxFace getTop() {
        return top;
    }

    public NtxElement3DBox setTop(NtxFace top) {
        this.top = top;
        return this;
    }

    public NtxFace getBottom() {
        return bottom;
    }

    public NtxElement3DBox setBottom(NtxFace bottom) {
        this.bottom = bottom;
        return this;
    }

    public NtxFace getLeft() {
        return left;
    }

    public NtxElement3DBox setLeft(NtxFace left) {
        this.left = left;
        return this;
    }

    public NtxFace getRight() {
        return right;
    }

    public NtxElement3DBox setRight(NtxFace right) {
        this.right = right;
        return this;
    }

    public NtxFace getFront() {
        return front;
    }

    public NtxElement3DBox setFront(NtxFace front) {
        this.front = front;
        return this;
    }

    public NtxFace getBack() {
        return back;
    }

    public NtxElement3DBox setBack(NtxFace back) {
        this.back = back;
        return this;
    }

    public NTxPoint3D getPosition() {
        return position;
    }

    public double getSizeX() {
        return sizeX;
    }

    public double getSizeY() {
        return sizeY;
    }

    public double getSizeZ() {
        return sizeZ;
    }

}
