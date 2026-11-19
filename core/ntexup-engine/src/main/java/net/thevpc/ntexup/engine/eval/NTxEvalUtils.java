package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.util.NTxNumberUtils;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.math.NNumberUtils;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

public class NTxEvalUtils {

    public static NElement simplify(NElement e) {
        e = NTxUtils.removeCompilerDeclarationPathAnnotations(e);
        return simplifyPars(e);
    }

    public static NElement simplifyPars(NElement e) {
        if (e.isTuple()) {
            NTupleElement u = e.asTuple().get();
            if (u.params().size() == 1) {
                return simplifyPars(u.params().get(0));
            }
        }
        return e;
    }

//    public static NElement substruct(NElement a, NElement b) {
//        NElement aa = simplify(a);
//        NElement bb = simplify(b);
//        if (aa.isNumber() && bb.isNumber()) {
//            Number na = aa.asNumberValue().get();
//            Number nb = bb.asNumberValue().get();
//            return NElement.ofNumber(NNumberUtils.substructNumbers(na, nb));
//        }
//        return NElement.ofUplet(NElement.ofBinaryInfixOperator(NOperatorSymbol.MINUS, aa, bb));
//    }

//    public static NElement remainder(NElement a, NElement b) {
//        NElement aa = simplify(a);
//        NElement bb = simplify(b);
//        if (aa.isNumber() && bb.isNumber()) {
//            Number na = aa.asNumberValue().get();
//            Number nb = bb.asNumberValue().get();
//            return NElement.ofNumber(NNumberUtils.reminderNumbers(na, nb));
//        }
//        return NElement.ofUplet(NElement.ofBinaryInfixOperator(NOperatorSymbol.REM, aa, bb));
//    }

    public static NOptional<NElement> remainder(NElement a, NElement b, MathContext mc) {
        NElement a1 = NTxEvalUtils.simplify(a);
        NElement b1 = NTxEvalUtils.simplify(b);
        if(a1.isNumber() && b1.isNumber()){
            NNumberElement nna = a1.asNumber().get();
            NNumberElement nnb = b1.asNumber().get();

            String sa = NStringUtils.strip(nna.numberSuffix()).toLowerCase();
            String sb = NStringUtils.strip(nnb.numberSuffix()).toLowerCase();
            if (sa.equals(sb)) {
                //same unit
                Number na = nna.numberValue();
                Number nb = nnb.numberValue();
                Number r;
                try {
                    r = NNumberUtils.reminderNumbers(na, nb);
                } catch (Exception ex) {
                    return NOptional.ofNamedEmpty("remainder");
                }
                return NOptional.of(NElement.ofNumber(r, null, null));
            } else if (sa.isEmpty()) {
                //upper no unit, result is inverse of the unit =>> no unit
                Number na = nna.numberValue();
                Number nb = nnb.numberValue();
                Number r;
                try {
                    r = NNumberUtils.reminderNumbers(na, nb);
                } catch (Exception ex) {
                    return NOptional.ofNamedEmpty("remainder");
                }
                return NOptional.of(NElement.ofNumber(r, null, null));
            } else if (sb.isEmpty()) {
                //lower no unit, result is the unit
                Number na = nna.numberValue();
                Number nb = nnb.numberValue();
                Number r;
                try {
                    r = NNumberUtils.reminderNumbers(na, nb);
                } catch (Exception ex) {
                    return NOptional.ofNamedEmpty("remainder");
                }
                return NOptional.of(NElement.ofNumber(r, null, sa));
            } else {
                NOptional<NNumberElement> asi = NTxNumberUtils.toSIUnit(nna);
                NOptional<NNumberElement> bsi = NTxNumberUtils.toSIUnit(nnb);
                if (asi.isPresent() && bsi.isPresent()) {

                    Number r;
                    try {
                        r = NNumberUtils.reminderNumbers(asi.get().numberValue(), bsi.get().numberValue());
                    } catch (Exception ex) {
                        return NOptional.ofNamedEmpty("remainder");
                    }
                    return NOptional.of(NElement.ofNumber(r, null, null));
                }
            }
        }
        return NOptional.ofNamedEmpty("remainder");
    }

