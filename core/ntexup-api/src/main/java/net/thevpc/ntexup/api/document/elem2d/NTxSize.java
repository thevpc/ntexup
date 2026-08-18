/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.elem.NTupleElement;

import java.util.Objects;

/**
 * @author vpc
 */
public class NTxSize implements NToElement {

    Type type;
    double value;
    double value2;

    public enum Type {
        PARENT,
        PAGE,
        BOUNDS,
        PX,
        REM
    }

    public static NTxSize ofElement(NElement b) {
        if(b==null) {
            return null;
        }
        if (b.isNumber()) {
            Number n = b.asNumberValue().get();
            String u = ((NNumberElement) b).numberSuffix();
            if (u == null) {
                u = "";
            } else {
                u = u.toLowerCase().trim();
            }
            switch (u) {
                case "%p":
                case "%P": {
                    return ofPage(n.doubleValue());
                }
                case "px": {
                    return ofPx(n.doubleValue());
                }
                case "rem": {
                    return ofRem(n.doubleValue());
                }
                case "":
                case "%":
                default: {
                    return ofParent(n.doubleValue());
                }
            }
        } else if(b.isTuple()){
            NTupleElement u = b.asTuple().get();
            return ofBounds(
                    u.get(0).get().asNumberValue().get().doubleValue(),
                    u.get(1).get().asNumberValue().get().doubleValue()
            );
        }
        throw new IllegalStateException("not a size");
    }

    public static NTxSize ofParent(double percent) {
        return new NTxSize(Type.PARENT, percent, 0);
    }

    public static NTxSize ofPage(double percent) {
        return new NTxSize(Type.PAGE, percent, 0);
    }

    public static NTxSize ofPx(double value) {
        return new NTxSize(Type.PX, value, 0);
    }

    public static NTxSize ofRem(double value) {
        return new NTxSize(Type.REM, value, 0);
    }

    public static NTxSize ofBounds(double width, double height) {
        return new NTxSize(Type.PAGE, width, height);
    }

    public static NTxSize ofJava(double size) {
        return new NTxSize(Type.PAGE, size, 0);
    }

    private NTxSize(Type type, double value, double value2) {
        this.type = type;
        this.value = value;
        this.value2 = value2;
    }

    public Type type() {
        return type;
    }

    public double value() {
        return value;
    }
    public double percent() {
        switch (type) {
            case PARENT:
            case PAGE:
                return value;
        }
        throw new IllegalStateException("not a percent");
    }

    public double size() {
        switch (type) {
            case PX:
            case REM:
                return value;
        }
        throw new IllegalStateException("not a size");
    }

    public double width() {
        switch (type) {
            case BOUNDS:
                return value;
        }
        throw new IllegalStateException("not a width");
    }

    public double height() {
        switch (type) {
            case BOUNDS:
                return value2;
        }
        throw new IllegalStateException("not a height");
    }

    @Override
    public NElement toElement() {
        switch (type) {
            case PARENT:
                return NElement.ofNumber(value + "%");
            case PAGE:
                return NElement.ofNumber(value + "%P");
            case REM:
                return NElement.ofNumber(value + "rem");
            case BOUNDS:
                return NElement.ofTuple(NElement.ofDouble(value), NElement.ofDouble(value2));
            case PX:
                return NElement.ofDouble(value, "px");
        }
        throw new IllegalStateException("not a size");
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        NTxSize nTxSize = (NTxSize) object;
        return Double.compare(value, nTxSize.value) == 0 && Double.compare(value2, nTxSize.value2) == 0 && type == nTxSize.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, value2);
    }
}
