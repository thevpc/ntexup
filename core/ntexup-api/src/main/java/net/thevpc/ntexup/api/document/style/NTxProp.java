/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.util.NNameFormat;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author vpc
 */
public class NTxProp implements NTxItem, NToElement {

    private String name;
    private NElement value;
    private NTxItem parent;

    public static NTxProp ofDouble(String name, Double value) {
        return of(name, NElement.ofDouble(value));
    }

    public static NTxProp ofInt(String name, Integer value) {
        return of(name, NElement.ofInt(value));
    }

    public static NTxProp ofBoolean(String name, Boolean value) {
        return of(name, NElement.ofBoolean(value));
    }

    public static NTxProp ofString(String name, String value) {
        return of(name, NElement.ofString(value));
    }

    public static NTxProp ofObject(String name, NElement value) {
        return of(name, value);
    }

    public static NTxProp ofStringArray(String name, String[] value) {
        return of(name, NElement.ofStringArray(value));
    }

    public static NTxProp ofDouble2(String name, double x, double y) {
        return of(name, NElement.ofUplet(NElement.ofDouble(x), NElement.ofDouble(y)));
    }

    public static NTxProp ofHPoint2D(String name, double x, double y) {
        return of(name, NElement.ofUplet(NElement.ofDouble(x), NElement.ofDouble(y)));
    }

    public static NTxProp ofDouble2(String name, NTxDouble2 d) {
        return of(name, d == null ? null : d.toElement());
    }

    public static NTxProp ofHPoint2D(String name, NTxPoint2D d) {
        return of(name, d == null ? null : d.toElement());
    }


    public static NTxProp ofDouble2Array(String name, NTxDouble2... d) {
        return of(name, NElement.ofArray(Arrays.stream(d).map(NTxDouble2::toElement).toArray(NElement[]::new)));
    }

    public static NTxProp ofDoubleArray(String name, double[] d) {
        return of(name, NElement.ofDoubleArray(d));
    }

    public static NTxProp ofIntArray(String name, int[] d) {
        return of(name, NElement.ofIntArray(d));
    }

    public static NTxProp ofHPoint2DArray(String name, NTxPoint2D... d) {
        return of(name, NElement.ofArray(Arrays.stream(d).map(NTxPoint2D::toElement).toArray(NElement[]::new)));
    }


    public NTxProp(String name, NElement value, NTxItem parent) {
        this.name = name;
        this.value = value;
        this.parent = parent;
    }

    public NTxProp(String name, NToElement value, NTxItem parent) {
        this.name = name;
        this.value = value == null ? null : value.toElement();
        this.parent = parent;
    }

    public static NTxProp of(String name, NElement v) {
        return new NTxProp(name, v, null);
    }

    public static NTxProp of(String name, NToElement v) {
        return new NTxProp(name,(v==null?null: NTxUtils.addCompilerDeclarationPathDummy(v.toElement())), null);
    }

    @Override
    public NTxSource source() {
        return null;
    }

    @Override
    public NTxItem parent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public NElement getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.value);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NTxProp other = (NTxProp) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.value, other.value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(NNameFormat.LOWER_KEBAB_CASE.format(name));
        sb.append(":");
        Object vv = value;
        if (vv != null) {
            if (vv.getClass().isArray()) {
                Class<?> ct = vv.getClass().getComponentType();
                if (ct.isPrimitive()) {
                    switch (ct.getName()) {
                        case "double": {
                            sb.append(Arrays.toString((double[]) vv));
                            break;
                        }
                        case "int": {
                            sb.append(Arrays.toString((int[]) vv));
                            break;
                        }
                        case "long": {
                            sb.append(Arrays.toString((long[]) vv));
                            break;
                        }
                        case "boolean": {
                            sb.append(Arrays.toString((boolean[]) vv));
                            break;
                        }
                        default: {
                            sb.append(vv);
                        }
                    }
                } else {
                    sb.append(Arrays.asList((Object[]) vv));
                }
            } else {
                sb.append(vv);
            }
        } else {
            sb.append(vv);
        }
        return sb.toString();
    }

    public NElement toElement() {
        return NElement.ofPair(NTxUtils.toElement(name), NTxUtils.toElement(value));
    }
}
