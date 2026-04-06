package net.thevpc.ntexup.api.util;

import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.log.NLog;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.util.Objects;

public class NTxNumberUtils {

    public static boolean eq(NNumberElement a1, NNumberElement b1) {
        String us1 = a1.numberSuffix();
        String us2 = b1.numberSuffix();
        if(us1.equalsIgnoreCase(us2)){
            return a1.numberValue().equals(b1.numberValue());
        }
        NTxNumberUtils.UnitType u1 = NTxNumberUtils.detectUnitType(us1);
        NTxNumberUtils.UnitType u2 = NTxNumberUtils.detectUnitType(us2);
        if (u1 == u2) {
            if (u1.isKnown()) {
                NOptional<NNumberElement> aa1 = NTxNumberUtils.toSIUnit(a1);
                NOptional<NNumberElement> bb1 = NTxNumberUtils.toSIUnit(b1);
                if(aa1.isPresent() &&  bb1.isPresent()){
                    return aa1.get().numberValue().equals(bb1.get().numberValue());
                }
            }
        }
        // dont use a1.equals(b1), this includes the comments etc!!
        if(!NStringUtils.trim(a1.numberSuffix()).equalsIgnoreCase(NStringUtils.trim(b1.numberSuffix()))){
            return false;
        }
        return Objects.equals(a1.numberValue(), b1.numberValue());
    }

    public enum UnitType {
        INV_METER,
        METER,
        HERTZ,
        SECOND,
        UNKNOWN,
        NONE;

        public String unitName() {
            switch (this) {
                case METER: {
                    return "m";
                }
                case INV_METER: {
                    return "dioptre";
                }
                case HERTZ: {
                    return "Hz";
                }
                case SECOND: {
                    return "s";
                }
                case UNKNOWN: {
                    return "unknown";
                }
                case NONE: {
                    return "";
                }
            }
            return "unknown";
        }

        public UnitType inverse() {
            switch (this) {
                case METER: {
                    return INV_METER;
                }
                case INV_METER: {
                    return METER;
                }
                case HERTZ: {
                    return SECOND;
                }
                case SECOND: {
                    return HERTZ;
                }
                case UNKNOWN: {
                    return UNKNOWN;
                }
                case NONE: {
                    return NONE;
                }
            }
            return this;
        }

        public boolean isKnown() {
            switch (this) {
                case UNKNOWN: ;
                case NONE:
                    return false;
            }
            return true;
        }
    }

    public static NOptional<NNumberElement> toSIUnit(NNumberElement a) {
        UnitType s = detectUnitType(a.numberSuffix());
        switch (s) {
            case METER:
                return toMeter(a).map(x -> (NNumberElement) NElement.ofNumber(x, null, s.unitName()));
            case HERTZ:
                return toHertz(a).map(x -> (NNumberElement) NElement.ofNumber(x, null, s.unitName()));
        }
        return NOptional.of(a);
    }

    public static UnitType detectUnitType(String any) {
        if (NBlankable.isBlank(any)) {
            return UnitType.NONE;
        }
        any = NStringUtils.trim(any).toLowerCase();
        switch (any) {
            // --- Metric (SI) ---
            case "m":
            case "km":
            case "dm":
            case "cm":
            case "mm":
            case "um":
            case "μm":
            case "nm":
            case "pm":

                // --- Imperial / US Customary ---
            case "in":
            case "inch":
            case "inches":
            case "ft":
            case "foot":
            case "feet":
            case "yd":
            case "yard":
            case "mi":
            case "mile":
            case "miles":
            case "nmi":    // Nautical Mile

                // --- Maritime / Small Scale ---
            case "fathom":
            case "mil":     // 1/1000th of an inch

                // --- Astronomical ---
            case "au":
            case "ly":
            case "pc":
                return UnitType.METER;

            // --- Standard SI Prefixes ---
            case "hz":
            case "khz":
            case "mhz":
            case "ghz":
            case "thz":
            case "phz":  // Petahertz
            case "ehz":  // Exahertz

                // --- Sub-Hertz (Low Frequency) ---
            case "mhz_low": // Millihertz (using suffix to avoid collision with Mega)
            case "mhz.":
            case "uhz":
            case "μhz":

                // --- Rotational / Angular Frequency ---
            case "rpm":           // Revolutions per minute
            case "rps":                 // Revolutions per second
            case "rad_s":   // Radians per second
            case "deg_s":       // Degrees per second

                // --- Temporal / Occurrences ---
            case "bpm":           // Beats per minute (Music/Heart rate)
            case "fps":               // Frames per second
                return UnitType.HERTZ;
        }
        return UnitType.UNKNOWN;
    }

