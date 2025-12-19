package net.thevpc.ntexup.api.document.elem3d;

public class NTxMatrix3D {
    double[][] m;

    public NTxMatrix3D(double[][] m) {
        this.m = m;
    }

    public static NTxMatrix3D identity() {
        return new NTxMatrix3D(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        });
    }

    // Translation matrix
    public NTxMatrix3D translate(double tx, double ty, double tz) {
        return multiply(new NTxMatrix3D(new double[][]{
                {1, 0, 0, tx},
                {0, 1, 0, ty},
                {0, 0, 1, tz},
                {0, 0, 0, 1}
        }));
    }

    // Rotation matrix around the X axis
    public NTxMatrix3D rotateVector(NTxPoint3D vec,double theta) {
// Normalize the axis vector
        double u = vec.x;
        double v = vec.y;
        double w = vec.z;
        double len = Math.sqrt(u*u + v*v + w*w);
        if(len == 0) return this; // avoid zero vector
        u /= len; v /= len; w /= len;

        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        double oneMinusCos = 1 - cos;

        double[][] r = new double[4][4];

        r[0][0] = cos + u*u*oneMinusCos;
        r[0][1] = u*v*oneMinusCos - w*sin;
        r[0][2] = u*w*oneMinusCos + v*sin;
        r[0][3] = 0;

        r[1][0] = v*u*oneMinusCos + w*sin;
        r[1][1] = cos + v*v*oneMinusCos;
        r[1][2] = v*w*oneMinusCos - u*sin;
        r[1][3] = 0;

        r[2][0] = w*u*oneMinusCos - v*sin;
        r[2][1] = w*v*oneMinusCos + u*sin;
        r[2][2] = cos + w*w*oneMinusCos;
        r[2][3] = 0;

        r[3][0] = 0;
        r[3][1] = 0;
        r[3][2] = 0;
        r[3][3] = 1;

        return this.multiply(new NTxMatrix3D(r));
    }
    public NTxMatrix3D rotateLine(NTxPoint3D a,NTxPoint3D b,double theta) {
// Step 1: Translate so point A is at origin
        NTxMatrix3D t1 = this.translate(-a.x, -a.y, -a.z);

        // Step 2: Compute axis vector B-A
        NTxPoint3D axis = new NTxPoint3D(b.x - a.x, b.y - a.y, b.z - a.z);

        // Step 3: Rotate around axis through origin
        NTxMatrix3D rotated = t1.rotateVector(axis, theta);

        // Step 4: Translate back
        return rotated.translate(a.x, a.y, a.z);
    }
    public NTxMatrix3D rotate(double x,double y,double z) {
        NTxMatrix3D a=this;
        if(x!=0) {
            a = a.multiply(NTxMatrix3D.identity().rotateX(x));
        }
        if(y!=0) {
            a = a.multiply(NTxMatrix3D.identity().rotateY(y));
        }
        if(z!=0) {
            a = a.multiply(NTxMatrix3D.identity().rotateZ(z));
        }
        return a;
    }

    public NTxMatrix3D rotateX(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return multiply(new NTxMatrix3D(
                new double[][]{
                        {1, 0, 0, 0},
                        {0, cos, -sin, 0},
                        {0, sin, cos, 0},
                        {0, 0, 0, 1}
                }
        ));
    }

    // Rotation matrix around the Y axis
    public NTxMatrix3D rotateY(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return multiply(new NTxMatrix3D(
                new double[][]{
                        {cos, 0, sin, 0},
                        {0, 1, 0, 0},
                        {-sin, 0, cos, 0},
                        {0, 0, 0, 1}
                }
        ));
    }

    // Rotation matrix around the Z axis
    public NTxMatrix3D rotateZ(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return multiply(new NTxMatrix3D(new double[][]{
                {cos, -sin, 0, 0},
                {sin, cos, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        }));
    }

    // Scaling matrix
    public NTxMatrix3D scale(double sx, double sy, double sz) {
        return multiply(new NTxMatrix3D(new double[][]{
                {sx, 0, 0, 0},
                {0, sy, 0, 0},
                {0, 0, sz, 0},
                {0, 0, 0, 1}
        }));
    }

    public NTxMatrix3D multiply(NTxMatrix3D b) {
        return multiply(this, b);
    }

    // Multiply two matrices
    public static NTxMatrix3D multiply(NTxMatrix3D a, NTxMatrix3D b) {
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 4; k++) {
                    result[i][j] += a.m[i][k] * b.m[k][j];
                }
            }
        }
        return new NTxMatrix3D(result);
    }
    public NTxPoint3D multiplyVector(NTxPoint3D v) {
        //w =0
        double x = m[0][0]*v.x + m[0][1]*v.y + m[0][2]*v.z;
        double y = m[1][0]*v.x + m[1][1]*v.y + m[1][2]*v.z;
        double z = m[2][0]*v.x + m[2][1]*v.y + m[2][2]*v.z;
        return new NTxPoint3D(x, y, z);
    }
    public NTxPoint3D multiplyPoint(NTxPoint3D p) {
        // w = 1
        double x = m[0][0]*p.x + m[0][1]*p.y + m[0][2]*p.z + m[0][3];
        double y = m[1][0]*p.x + m[1][1]*p.y + m[1][2]*p.z + m[1][3];
        double z = m[2][0]*p.x + m[2][1]*p.y + m[2][2]*p.z + m[2][3];
        return new NTxPoint3D(x, y, z);
    }
}
