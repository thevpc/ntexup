package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.util.NNumberUtils;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

public class NTxEvalUtils {

    public static NElement simplify(NElement e) {
        e = NTxUtils.removeCompilerDeclarationPathAnnotations(e);
        return simplifyPars(e);
    }

    public static NElement simplifyPars(NElement e) {
        if (e.isUplet()) {
            NUpletElement u = e.asUplet().get();
            if (u.params().size() == 1) {
                return simplifyPars(u.params().get(0));
            }
        }
        return e;
    }

    public static NElement substruct(NElement a, NElement b) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            Number na = aa.asNumberValue().get();
            Number nb = bb.asNumberValue().get();
            return NElement.ofNumber(NNumberUtils.substructNumbers(na, nb));
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.MINUS, aa, bb));
    }

    public static NElement remainder(NElement a, NElement b) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            Number na = aa.asNumberValue().get();
            Number nb = bb.asNumberValue().get();
            return NElement.ofNumber(NNumberUtils.reminderNumbers(na, nb));
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.REM, aa, bb));
    }

    public static NElement remainder2(NElement a, NElement b) {
        NElement a1 = NTxEvalUtils.simplify(a);
        NElement b1 = NTxEvalUtils.simplify(b);
        Number n1 = NTxValue.of(a1).asNumber().orNull();
        Number n2 = NTxValue.of(b1).asNumber().orNull();
        if (n1 != null && n2 != null) {
            try {
                Number r = NNumberUtils.reminderNumbers(n1, n2);
                return NElement.ofNumber(r);
            } catch (Exception ex) {
                //
            }
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.REM, a1, b1));
    }

    public static NElement negate(NElement a) {
        NElement aa = simplify(a);
        if (aa.isNumber()) {
            Number na = aa.asNumberValue().get();
            return NElement.ofNumber(NNumberUtils.negateNumber(na));
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.MINUS, aa));
    }

    public static NElement inv(NElement a, MathContext mc) {
        NElement aa = simplify(a);
        if (aa.isNumber()) {
            Number na = aa.asNumberValue().get();
            return NElement.ofNumber(NNumberUtils.invNumber(na, mc));
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.DIV, aa));
    }

    public static NElement add(NElement a, NElement b) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            Number na = aa.asNumberValue().get();
            Number nb = bb.asNumberValue().get();
            return NElement.ofNumber(NNumberUtils.addNumbers(na, nb));
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.PLUS, aa, bb));
    }

    public static NElement div(NElement a, NElement b, MathContext mc) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            Number na = aa.asNumberValue().get();
            Number nb = bb.asNumberValue().get();
            return NElement.ofNumber(NNumberUtils.divideNumbers(na, nb, mc));
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.DIV, aa, bb));
    }

    public static NElement mul(NElement a, NElement b, MathContext mc) {
        NElement aa = simplify(a);
        NElement bb = simplify(b);
        if (aa.isNumber() && bb.isNumber()) {
            Number na = aa.asNumberValue().get();
            Number nb = bb.asNumberValue().get();
            return NElement.ofNumber(NNumberUtils.multiplyNumbers(na, nb, mc));
        }
        return NElement.ofUplet(NElement.ofOp(NOperatorSymbol.MUL, aa, bb));
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

    public static NElement[] evalInterval(NElement f, NElement s) {
        f = simplify(f);
        s = simplify(s);
        if (f.isNumber() && s.isNumber()) {
            NElementType ct = NElements.of().commonNumberType(f.type(), s.type());
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
        return NElement.ofBoolean(a1.equals(b1));
    }
}
