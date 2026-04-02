//package net.thevpc.ntexup.api.util;
//
//public class NTxMinMax {
//    private double min = Double.NaN;
//    private double max = Double.NaN;
//    private boolean infinite = true;
//
//    public NTxMinMax() {
//    }
//
//    public boolean isInfinite() {
//        return infinite;
//    }
//
//    public NTxMinMax setInfinite(boolean infinite) {
//        this.infinite = infinite;
//        return this;
//    }
//
//    public void registerAbsValues(double[] d) {
//        for (double aD : d) {
//            registerAbsValue(aD);
//        }
//    }
//
//    public void registerAbsValue(double d) {
//        registerValue(Math.abs(d));
//    }
//
//    public void registerValue(double d) {
//        if (Double.isNaN(min) || (!Double.isNaN(d) && d < min)) {
//            if (infinite || Double.isFinite(d)) {
//                min = d;
//            }
//        }
//        if (Double.isNaN(max) || (!Double.isNaN(d) && d > max)) {
//            if (infinite || Double.isFinite(d)) {
//                max = d;
//            }
//        }
//    }
//
//    public void registerValues(double[] d) {
//        for (double aD : d) {
//            registerValue(aD);
//        }
//    }
//
//    public void registerValues(double[][] d) {
//        for (double[] aD : d) {
//            for (double anAD : aD) {
//                registerValue(anAD);
//            }
//        }
//    }
//
//    public void registerValues(double[][][] d) {
//        for (double[][] z : d) {
//            for (double[] y : z) {
//                for (double x : y) {
//                    registerValue(x);
//                }
//            }
//        }
//    }
//
//    public void registerAbsValues(double[][] d) {
//        for (double[] aD : d) {
//            for (double anAD : aD) {
//                registerAbsValue(anAD);
//            }
//        }
//    }
//
//    public float getRatio(double d) {
//        if (Double.isNaN(d)) {
//            return Float.NaN;
//        }
//        if (min == max) {
//            return 0f;
//        }
//        return (float) ((d - min) / (max - min));
//    }
//
//
//    public boolean isUnset() {
//        return Double.isNaN(min) || Double.isNaN(max);
//    }
//
//    public double getMin() {
//        return min;
//    }
//
//    public double getLength() {
//        return max - min;
//    }
//
//    public double getMax() {
//        return max;
//    }
//
//    public boolean isNaN() {
//        return Double.isNaN(min) || Double.isNaN(max);
//    }
//
//    @Override
//    public int hashCode() {
//        int result;
//        result = Double.hashCode(min);
//        result = 31 * result + Double.hashCode(max);
//        return result;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        NTxMinMax minMax = (NTxMinMax) o;
//
//        if (Double.compare(minMax.min, min) != 0) return false;
//        return Double.compare(minMax.max, max) == 0;
//    }
//
//    @Override
//    public String toString() {
//        return "{" + "min=" + min + ", max=" + max + '}';
//    }
//
//    public double ratio(double z) {
//        double w = (max - min);
//        if (w == 0) {
//            return 1;
//        }
//        return (z - min) / w;
//    }
//}