    public static NOptional<Double> toMeter(NElement e) {
        if (e == null || e.isNull()) return NOptional.of(0.0);
        if(!e.isNumber()){
            return NOptional.ofNamedEmpty("not a double");
        }
        if (e.isComplexNumber()) {
            return NOptional.ofNamedEmpty("not a double");
        }
        NNumberElement a = e.asNumber().get();

        double d = a.asDoubleValue().get();
        String s = NStringUtils.trim(a.numberSuffix()).toLowerCase();

        switch (s) {
            // --- Metric (SI) ---
            case "m":
                return NOptional.of(d);
            case "km":
                return NOptional.of(d * 1E3);
            case "dm":
                return NOptional.of(d * 1E-1);
            case "cm":
                return NOptional.of(d * 1E-2);
            case "mm":
                return NOptional.of(d * 1E-3);
            case "um":
            case "μm":
                return NOptional.of(d * 1E-6);
            case "nm":
                return NOptional.of(d * 1E-9);
            case "pm":
                return NOptional.of(d * 1E-12);

            // --- Imperial / US Customary ---
            case "in":
            case "inch":
            case "inches":
                return NOptional.of(d * 0.0254);
            case "ft":
            case "foot":
            case "feet":
                return NOptional.of(d * 0.3048);
            case "yd":
            case "yard":
                return NOptional.of(d * 0.9144);
            case "mi":
            case "mile":
            case "miles":
                return NOptional.of(d * 1609.344);
            case "nmi":
                return NOptional.of(d * 1852.0); // Nautical Mile

            // --- Maritime / Small Scale ---
            case "fathom":
                return NOptional.of(d * 1.8288);
            case "mil":
                return NOptional.of(d * 0.0000254); // 1/1000th of an inch

            // --- Astronomical ---
            case "au":
                return NOptional.of(d * 1.495978707E11);
            case "ly":
                return NOptional.of(d * 9.4607E15);
            case "pc":
                return NOptional.of(d * 3.085677581E16);

            // Default or unknown suffix
            case "":
                return NOptional.of(d);
            default:
                return NOptional.of(d);
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
            case "hz":
                return NOptional.of(d);
            case "khz":
                return NOptional.of(d * 1E3);
            case "mhz":
                return NOptional.of(d * 1E6);
            case "ghz":
                return NOptional.of(d * 1E9);
            case "thz":
                return NOptional.of(d * 1E12);
            case "phz":
                return NOptional.of(d * 1E15); // Petahertz
            case "ehz":
                return NOptional.of(d * 1E18); // Exahertz

            // --- Sub-Hertz (Low Frequency) ---
            case "mhz_low": // Millihertz (using suffix to avoid collision with Mega)
            case "mhz.":
                return NOptional.of(d * 1E-3);
            case "uhz":
            case "μhz":
                return NOptional.of(d * 1E-6);

            // --- Rotational / Angular Frequency ---
            case "rpm":
                return NOptional.of(d / 60.0);       // Revolutions per minute
            case "rps":
                return NOptional.of(d);             // Revolutions per second
            case "rad_s":
                return NOptional.of(d / (2 * Math.PI)); // Radians per second
            case "deg_s":
                return NOptional.of(d / 360.0);     // Degrees per second

            // --- Temporal / Occurrences ---
            case "bpm":
                return NOptional.of(d / 60.0);       // Beats per minute (Music/Heart rate)
            case "fps":
                return NOptional.of(d);             // Frames per second

            // Default or unknown suffix
            case "":
                return NOptional.of(d);
            default:
                return NOptional.of(d);
        }
    }

    public static NNumberElement ofNumber(NNumberElement a) {
        return a == null ? (NNumberElement) NElement.ofDouble(0) : a;
    }

    public static NNumberElement ofNumber(Double a) {
        return (NNumberElement) (a == null ? NElement.ofDouble(0) : NElement.ofDouble(a));
    }

    public static NNumberElement asNumberElement(NElement e, NTxResolutionContext context) {
        if (e.isNumber()) {
            return e.asNumber().get();
        }
        NOptional<NElement> e2 = context.evalExpression(e);
        if(e2.isPresent()) {
            if (e2.get().isNumber()) {
                return e2.get().asNumber().get();
            }
        }
        context.log(NMsg.ofC("invalid number %s", e).asError());
        return null;
    }

    public static double evalMeterPosition(NNumberElement e, NNumberElement baseSize, NNumberElement baseOffset) {
        NTxNumberUtils.UnitType unitType = NTxNumberUtils.detectUnitType(e.numberSuffix());
        switch (unitType) {
            case METER: {
                return NTxNumberUtils.toMeter(e).get();
            }
            case NONE: {
                return baseSize.numberValue().doubleValue() * e.numberValue().doubleValue() / 100.0 + baseOffset.numberValue().doubleValue();
            }
            case UNKNOWN: {
                switch (NStringUtils.trim(e.numberSuffix())) {
                    case "%": {
                        return baseSize.numberValue().doubleValue() * e.numberValue().doubleValue() / 100.0 + baseOffset.numberValue().doubleValue();
                    }
                    case "%P": {
                        NLog.ofScoped(NTxNumberUtils.class).log(NMsg.ofC("invalid %P ignored in %s", e).asError());
                        return baseSize.numberValue().doubleValue() * e.numberValue().doubleValue() / 100.0 + baseOffset.numberValue().doubleValue();
                    }
                }
            }
        }
        NLog.ofScoped(NTxNumberUtils.class).log(NMsg.ofC("invalid %s in %s", e.numberSuffix(), e).asError());
        return baseSize.numberValue().doubleValue();
    }

}
