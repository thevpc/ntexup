package net.thevpc.ntexup.extension.simple3d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem3d.NTxCamera3D;
import net.thevpc.ntexup.api.document.elem3d.NTxMatrix3D;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;
import net.thevpc.ntexup.api.document.elem3d.NTxVector3D;

public class NTxCamera3DImpl implements NTxCamera3D {
    private NTxPoint3D position;
    private NTxPoint3D target;
    private NTxVector3D up;

    public static NTxCamera3DImpl defaultCamera() {
        return new NTxCamera3DImpl(new NTxPoint3D(0, 0, 1000),new NTxPoint3D(0, 0, 0),new NTxVector3D(0, 1, 0));
    }

    public NTxCamera3DImpl() {}
    public NTxCamera3DImpl(NTxPoint3D position, NTxPoint3D target, NTxVector3D up) {
        this.position = position;
        this.target = target;
        this.up = up;
    }

    public NTxMatrix3D getViewMatrix() {
//        NTxVector3D forward = target.asVector().minus(position.asVector()).normalize(); // -Z axis
        NTxVector3D forward = position.asVector().minus(target.asVector()).normalize(); // -Z axis
        NTxVector3D right = up.cross(forward).normalize(); // +X axis
        NTxVector3D trueUp = forward.cross(right).normalize(); // +Y axis

        double[][] m = new double[4][4];
        m[0][0] = right.x; m[0][1] = right.y; m[0][2] = right.z; m[0][3] = -right.dot(position.asVector());
        m[1][0] = trueUp.x; m[1][1] = trueUp.y; m[1][2] = trueUp.z; m[1][3] = -trueUp.dot(position.asVector());
        m[2][0] = forward.x; m[2][1] = forward.y; m[2][2] = forward.z; m[2][3] = -forward.dot(position.asVector());
        m[3][0] = 0; m[3][1] = 0; m[3][2] = 0; m[3][3] = 1;

        return new NTxMatrix3D(m);
    }

    // Project a world point to screen coordinates
    public NTxPoint2D project(NTxPoint3D point, double focalLength) {
        // Transform world point to camera space
        NTxMatrix3D view = getViewMatrix();
        NTxPoint3D pCam = view.multiplyPoint(point);

        // Perspective projection
        double factor = focalLength / (focalLength + pCam.z);  // +Z toward camera
        int x = (int) (pCam.x * factor);
        int y = (int) (pCam.y * factor);
        return new NTxPoint2D(x, y);
    }

    // Getters / setters
    public NTxPoint3D getPosition() { return position; }
    public NTxPoint3D getTarget() { return target; }
    public NTxVector3D getUp() { return up; }
    public void setPosition(NTxPoint3D position) { this.position = position; }
    public void setTarget(NTxPoint3D target) { this.target = target; }
    public void setUp(NTxVector3D up) { this.up = up; }
}
