package net.thevpc.ntexup.api.util;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

public class NTxNumberUtils {

    public static NOptional<Double> toMeter(NNumberElement a) {
        if (a == null) return NOptional.of(0.0);
        if (a.isComplexNumber()) {
            return NOptional.ofNamedEmpty("not a double");
        }

        double d = a.asDoubleValue().get();
        String s = NStringUtils.trim(a.numberSuffix()).toLowerCase();

        switch (s) {
            // --- Metric (SI) ---
            case "m":  return NOptional.of(d);
            case "km": return NOptional.of(d * 1E3);
            case "dm": return NOptional.of(d * 1E-1);
            case "cm": return NOptional.of(d * 1E-2);
            case "mm": return NOptional.of(d * 1E-3);
            case "um":
            case "μm": return NOptional.of(d * 1E-6);
            case "nm": return NOptional.of(d * 1E-9);
            case "pm": return NOptional.of(d * 1E-12);

            // --- Imperial / US Customary ---
            case "in":
            case "inch":
            case "inches": return NOptional.of(d * 0.0254);
            case "ft":
            case "foot":
            case "feet":  return NOptional.of(d * 0.3048);
            case "yd":
            case "yard":  return NOptional.of(d * 0.9144);
            case "mi":
            case "mile":
            case "miles": return NOptional.of(d * 1609.344);
            case "nmi":   return NOptional.of(d * 1852.0); // Nautical Mile

            // --- Maritime / Small Scale ---
            case "fathom": return NOptional.of(d * 1.8288);
            case "mil":    return NOptional.of(d * 0.0000254); // 1/1000th of an inch

            // --- Astronomical ---
            case "au": return NOptional.of(d * 1.495978707E11);
            case "ly": return NOptional.of(d * 9.4607E15);
            case "pc": return NOptional.of(d * 3.085677581E16);

            // Default or unknown suffix
            case "": return NOptional.of(d);
            default: return NOptional.of(d);
        }
    }

    public static NOptional<Double> toHertz(NNumberElement a) {
        if (a == null) return NOptional.of(0.0);
        if (a.isComplexNumber()) {
            return NOptional.ofNamedEmpty("not a double");
        }

        double d = a.asDoubleValue().get();
        String s = NStringUtils.trim(a.numberSuffix()).toLowerCase();

        switch (s) {
            // --- Standard SI Prefixes ---
            case "hz":  return NOptional.of(d);
            case "khz": return NOptional.of(d * 1E3);
            case "mhz": return NOptional.of(d * 1E6);
            case "ghz": return NOptional.of(d * 1E9);
            case "thz": return NOptional.of(d * 1E12);
            case "phz": return NOptional.of(d * 1E15); // Petahertz
            case "ehz": return NOptional.of(d * 1E18); // Exahertz

            // --- Sub-Hertz (Low Frequency) ---
            case "mhz_low": // Millihertz (using suffix to avoid collision with Mega)
            case "mhz.":    return NOptional.of(d * 1E-3);
            case "uhz":
            case "μhz":     return NOptional.of(d * 1E-6);

            // --- Rotational / Angular Frequency ---
            case "rpm":    return NOptional.of(d / 60.0);       // Revolutions per minute
            case "rps":    return NOptional.of(d);             // Revolutions per second
            case "rad_s":  return NOptional.of(d / (2 * Math.PI)); // Radians per second
            case "deg_s":  return NOptional.of(d / 360.0);     // Degrees per second

            // --- Temporal / Occurrences ---
            case "bpm":    return NOptional.of(d / 60.0);       // Beats per minute (Music/Heart rate)
            case "fps":    return NOptional.of(d);             // Frames per second

            // Default or unknown suffix
            case "": return NOptional.of(d);
            default: return NOptional.of(d);
        }
    }

    public static NNumberElement ofNumber(NNumberElement a) {
        return a == null ? (NNumberElement) NElement.ofDouble(0) : a;
    }

    public static NNumberElement ofNumber(Double a) {
        return (NNumberElement) (a == null ? NElement.ofDouble(0) : NElement.ofDouble(a));
    }

    public static double[] dtimes(double min, double max, int times) {
        double[] d = new double[times];
        if (times == 1) {
            d[0] = min;
        } else {
            double step = (max - min) / (times - 1);
            for (int i = 0; i < d.length; i++) {
                d[i] = min + i * step;
            }
        }
        return d;
    }

    public static double[] dsteps(double min, double max, double step) {
        if (step >= 0) {
            if (max < min) {
                return new double[0];
            }
            int times = (int) Math.abs((max - min) / step) + 1;
            double[] d = new double[times];
            for (int i = 0; i < d.length; i++) {
                d[i] = min + i * step;
            }
            return d;
        } else {
            if (min < max) {
                return new double[0];
            }
            int times = (int) Math.abs((max - min) / step) + 1;
            double[] d = new double[times];
            for (int i = 0; i < d.length; i++) {
                d[i] = min + i * step;
            }
            return d;
        }
    }
}