    public static NOptional<NElement> negate(NElement a) {
        NElement aa = simplify(a);
        if (aa.isNumber()) {
            NNumberElement nn = aa.asNumber().get();
            Number na = nn.numberValue();
            return NOptional.of(NElement.ofNumber(NNumberUtils.negateNumber(na), nn.numberLayout(), nn.numberSuffix()));
        }
        return NOptional.ofNamedEmpty("negate");
    }

    public static NOptional<NElement> inv(NElement a, MathContext mc) {
        NElement aa = simplify(a);
        if (aa.isNumber()) {
            NNumberElement na = aa.asNumber().get();
            Number nv = na.numberValue();
            String unit = na.numberSuffix();
            String inverseUnit = NTxNumberUtils.detectUnitType(unit).inverse().unitName();
            return NOptional.of(NElement.ofNumber(NNumberUtils.invNumber(nv, mc), null, inverseUnit));
        }
        return NOptional.ofNamedEmpty("inv");
    }

    public static NOptional<NElement> div(NElement a, NElement b, MathContext mc) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            NNumberElement nna = aa.asNumber().get();
            NNumberElement nnb = bb.asNumber().get();
            String sa = NStringUtils.strip(nna.numberSuffix()).toLowerCase();
            String sb = NStringUtils.strip(nnb.numberSuffix()).toLowerCase();
            if (sa.equals(sb)) {
                //same unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.divideNumbers(na, nb, mc), null, null));
            } else if (sa.isEmpty()) {
                //upper no unit, result is inverse of the unit =>> no unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.divideNumbers(na, nb, mc), null, ""));
            } else if (sb.isEmpty()) {
                //lower no unit, result is the unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.divideNumbers(na, nb, mc), null, sa));
            } else {
                NOptional<NNumberElement> asi = NTxNumberUtils.toSIUnit(nna);
                NOptional<NNumberElement> bsi = NTxNumberUtils.toSIUnit(nnb);
                if (asi.isPresent() && bsi.isPresent()) {
                    return NOptional.of(NElement.ofNumber(NNumberUtils.divideNumbers(
                            asi.get().numberValue(), bsi.get().numberValue(), mc), null, null));
                }
            }
        }
        return NOptional.ofNamedEmpty("div");
    }

    public static NOptional<NElement> mul(NElement a, NElement b, MathContext mc) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            NNumberElement nna = aa.asNumber().get();
            NNumberElement nnb = bb.asNumber().get();
            String sa = NStringUtils.strip(nna.numberSuffix()).toLowerCase();
            String sb = NStringUtils.strip(nnb.numberSuffix()).toLowerCase();
            if (sa.isEmpty()) {
                //upper no unit, result is inverse of the unit =>> no unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.multiplyNumbers(na, nb, mc), null, sb));
            } else if (sb.isEmpty()) {
                //lower no unit, result is the unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.multiplyNumbers(na, nb, mc), null, sa));
            } else {
                NOptional<NNumberElement> asi = NTxNumberUtils.toSIUnit(nna);
                NOptional<NNumberElement> bsi = NTxNumberUtils.toSIUnit(nnb);
                if (asi.isPresent() && bsi.isPresent()) {
                    return NOptional.of(NElement.ofNumber(NNumberUtils.multiplyNumbers(
                            asi.get().numberValue(), bsi.get().numberValue(), mc), null, null));
                }
            }
        }
        return NOptional.ofNamedEmpty("mul");
    }

    public static NOptional<NElement> add(NElement a, NElement b, MathContext mc) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            NNumberElement nna = aa.asNumber().get();
            NNumberElement nnb = bb.asNumber().get();
            String sa = NStringUtils.strip(nna.numberSuffix()).toLowerCase();
            String sb = NStringUtils.strip(nnb.numberSuffix()).toLowerCase();
            if (sa.equals(sb)) {
                //same unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.addNumbers(na, nb), null, sa));
            } else if (sa.isEmpty()) {
                //upper no unit, result is inverse of the unit =>> no unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.addNumbers(na, nb), null, sb));
            } else if (sb.isEmpty()) {
                //lower no unit, result is the unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.addNumbers(na, nb), null, sa));
            } else {
                NOptional<NNumberElement> asi = NTxNumberUtils.toSIUnit(nna);
                NOptional<NNumberElement> bsi = NTxNumberUtils.toSIUnit(nnb);
                if (asi.isPresent() && bsi.isPresent()) {
                    NTxNumberUtils.UnitType u1 = NTxNumberUtils.detectUnitType(asi.get().numberSuffix());
                    if (
                            u1
                                    == NTxNumberUtils.detectUnitType(bsi.get().numberSuffix())
                    ) {
                        if (u1.isKnown()) {
                            return NOptional.of(NElement.ofNumber(NNumberUtils.addNumbers(
                                    asi.get().numberValue(), bsi.get().numberValue()), null, u1.unitName()));
                        }
                        return NOptional.of(NElement.ofNumber(NNumberUtils.addNumbers(
                                asi.get().numberValue(), bsi.get().numberValue()), null, null));
                    }
                    return NOptional.of(NElement.ofNumber(NNumberUtils.addNumbers(
                            asi.get().numberValue(), bsi.get().numberValue()), null, null));
                }
            }
        }
        // string concatenation: "Total: " + 3  ->  "Total: 3"
        if ((aa.isString() && (bb.isString() || bb.isNumber() || bb.isBoolean()))
                || (bb.isString() && (aa.isNumber() || aa.isBoolean()))) {
            return NOptional.of(NElement.ofString(concatPart(aa) + concatPart(bb)));
        }
        return NOptional.ofNamedEmpty("add");
    }

    private static String concatPart(NElement e) {
        return e.isString() ? e.asStringValue().get() : e.toString();
    }

    public static NOptional<NElement> substruct(NElement a, NElement b, MathContext mc) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            NNumberElement nna = aa.asNumber().get();
            NNumberElement nnb = bb.asNumber().get();
            String sa = NStringUtils.strip(nna.numberSuffix()).toLowerCase();
            String sb = NStringUtils.strip(nnb.numberSuffix()).toLowerCase();
            if (sa.equals(sb)) {
                //same unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.substructNumbers(na, nb), null, sa));
            } else if (sa.isEmpty()) {
                //upper no unit, result is inverse of the unit =>> no unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.substructNumbers(na, nb), null, sb));
            } else if (sb.isEmpty()) {
                //lower no unit, result is the unit
                Number na = aa.asNumberValue().get();
                Number nb = bb.asNumberValue().get();
                return NOptional.of(NElement.ofNumber(NNumberUtils.substructNumbers(na, nb), null, sa));
            } else {
                NOptional<NNumberElement> asi = NTxNumberUtils.toSIUnit(nna);
                NOptional<NNumberElement> bsi = NTxNumberUtils.toSIUnit(nnb);
                if (asi.isPresent() && bsi.isPresent()) {
                    NTxNumberUtils.UnitType u1 = NTxNumberUtils.detectUnitType(asi.get().numberSuffix());
                    if (
                            u1
                                    == NTxNumberUtils.detectUnitType(bsi.get().numberSuffix())
                    ) {
                        switch (u1) {
                            case METER: {
                                return NOptional.of(NElement.ofNumber(NNumberUtils.substructNumbers(
                                        asi.get().numberValue(), bsi.get().numberValue()), null, "m"));
                            }
                            case HERTZ: {
                                return NOptional.of(NElement.ofNumber(NNumberUtils.substructNumbers(
                                        asi.get().numberValue(), bsi.get().numberValue()), null, "Hz"));
                            }
                        }
                        return NOptional.of(NElement.ofNumber(NNumberUtils.substructNumbers(
                                asi.get().numberValue(), bsi.get().numberValue()), null, null));
                    }
                    return NOptional.of(NElement.ofNumber(NNumberUtils.substructNumbers(
                            asi.get().numberValue(), bsi.get().numberValue()), null, null));
                }
            }
        }
        return NOptional.ofNamedEmpty("substruct");
    }


    public static int compareNumbers(Number a, Number b) {
        return NNumberUtils.compareNumbers(a, b);
    }

    public static Number reminderNumbers(Number a, Number b) {
        return NNumberUtils.reminderNumbers(a, b);
    }

    public static Number addNumbers(Number a, Number b) {
        return NNumberUtils.addNumbers(a, b);
    }

    /**
     * interval with an explicit step ([a -> b step s]). The step magnitude is
     * used; direction follows a/b. Integral bounds+step yield integers.
     */
    public static NElement[] evalInterval(NElement f, NElement s, NElement step) {
        f = simplify(f);
        s = simplify(s);
        step = simplify(step);
        if (!f.isNumber() || !s.isNumber() || !step.isNumber()) {
            return null;
        }
        java.math.BigDecimal a = new java.math.BigDecimal(String.valueOf(f.asNumberValue().get()));
        java.math.BigDecimal b = new java.math.BigDecimal(String.valueOf(s.asNumberValue().get()));
        java.math.BigDecimal st = new java.math.BigDecimal(String.valueOf(step.asNumberValue().get())).abs();
        if (st.signum() == 0) {
            return null;
        }
        boolean integral = !f.type().isAnyDecimalNumber() && !s.type().isAnyDecimalNumber() && !step.type().isAnyDecimalNumber();
        int dir = a.compareTo(b) <= 0 ? 1 : -1;
        java.math.BigDecimal inc = dir > 0 ? st : st.negate();
        List<NElement> all = new ArrayList<>();
        java.math.BigDecimal i = a;
        int guard = 0;
        while ((dir > 0 ? i.compareTo(b) <= 0 : i.compareTo(b) >= 0) && guard++ < 1_000_000) {
            all.add(integral ? NElement.ofLong(i.longValueExact()) : NElement.ofDouble(i.doubleValue()));
            i = i.add(inc);
        }
        return all.toArray(new NElement[0]);
    }

    public static NElement[] evalInterval(NElement f, NElement s) {
        f = simplify(f);
        s = simplify(s);
        if (f.isNumber() && s.isNumber()) {
            NElementType ct = NElementType.computeNumber(f.type(), s.type());
            if (ct.isAnyNumber()) {
                Number fn = f.asNumberType(ct).get().asNumberValue().get();
                Number sn = s.asNumberType(ct).get().asNumberValue().get();
                int u = NTxEvalUtils.compareNumbers(fn, sn);
                List<NElement> all = new ArrayList<>();
                if (u == 0) {
                    all.add(NElement.ofNumber(fn));
                } else if (u < 0) {
                    Number i = fn;
                    while (NTxEvalUtils.compareNumbers(i, sn) <= 0) {
                        all.add(NElement.ofNumber(i));
                        i = NNumberUtils.addNumbers(i, (byte) 1);
                    }
                } else if (u > 0) {
                    Number i = fn;
                    while (NTxEvalUtils.compareNumbers(i, sn) >= 0) {
                        all.add(NElement.ofNumber(i));
                        i = NNumberUtils.addNumbers(i, (byte) -1);
                    }
                }
                return all.toArray(new NElement[0]);
            }
        }
        return null;
    }


    public static NElement eq(NElement a, NElement b) {
        NElement a1 = NTxEvalUtils.simplify(a);
        NElement b1 = NTxEvalUtils.simplify(b);
        if (a1.isNumber() && b1.isNumber()) {
            return NElement.ofBoolean(NTxNumberUtils.eq(a1.asNumber().get(), b1.asNumber().get()));
        }
        return NElement.ofBoolean(a1.equals(b1));
    }
}
