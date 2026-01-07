package net.thevpc.ntexup.extension.shapes3d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxCamera3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxMatrix3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxVector3D;

public class NTxCamera3DImpl implements NTxCamera3D {
    private NTxPoint3D position;
    private NTxPoint3D target;
    NTxVector3D worldUp = new NTxVector3D(0, 1, 0);

    public static NTxCamera3DImpl fromSpherical(double azimuthDeg, double elevationDeg, double distance) {
        double az = Math.toRadians(azimuthDeg);
        double el = Math.toRadians(elevationDeg);

        // Camera looks toward origin (0,0,0)
        double x = distance * Math.cos(el) * Math.cos(az);
        double y = distance * Math.cos(el) * Math.sin(az);
        double z = distance * Math.sin(el);

        return new NTxCamera3DImpl(new NTxPoint3D(x, y, z), new NTxPoint3D(0, 0, 0));
    }

    public static NTxCamera3DImpl isometric() {
        return fromSpherical(-45, 35.264, 1000); // classic isometric
    }

    public static NTxCamera3DImpl defaultCamera() {
        return new NTxCamera3DImpl(new NTxPoint3D(0, 0, 1000), new NTxPoint3D(0, 0, 0));
    }

    public NTxCamera3DImpl() {
    }

    public NTxCamera3DImpl(NTxPoint3D position, NTxPoint3D target) {
        this.position = position;
        this.target = target;
    }

    public NTxPoint2D projectFromWorldToScreen(NTxPoint3D worldPoint, NTxPoint2D screenOrigin) {
        // 1. Transform world point to camera space
        NTxMatrix3D viewMatrix = getViewMatrix();
        NTxPoint3D pCam = viewMatrix.multiplyPoint(worldPoint);


        double x2d = pCam.x;
        double y2d = pCam.y;

        // 4. Offset by screen origin (e.g., top-left of the scene3d area on page)
        if (screenOrigin != null) {
            x2d += screenOrigin.x;
            y2d += screenOrigin.y;
        }
        return new NTxPoint2D(x2d, y2d);
    }

    public NTxPoint2D[] projectFromWorldToScreen(NTxPoint3D[] worldPoint, NTxPoint2D screenOrigin) {
        // 1. Transform world point to camera space
        NTxMatrix3D viewMatrix = getViewMatrix();
        NTxPoint2D[] ret=new NTxPoint2D[worldPoint.length];
        for (int i = 0; i < worldPoint.length; i++) {
            NTxPoint3D pCam = viewMatrix.multiplyPoint(worldPoint[i]);
            double x2d = pCam.x;
            double y2d = pCam.y;
            // 4. Offset by screen origin (e.g., top-left of the scene3d area on page)
            if (screenOrigin != null) {
                x2d += screenOrigin.x;
                y2d += screenOrigin.y;
            }
            ret[i]=new NTxPoint2D(x2d, y2d);
        }
        return ret;
    }

    public NTxMatrix3D getViewMatrix() {
        // Forward: normalized direction from eye to target
        NTxVector3D f = target.asVector().minus(position.asVector()).normalize();

        // Right: perpendicular to forward and worldUp
        NTxVector3D r = f.cross(worldUp).normalize();

        // True up: perpendicular to forward and right
        NTxVector3D u = r.cross(f); // already normalized if r,f are orthonormal

        // Build matrix (column-major layout for column-vector multiplication)
        double[][] m = new double[4][4];

        // Rotation part (inverse of camera orientation)
        m[0][0] = r.x;
        m[1][0] = r.y;
        m[2][0] = r.z;
        m[3][0] = 0;
        m[0][1] = u.x;
        m[1][1] = u.y;
        m[2][1] = u.z;
        m[3][1] = 0;
        m[0][2] = -f.x;
        m[1][2] = -f.y;
        m[2][2] = -f.z;
        m[3][2] = 0;

        // Translation part: -R * eye
        double tx = -(r.x * position.x + r.y * position.y + r.z * position.z);
        double ty = -(u.x * position.x + u.y * position.y + u.z * position.z);
        double tz = -(-f.x * position.x - f.y * position.y - f.z * position.z);

        m[0][3] = tx;
        m[1][3] = ty;
        m[2][3] = tz;
        m[3][3] = 1.0;

        return new NTxMatrix3D(m);
    }


    // Getters / setters
    public NTxPoint3D getPosition() {
        return position;
    }

    public NTxPoint3D getTarget() {
        return target;
    }

    public NTxVector3D getUp() {
        return worldUp;
    }

    public void setPosition(NTxPoint3D position) {
        this.position = position;
    }

    public void setTarget(NTxPoint3D target) {
        this.target = target;
    }
}
