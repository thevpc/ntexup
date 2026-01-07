package net.thevpc.ntexup.extension.shapes3d.api;

public class NTxTransformation {

    // Identity matrix
    public static double[][] identity() {
        return new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
    }

    // Translation matrix
    public static double[][] translate(double tx, double ty, double tz) {
        return new double[][]{
                {1, 0, 0, tx},
                {0, 1, 0, ty},
                {0, 0, 1, tz},
                {0, 0, 0, 1}
        };
    }

    // Rotation matrix around the X axis
    public static double[][] rotateX(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[][]{
                {1, 0, 0, 0},
                {0, cos, -sin, 0},
                {0, sin, cos, 0},
                {0, 0, 0, 1}
        };
    }

    // Rotation matrix around the Y axis
    public static double[][] rotateY(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[][]{
                {cos, 0, sin, 0},
                {0, 1, 0, 0},
                {-sin, 0, cos, 0},
                {0, 0, 0, 1}
        };
    }

    // Rotation matrix around the Z axis
    public static double[][] rotateZ(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new double[][]{
                {cos, -sin, 0, 0},
                {sin, cos, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
    }

    // Scaling matrix
    public static double[][] scale(double sx, double sy, double sz) {
        return new double[][]{
                {sx, 0, 0, 0},
                {0, sy, 0, 0},
                {0, 0, sz, 0},
                {0, 0, 0, 1}
        };
    }

    // Multiply two matrices
    public static double[][] multiply(double[][] a, double[][] b) {
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 4; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }
}
