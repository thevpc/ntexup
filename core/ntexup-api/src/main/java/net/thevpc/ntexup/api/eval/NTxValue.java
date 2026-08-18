package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.NTxArrowType;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.DefaultNTxColorPalette;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.pipeline.NStream;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.awt.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.List;

public class NTxValue {

    private Object element;
    private String name;
    private List<NElement> args = new ArrayList<>();
    private List<NElement> children = new ArrayList<>();

    private static NOptional<Color> getRegisteredColor(String name) {
        return NColor.ofName(name).map(x->new Color(x.rgb())).withMessage(() -> NMsg.ofC("color %s", name));
    }

    private boolean parsedChildren;

    public static NTxValue ofProp(NTxNode n, String name) {
        if (n == null) {
            return of(null);
        }
        return of(n.getPropertyValue(name).orNull());
    }

    public static NTxValue of(Object element) {
        if (element instanceof NTxValue) {
            return (NTxValue) element;
        }
        if (element instanceof NOptional) {
            return of(((NOptional) element).orNull());
        }
        return new NTxValue(element);
    }

    public NTxValue(Object element) {
        if (element instanceof NListContainerElement) {
            element = ((NListContainerElement) element);
        }
        this.element = element;
    }

    private void _parsedChildren() {
        if (!parsedChildren) {
            parsedChildren = true;
            if (element instanceof NListContainerElement) {
                switch (((NListContainerElement) element).type()) {
                    case OBJECT:
                    case FULL_OBJECT:
                    case PARAM_OBJECT:
                    case NAMED_OBJECT:

                    case ARRAY:
                    case FULL_ARRAY:
                    case PARAM_ARRAY:
                    case NAMED_ARRAY: {
                        NListContainerElement te = (NListContainerElement) element;
                        name = NTxUtils.uid(te.toNamed().flatMap(NNamedElement::name).orNull());
                        List<NElement> a = te.children();
                        if (a != null) {
                            children.addAll(a);
                        }
                        break;
                    }

                    case TUPLE:
                    case NAMED_TUPLE: {
                        NListContainerElement te = (NListContainerElement) element;
                        name = NTxUtils.uid(te.toNamed().flatMap(NNamedElement::name).orNull());
                        List<NElement> a = te.children();
                        if (a != null) {
                            args.addAll(a);
                        }
                        break;
                    }
                }
            }

        }
    }

    public NOptional<SimplePair> asSimplePair() {
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            switch (te.type()) {
                case PAIR: {
                    NPairElement pair = te.asPair().get();
                    NElement key = pair.key();
                    NOptional<String> s = NTxValue.of(key).asStringOrName();
                    if (s.isPresent()) {
                        return NOptional.of(
                                new SimplePair(
                                        s.get(),
                                        key,
                                        NTxValue.of(pair.value())
                                )
                        );
                    }
                    break;
                }
            }
        }
        return NOptional.ofNamedEmpty("pair");
    }

    public String name() {
        _parsedChildren();
        return name;
    }

    public List<NElement> argsOrBody() {
        List<NElement> a = new ArrayList<>();
        a.addAll(args());
        a.addAll(body());
        return a;
    }

    public Map<String, NTxValue> argsOrBodyMap() {
        Map<String, NTxValue> a = new HashMap<>();
        for (NElement arg : args()) {
            NOptional<SimplePair> sp = NTxValue.of(arg).asSimplePair();
            if (sp.isPresent()) {
                a.put(NTxUtils.uid(sp.get().name), sp.get().value);
            }
        }
        for (NElement arg : body()) {
            NOptional<SimplePair> sp = NTxValue.of(arg).asSimplePair();
            if (sp.isPresent()) {
                a.put(NTxUtils.uid(sp.get().name), sp.get().value);
            }
        }
        return a;
    }

    public Map<String, NTxValue> argsMap() {
        Map<String, NTxValue> a = new HashMap<>();
        for (NElement arg : args()) {
            NOptional<SimplePair> sp = NTxValue.of(arg).asSimplePair();
            if (sp.isPresent()) {
                a.put(NTxUtils.uid(sp.get().name), sp.get().value);
            }
        }
        return a;
    }

    public List<NElement> args() {
        _parsedChildren();
        return args;
    }

    public List<NElement> body() {
        _parsedChildren();
        return children;
    }

    public NOptional<NTxBounds2D> asBounds2() {
        if(element instanceof NTxBounds2D) {
            return  NOptional.of((NTxBounds2D) element);
        }
        NOptional<double[]> d = asDoubleArrayOrDouble();
        if(d.isPresent()) {
            double[] arr = d.get();
            if(arr.length==4){
                return NOptional.of(NTxBounds2D.ofWidth(arr[0], arr[1], arr[2], arr[3]));
            }
        }
        return NOptional.ofNamedEmpty("Bounds2 from " + element);
    }

    public NOptional<Paint> asPaint() {
        if (element instanceof Paint) {
            return NOptional.of((Paint) element);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            switch (te.type()) {
                case CUSTOM:{
                    Object v = te.asCustom().get().value();
                    if(v instanceof Paint) {
                        return NOptional.of((Paint) v);
                    }
                    return NOptional.ofNamedEmpty("paint from " + element);
                }
                case BYTE:
                case BIG_INT:
                case SHORT:
                case LONG:
                case INT: {
                    return NTxValue.of(te.asIntValue().get()).asPaint();
                }
                case TUPLE:
                case NAMED_TUPLE: {
                    NOptional<int[]> ri = asIntArray();
                    if (ri.isPresent()) {
                        int[] ints = ri.get();
                        if (ints.length == 3 || ints.length == 4) {
                            return NOptional.of(
                                    new Color(
                                            ints[0],
                                            ints[1],
                                            ints[2],
                                            ints.length == 3 ? 255 : ints[3]
                                    )
                            );
                        }
                    }
                    NOptional<float[]> rd = asFloatArray();
                    if (rd.isPresent()) {
                        float[] ints = rd.get();
                        if (ints.length == 3 || ints.length == 4) {
                            return NOptional.of(
                                    new Color(
                                            ints[0],
                                            ints[1],
                                            ints[2],
                                            ints.length == 3 ? 1.0f : ints[3]
                                    )
                            );
                        }
                    }
                    break;
                }
                case DOUBLE_QUOTED_STRING:
                case SINGLE_QUOTED_STRING:
                case BACKTICK_STRING:
                case TRIPLE_DOUBLE_QUOTED_STRING:
                case TRIPLE_SINGLE_QUOTED_STRING:
                case TRIPLE_BACKTICK_STRING:
                case LINE_STRING:
                case BLOCK_STRING:
                case NAME: {
                    NTxValue h = NTxValue.of(element);
                    String s = h.asStringOrName().get();
                    return NTxValue.of(s).asPaint();
                }
            }
        } else {
            if (element instanceof Integer
                    || element instanceof Short
                    || element instanceof Byte
                    || element instanceof Long
                    || element instanceof BigInteger) {
                return NOptional.of(new Color(((Number) element).intValue()));
            }
            if (element instanceof String) {
                String s = (String) element;
                if (s.startsWith("#")) {
                    try {
                        Color value = new Color(Integer.parseInt(s.substring(1), 16));
                        return NOptional.of(value);
                    } catch (Exception ex) {
                        return NOptional.ofNamedError(NMsg.ofC("invalid color %s", s));
                    }
                }
                if (s.indexOf(",") >= 0) {
                    String[] a = s.split(",");
                    if (a.length == 3 || a.length == 4) {
                        NTxValue r = NTxValue.of(a[0]);
                        NTxValue g = NTxValue.of(a[1]);
                        NTxValue b = NTxValue.of(a[2]);
                        NTxValue aa = NTxValue.of(a.length == 4 ? a[3] : null);
                        if (r.asInt().isPresent() && g.asInt().isPresent() && b.asInt().isPresent()) {
                            return NOptional.of(
                                    new Color(
                                            r.asInt().get(),
                                            g.asInt().get(),
                                            b.asInt().get(),
                                            aa.asInt().orElse(255)
                                    )
                            );
                        }
                        if (r.asDouble().isPresent() && g.asDouble().isPresent() && b.asDouble().isPresent()) {
                            return NOptional.of(
                                    new Color(
                                            (r.asFloat().get()),
                                            (g.asFloat().get()),
                                            (b.asFloat().get()),
                                            aa.asFloat().orElse(1.0f)
                                    )
                            );
                        }
                    }
                }
                if (s.toLowerCase().startsWith("c")) {
                    NOptional<Integer> u = NLiteral.of(s.substring(1).trim()).asInt();
                    if (u.isPresent()) {
                        return NOptional.of(DefaultNTxColorPalette.INSTANCE.getColor(u.get()));
                    }
                }
                NOptional<Color> color = getRegisteredColor(s);
                if (color.isPresent()) {
                    return NOptional.of(color.get());
                }
                NOptional<NColor> nc = NColor.ofName(s);
                if(nc.isPresent()) {
                    return NOptional.of(new Color(nc.get().rgb()));
                }
                try {
                    int z = Integer.parseInt(s, 16);
                    return NOptional.of(new Color(z));
                } catch (Exception e) {

                }
                try {
                    int z = Integer.parseInt(s);
                    return NOptional.of(new Color(z));
                } catch (Exception e) {
                    //
                }
            }
        }
        return NOptional.ofNamedEmpty("color from " + element);
    }

    public NOptional<Color> asColor() {
        if (element instanceof Color) {
            return NOptional.of((Color) element);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            switch (te.type()) {
                case BYTE:
                case BIG_INT:
                case SHORT:
                case LONG:
                case INT: {
                    return NTxValue.of(te.asIntValue().get()).asColor();
                }
                case TUPLE:
                case NAMED_TUPLE: {
                    NOptional<int[]> ri = asIntArray();
                    if (ri.isPresent()) {
                        int[] ints = ri.get();
                        if (ints.length == 3 || ints.length == 4) {
                            return NOptional.of(
                                    new Color(
                                            ints[0],
                                            ints[1],
                                            ints[2],
                                            ints.length == 3 ? 255 : ints[3]
                                    )
                            );
                        }
                    }
                    NOptional<float[]> rd = asFloatArray();
                    if (rd.isPresent()) {
                        float[] ints = rd.get();
                        if (ints.length == 3 || ints.length == 4) {
                            return NOptional.of(
                                    new Color(
                                            ints[0],
                                            ints[1],
                                            ints[2],
                                            ints.length == 3 ? 1.0f : ints[3]
                                    )
                            );
                        }
                    }
                    break;
                }
                case DOUBLE_QUOTED_STRING:
                case SINGLE_QUOTED_STRING:
                case BACKTICK_STRING:
                case TRIPLE_DOUBLE_QUOTED_STRING:
                case TRIPLE_SINGLE_QUOTED_STRING:
                case TRIPLE_BACKTICK_STRING:
                case LINE_STRING:
                case BLOCK_STRING:
                case NAME: {
                    NTxValue h = NTxValue.of(element);
                    String s = h.asStringOrName().get();
                    return NTxValue.of(s).asColor();
                }
            }
        } else {
            if (element instanceof Integer
                    || element instanceof Short
                    || element instanceof Byte
                    || element instanceof Long
                    || element instanceof BigInteger) {
                return NOptional.of(new Color(((Number) element).intValue()));
            }
            if (element instanceof String) {
                String s = (String) element;
                if (s.startsWith("#")) {
                    try {
                        Color value = new Color(Integer.parseInt(s.substring(1), 16));
                        return NOptional.of(value);
                    } catch (Exception ex) {
                        return NOptional.ofNamedError(NMsg.ofC("invalid color %s", s));
                    }
                }
                if (s.indexOf(",") >= 0) {
                    String[] a = s.split(",");
                    if (a.length == 3 || a.length == 4) {
                        NTxValue r = NTxValue.of(a[0]);
                        NTxValue g = NTxValue.of(a[1]);
                        NTxValue b = NTxValue.of(a[2]);
                        NTxValue aa = NTxValue.of(a.length == 4 ? a[3] : null);
                        if (r.asInt().isPresent() && g.asInt().isPresent() && b.asInt().isPresent()) {
                            return NOptional.of(
                                    new Color(
                                            r.asInt().get(),
                                            g.asInt().get(),
                                            b.asInt().get(),
                                            aa.asInt().orElse(255)
                                    )
                            );
                        }
                        if (r.asDouble().isPresent() && g.asDouble().isPresent() && b.asDouble().isPresent()) {
                            return NOptional.of(
                                    new Color(
                                            (r.asFloat().get()),
                                            (g.asFloat().get()),
                                            (b.asFloat().get()),
                                            aa.asFloat().orElse(1.0f)
                                    )
                            );
                        }
                    }
                }
                if (s.toLowerCase().startsWith("c")) {
                    NOptional<Integer> u = NLiteral.of(s.substring(1).trim()).asInt();
                    if (u.isPresent()) {
                        return NOptional.of(DefaultNTxColorPalette.INSTANCE.getColor(u.get()));
                    }
                }
                NOptional<Color> color = getRegisteredColor(s);
                if (color.isPresent()) {
                    return color;
                }
                NOptional<NColor> nc = NColor.ofName(s);
                if(nc.isPresent()) {
                    return NOptional.of(new Color(nc.get().rgb()));
                }
                try {
                    int z = Integer.parseInt(s, 16);
                    return NOptional.of(new Color(z));
                } catch (Exception e) {

                }
                try {
                    int z = Integer.parseInt(s);
                    return NOptional.of(new Color(z));
                } catch (Exception e) {
                    //
                }
            }
        }
        return NOptional.ofNamedEmpty("color from " + element);
    }

    public NOptional<Float> asFloat() {
        return asDouble().map(Double::floatValue);
    }

    public NOptional<Number> asNumber() {
        if (element instanceof Number) {
            return NOptional.of(((Number) element));
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            te = simplifyContainer(te);
            if (te.type().isAnyNumber()) {
                return NOptional.of(te.asNumberValue().get());
            } else if (te.isAnyString()) {
                return (NOptional) NLiteral.of(((NElement) element).asStringValue().get()).asDouble();
            }
        }
        if (element instanceof String) {
            return (NOptional) NLiteral.of(element).asDouble();
        }
        return NOptional.ofNamedEmpty("number from " + element);
    }

    public NOptional<Double> asDouble() {
        if (element instanceof Double
                || element instanceof Float
                || element instanceof BigDecimal) {
            return NOptional.of(((Number) element).doubleValue());
        }
        if (element instanceof String) {
            return NLiteral.of(element).asDouble();
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.type().isAnyNumber()) {
                switch (te.type()) {
                    case BYTE:
                    case INT:
                    case SHORT:
                    case LONG:
                    case FLOAT:
                    case DOUBLE:
                    case BIG_DECIMAL: {
                        return NOptional.of(te.asDoubleValue().get());
                    }
                }
                return NOptional.of(te.asDoubleValue().get());
            } else if (te.type().isAnyString()) {
                return NTxValue.of(te.asStringValue().get()).asDouble();
            } else {
                return NOptional.ofNamedEmpty("double from " + element);
            }
        }
        return NOptional.ofNamedEmpty("double from " + element);
    }

    public NOptional<Double> asDoubleOrNumber() {
        if (element instanceof Number) {
            return NOptional.of(((Number) element).doubleValue());
        }
        if (element instanceof String) {
            return NLiteral.of(element).asDouble();
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.type().isAnyNumber()) {
                switch (te.type()) {
                    case BYTE:
                    case INT:
                    case SHORT:
                    case LONG:
                    case FLOAT:
                    case DOUBLE:
                    case BIG_DECIMAL: {
                        return NOptional.of(te.asDoubleValue().get());
                    }
                }
                return NOptional.of(te.asDoubleValue().get());
            } else if (te.type().isAnyString()) {
                return NTxValue.of(te.asStringValue().get()).asDouble();
            } else {
                return NOptional.ofNamedEmpty("double from " + element);
            }
        }
        return NOptional.ofNamedEmpty("double from " + element);
    }

    public NOptional<Integer> asIntOrBoolean() {
        return asInt().orElseGetOptionalFrom(() -> asBoolean().map(x -> x ? 1 : 0));
    }

    public NOptional<NElement> asElementInt() {
        return asInt().map(x -> NElement.ofInt(x));
    }

    public NOptional<Integer> asInt() {
        if (element instanceof Integer
                || element instanceof Long
                || element instanceof Byte
                || element instanceof Short
                || element instanceof BigInteger) {
            return NOptional.of(((Number) element).intValue());
        }
        if (element instanceof String) {
            return NLiteral.of(element).asInt();
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.type().isAnyNumber()) {
                switch (te.type()) {
                    case BYTE:
                    case SHORT:
                    case INT:
                    case LONG: {
                        return NOptional.of(te.asIntValue().get());
                    }
                }
            } else if (te.type().isAnyString()) {
                return NTxValue.of(te.asStringValue().get()).asInt();
            } else {
                return NOptional.ofNamedEmpty("double from " + element);
            }
        }
        return NOptional.ofNamedEmpty("double from " + element);
    }

    public NOptional<Boolean> asBoolean() {
        if (element instanceof Boolean) {
            return NOptional.of(((Boolean) element));
        }
        if (element instanceof String) {
            return NLiteral.of(element).asBoolean();
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            switch (te.type()) {
                case BOOLEAN: {
                    return NOptional.of(te.asBooleanValue().get());
                }
                case DOUBLE_QUOTED_STRING:
                case SINGLE_QUOTED_STRING:
                case BACKTICK_STRING:
                case TRIPLE_DOUBLE_QUOTED_STRING:
                case TRIPLE_SINGLE_QUOTED_STRING:
                case TRIPLE_BACKTICK_STRING:
                case LINE_STRING:
                case BLOCK_STRING:
                {
                    return NLiteral.of(te.asStringValue()).asBoolean();
                }
                case NAME: {
                    return NLiteral.of(te.asStringValue().get()).asBoolean();
                }
            }
        }
        return NOptional.ofNamedEmpty("boolean from " + element);
    }

    public NOptional<NElement> asElementStringOrName() {
        if (element instanceof String) {
            return NOptional.of(NElement.ofString((String) element));
        }
        if (element instanceof NElement) {
            if (((NElement) element).isAnyString()) {
                return NOptional.of((NElement) element);
            }
        }
        return NOptional.ofNamedEmpty("string from " + element);
    }

    public NOptional<String> asStringOrName() {
        if (element instanceof String) {
            return NOptional.of((String) element);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isAnyString()) {
                return NOptional.of(te.asStringValue().get());
            }
        }
        return NOptional.ofNamedEmpty("string from " + element);
    }

    public NOptional<String> asString() {
        if (element instanceof String) {
            return NOptional.of((String) element);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isString()) {
                return NOptional.of(te.asStringValue().get());
            }
            if (te.isName()) {
                return NOptional.of(te.asStringValue().get());
            }
        }
        return NOptional.ofNamedEmpty("string from " + element);
    }

    public NOptional<int[]> asIntArray() {
        if (element instanceof int[]) {
            return NOptional.of((int[]) element);
        }
        if (element instanceof NElement[]) {
            NElement[] arr = (NElement[]) element;
            int[] aa = new int[arr.length];
            for (int i = 0; i < aa.length; i++) {
                NOptional<Integer> d = NTxValue.of(arr[i]).asInt();
                if (d.isPresent()) {
                    aa[i] = d.get();
                } else {
                    return NOptional.ofNamedEmpty("int[] from " + element);
                }
            }
            return NOptional.of(aa);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                return NTxValue.of(te.toListContainer().get().children().toArray(new NElement[0])).asIntArray();
            }
        }
        if (element instanceof NTxInt2) {
            return NOptional.of(new int[]{((NTxInt2) element).getY(), ((NTxInt2) element).getY()});
        }
        return NOptional.ofNamedEmpty("int[] from " + element);
    }

    public NOptional<double[]> asDoubleArrayOrDouble() {
        NOptional<double[]> a = asDoubleArray();
        if (a.isPresent()) {
            return a;
        }
        NOptional<Double> b = asDouble();
        if (b.isPresent()) {
            return NOptional.of(new double[]{b.get()});
        }
        return a;
    }

    public NOptional<float[]> asFloatArrayOrFloat() {
        NOptional<float[]> a = asFloatArray();
        if (a.isPresent()) {
            return a;
        }
        NOptional<Float> b = asFloat();
        if (b.isPresent()) {
            return NOptional.of(new float[]{b.get()});
        }
        return a;
    }

    public NOptional<float[]> asFloatArray() {
        return asDoubleArray().map(x -> {
            float[] ff = new float[x.length];
            for (int i = 0; i < x.length; i++) {
                ff[i] = (float) x[i];
            }
            return ff;
        });
    }

    public NOptional<NElement[]> asElementArray() {
        if (element instanceof NElement[]) {
            return NOptional.of(((NElement[]) element));
        }
        if (element instanceof double[]) {
            return NOptional.of(NElement.ofDoubleArray((double[]) element).children().toArray(new NElement[0]));
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            switch (te.type()) {
                case ARRAY:
                case FULL_ARRAY:
                case PARAM_ARRAY:
                case NAMED_ARRAY: {
                    NArrayElement a = te.asArray().get();
                    if (a.isNamed() || a.isParametrized()) {
                        return NOptional.of(new NElement[]{te});
                    }
                    return NOptional.of(te.asArray().get().children().toArray(new NElement[0]));
                }
                case OBJECT:
                case FULL_OBJECT:
                case NAMED_OBJECT:
                case PARAM_OBJECT: {
                    NObjectElement a = te.asObject().get();
                    if (a.isNamed() || a.isParametrized()) {
                        return NOptional.of(new NElement[]{te});
                    }
                    return NOptional.of(te.asObject().get().children().toArray(new NElement[0]));
                }
                case TUPLE:
                case NAMED_TUPLE: {
                    return NOptional.of(te.asTuple().get().children().toArray(new NElement[0]));
                }
            }
        }
        if (element instanceof NTxDouble2) {
            return NTxValue.of(new double[]{((NTxDouble2) element).getX(), ((NTxDouble2) element).getY()}).asElementArray();
        }
        if (element instanceof NTxPoint2D) {
            return NTxValue.of(new double[]{((NTxPoint2D) element).getX(), ((NTxPoint2D) element).getY()}).asElementArray();
        }
//        if (element instanceof NTxPoint3D) {
//            return NTxValue.of(new double[]{((NTxPoint3D) element).getX(), ((NTxPoint3D) element).getY(), ((NTxPoint3D) element).getZ()}).asElementArray();
//        }
        if (element instanceof NTxDouble4) {
            return NTxValue.of(new double[]{
                    ((NTxDouble4) element).getX1(),
                    ((NTxDouble4) element).getX2()
                    , ((NTxDouble4) element).getX3()
                    , ((NTxDouble4) element).getX4()
            }).asElementArray();
        }
        return NOptional.ofNamedEmpty("NElement[] from " + element);
    }

    public NOptional<double[]> asDoubleArray() {
        if (element instanceof double[]) {
            return NOptional.of((double[]) element);
        }
        if (element instanceof Object[]) {
            Object[] arr = (Object[]) element;
            double[] aa = new double[arr.length];
            for (int i = 0; i < aa.length; i++) {
                NOptional<Double> d = NTxValue.of(arr[i]).asDouble();
                if (d.isPresent()) {
                    aa[i] = d.get();
                } else {
                    return NOptional.ofNamedEmpty("double[] from " + element);
                }
            }
            return NOptional.of(aa);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                return NTxValue.of(te.asListContainer().get().children().toArray(new NElement[0])).asDoubleArray();
            }
        }
        if (element instanceof NTxDouble2) {
            return NOptional.of(new double[]{((NTxDouble2) element).getX(), ((NTxDouble2) element).getY()});
        }
        if (element instanceof NTxPoint2D) {
            return NOptional.of(new double[]{((NTxPoint2D) element).getY(), ((NTxPoint2D) element).getY()});
        }
//        if (element instanceof NTxPoint3D) {
//            return NOptional.of(new double[]{((NTxPoint3D) element).getY(), ((NTxPoint3D) element).getY(), ((NTxPoint3D) element).getZ()});
//        }
        if (element instanceof NTxDouble4) {
            NTxDouble4 d = (NTxDouble4) element;
            return NOptional.of(new double[]{
                    d.getX1(),
                    d.getX2(),
                    d.getX3(),
                    d.getX4(),});
        }
        if (element instanceof Collection) {
            Object[] arr = ((Collection) element).toArray();
            double[] aa = new double[arr.length];
            for (int i = 0; i < aa.length; i++) {
                NOptional<Double> d = NTxValue.of(arr[i]).asDouble();
                if (d.isPresent()) {
                    aa[i] = d.get();
                } else {
                    return NOptional.ofNamedEmpty("double[] from " + element);
                }
            }
            return NOptional.of(aa);
        }
        return NOptional.ofNamedEmpty("double[] from " + element);
    }

    public NOptional<String[]> asStringArrayOrString() {
        NOptional<String[]> a = asStringArray();
        if (a.isPresent()) {
            return a;
        }
        NOptional<String> b = asStringOrName();
        if (b.isPresent()) {
            return NOptional.of(new String[]{b.get()});
        }
        return a;
    }

    public NOptional<Object[]> asObjectArray() {
        if (element instanceof Object[]) {
            return NOptional.of((Object[]) element);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                return NOptional.of(te.asListContainer().get().children().toArray(new NElement[0]));
            }
        }
        return NOptional.ofNamedEmpty("Object[] from " + element);
    }

    public NOptional<Color[]> asColorArrayOrColor() {
        NOptional<Color[]> a = asColorArray();
        if (a.isPresent()) {
            return a;
        }
        NOptional<Color> b = asColor();
        if (b.isPresent()) {
            return NOptional.of(new Color[]{b.get()});
        }
        return a;
    }

    public NOptional<Color[]> asColorArray() {
        NOptional<Object[]> o = asObjectArray();
        if (o.isPresent()) {
            List<Color> cc = new ArrayList<>();
            for (Object oi : o.get()) {
                NOptional<Color> y = NTxValue.of(oi).asColor();
                if (y.isPresent()) {
                    cc.add(y.get());
                } else {
                    return NOptional.ofNamedEmpty("Object[] from " + element);
                }
            }
            return NOptional.of(cc.toArray(new Color[0]));
        }
        return NOptional.ofNamedEmpty("Object[] from " + element);
    }

    public NOptional<String[]> asStringArray() {
        if (element instanceof String[]) {
            return NOptional.of((String[]) element);
        }
        if (element instanceof NElement[]) {
            NElement[] arr = (NElement[]) element;
            String[] aa = new String[arr.length];
            for (int i = 0; i < aa.length; i++) {
                NOptional<String> d = NTxValue.of(arr[i]).asStringOrName();
                if (d.isPresent()) {
                    aa[i] = d.get();
                } else {
                    return NOptional.ofNamedEmpty("double[] from " + element);
                }
            }
            return NOptional.of(aa);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                return NOptional.of(te.asListContainer().get().children().stream().map(x -> x.asStringValue().get()).toArray(String[]::new));
            }
        }
        return NOptional.ofNamedEmpty("String[] from " + element);
    }

    public NOptional<boolean[]> asBooleanArray() {
        if (element instanceof boolean[]) {
            return NOptional.of((boolean[]) element);
        }
        if (element instanceof NElement[]) {
            NElement[] arr = (NElement[]) element;
            boolean[] aa = new boolean[arr.length];
            for (int i = 0; i < aa.length; i++) {
                NOptional<Boolean> d = NTxValue.of(arr[i]).asBoolean();
                if (d.isPresent()) {
                    aa[i] = d.get();
                } else {
                    return NOptional.ofNamedEmpty("double[] from " + element);
                }
            }
            return NOptional.of(aa);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                return NOptional.of(NStream.ofStream(te.asListContainer().get().children().stream()).map(x -> x.asBooleanValue().get()).toBooleanArray());
            }
        }
        return NOptional.ofNamedEmpty("boolean[] from " + element);
    }

    public NOptional<NTxInt2> asInt2() {
        if (element instanceof NTxInt2) {
            return NOptional.of((NTxInt2) element);
        }
        NOptional<int[]> a = asIntArray();
        if (a.isPresent()) {
            int[] g = a.get();
            if (g.length == 2) {
                return NOptional.of(new NTxInt2(g[0], g[1]));
            }
        }
        return NOptional.ofNamedEmpty("int2 from " + element);
    }

    public NOptional<NTxDouble2> asDouble2OrHAlign() {
        NOptional<NTxDouble2> p = asDouble2();
        if (p.isPresent()) {
            return p;
        }
        if (element instanceof NTxAlign) {
            return ((NTxAlign) element).toPosition();
        }
        NOptional<String> k = asStringOrName();
        if (k.isPresent()) {
            return NTxAlign.parse(k.get()).flatMap(x -> x.toPosition());
        }
        return NOptional.ofNamedEmpty("Double from " + element);
    }

    public NOptional<NTxElemNumber2> asNNumberElement2Or1OrHAlign() {
        NOptional<NElement[]> ta = asElementArray();
        if (ta.isPresent()) {
            NElement[] taa = ta.get();
            switch (taa.length) {
                case 1: {
                    if (taa[0].isNumber()) {
                        return NOptional.of(new NTxElemNumber2((NNumberElement) taa[0], (NNumberElement) taa[0]));
                    } else if (taa[0].isAnyString()) {
                        NTxDouble2 size = NTxAlign.parse(NTxValue.of(taa[0]).asStringOrName().get()).flatMap(x -> x.toPosition()).get();
                        return NOptional.of(
                                new NTxElemNumber2(
                                        NElement.ofDouble(size.getX()).asNumber().get(),
                                        NElement.ofDouble(size.getY()).asNumber().get()
                                )
                        );
                    }
                    break;
                }
                case 2: {
                    NNumberElement xx;
                    NNumberElement yy;
                    if (taa[0].isNumber()) {
                        xx = taa[0].asNumber().get();
                    } else if (taa[0].isAnyString()) {
                        xx = NElement.ofDouble(NTxAlign.parse(NTxValue.of(taa[0]).asStringOrName().get()).flatMap(NTxAlign::toPosition).get().getX()).asNumber().get();
                    } else {
                        return NOptional.ofNamedError(NMsg.ofC("not a number %s in %s", taa[0], element));
                    }
                    if (taa[1].isNumber()) {
                        yy = taa[1].asNumber().get();
                    } else if (taa[1].isAnyString()) {
                        yy = NElement.ofDouble(NTxAlign.parse(NTxValue.of(taa[1]).asStringOrName().get()).flatMap(x -> x.toPosition()).get().getY()).asNumber().get();
                    } else {
                        return NOptional.ofNamedError(NMsg.ofC("not a number %s in %s", taa[1], element));
                    }
                    return NOptional.of(new NTxElemNumber2(xx, yy));
                }
            }
        }
        NOptional<NElement> te = asElement();
        if (te.isPresent()) {
            NElement taa = te.get();
            if (taa.isNumber()) {
                return NOptional.of(new NTxElemNumber2((NNumberElement) taa, (NNumberElement) taa));
            } else if (taa.isAnyString()) {
                NTxDouble2 size = NTxAlign.parse(NTxValue.of(taa).asStringOrName().get()).flatMap(x -> x.toPosition()).get();
                return NOptional.of(
                        new NTxElemNumber2(
                                NElement.ofDouble(size.getX()).asNumber().get(),
                                NElement.ofDouble(size.getY()).asNumber().get()
                        )
                );
            }
        }
        return NOptional.ofNamedEmpty("NNumberElement2Or1OrHAlign from " + element);
    }


    public NOptional<NTxMargin> asPadding() {
        if (element instanceof NTxMargin) {
            return NOptional.of((NTxMargin) element);
        }
        NOptional<double[]> d = asDoubleArrayOrDouble();
        if (d.isPresent()) {
            double[] dd = d.get();
            switch (dd.length) {
                case 1: {
                    return NOptional.of(NTxMargin.of(dd[0]));
                }
                case 2: {
                    return NOptional.of(NTxMargin.of(dd[0], dd[1]));
                }
                case 3: {
                    return NOptional.of(NTxMargin.of(dd[0], dd[1], dd[2]));
                }
                case 4: {
                    return NOptional.of(NTxMargin.of(dd[0], dd[1], dd[2], dd[3]));
                }
            }
        }
        return NOptional.ofNamedEmpty("Padding from " + element);
    }

    public NOptional<NTxRotation> asRotation() {
        if (element instanceof NTxRotation) {
            return NOptional.of((NTxRotation) element);
        }
        NOptional<double[]> d = asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            switch (dd.length) {
                case 1: {
                    return NOptional.of(new NTxRotation(
                            NElement.ofDouble(dd[0]),
                            NElement.ofDouble(50),
                            NElement.ofDouble(50)
                    ));
                }
                case 3: {
                    return NOptional.of(new NTxRotation(
                            NElement.ofDouble(dd[0]),
                            NElement.ofDouble(50),
                            NElement.ofDouble(50)
                    ));
                }
            }
        }
        NOptional<NElement> dd = asElement();
        if (dd.isPresent()) {
            return NOptional.of(new NTxRotation(
                    dd.get(),
                    NElement.ofDouble(50),
                    NElement.ofDouble(50)
            ));
        }
        return NOptional.ofNamedEmpty("Rotation from " + element);
    }

    public NOptional<NTxDouble4> asDouble4() {
        if (element instanceof NTxDouble4) {
            return NOptional.of((NTxDouble4) element);
        }
        NOptional<double[]> d = asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 4) {
                return NOptional.of(new NTxDouble4(dd[0], dd[1], dd[2], dd[3]));
            }
        }
        return NOptional.ofNamedEmpty("Double4 from " + element);
    }

    public NOptional<NTxDouble2> asDouble2OrDouble() {
        if (element instanceof NTxDouble2) {
            return NOptional.of((NTxDouble2) element);
        }
        if (element instanceof NTxPoint2D) {
            NTxPoint2D p = (NTxPoint2D) element;
            return NOptional.of(new NTxDouble2(p.x, p.y));
        }
        NOptional<double[]> d = asDoubleArrayOrDouble();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 1) {
                return NOptional.of(new NTxDouble2(dd[0], dd[0]));
            }
            if (dd.length >= 2) {
                return NOptional.of(new NTxDouble2(dd[0], dd[1]));
            }
        }
        return NOptional.ofNamedEmpty("Double2 from " + element);
    }

    public NOptional<NTxDouble2> asDouble2() {
        if (element instanceof NTxDouble2) {
            return NOptional.of((NTxDouble2) element);
        }
        if (element instanceof NTxPoint2D) {
            NTxPoint2D p = (NTxPoint2D) element;
            return NOptional.of(new NTxDouble2(p.x, p.y));
        }
        NOptional<double[]> d = asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 2) {
                return NOptional.of(new NTxDouble2(dd[0], dd[1]));
            }
        }
        return NOptional.ofNamedEmpty("Double2 from " + element);
    }

    public NOptional<NTxDouble3> asDouble3() {
        if (element instanceof NTxDouble3) {
            return NOptional.of((NTxDouble3) element);
        }
//        if (element instanceof NTxPoint3D) {
//            NTxPoint3D p = (NTxPoint3D) element;
//            return NOptional.of(new NTxDouble3(p.x, p.y, p.z));
//        }
        NOptional<double[]> d = asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 3) {
                return NOptional.of(new NTxDouble3(dd[0], dd[1], dd[2]));
            }
        }
        return NOptional.ofNamedEmpty("Double3 from " + element);
    }

    public NOptional<NTxPoint2D> asPoint2DOrDouble() {
        if (element instanceof NTxPoint2D) {
            return NOptional.of((NTxPoint2D) element);
        }
        NOptional<double[]> d = asDoubleArrayOrDouble();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 2) {
                return NOptional.of(new NTxPoint2D(dd[0], dd[1]));
            }
            if (dd.length == 1) {
                return NOptional.of(new NTxPoint2D(dd[0], dd[0]));
            }
        }
        return NOptional.ofNamedEmpty("Point2D from " + element);
    }

    public NOptional<NTxPoint2D> asPoint2D() {
        if (element instanceof NTxPoint2D) {
            return NOptional.of((NTxPoint2D) element);
        }
        NOptional<double[]> d = asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 2) {
                return NOptional.of(new NTxPoint2D(dd[0], dd[1]));
            }
        }
        return NOptional.ofNamedEmpty("Point2D from " + element);
    }


    public NOptional<NTxArrowType> asArrowType() {
        if (element instanceof NTxArrowType) {
            return NOptional.of((NTxArrowType) element);
        }
        NOptional<String> s = asStringOrName();
        if (s.isPresent()) {
            String v = s.get().trim();
            if (v.isEmpty()) {
                return NOptional.ofNamedEmpty("arrow-type");
            }
            try {
                NTxArrowType y = NTxArrowType.valueOf(NNameFormat.CONST_NAME.format(v));
                return NOptional.of(y);
            } catch (Exception e) {
                //
            }
        }
        return NOptional.ofNamedError("HArrowType from " + element);
    }

    public NOptional<NTxArrow> asArrow() {
        if (element instanceof NTxArrow) {
            return NOptional.of((NTxArrow) element);
        }
        if (element instanceof NTxArrowType) {
            return NOptional.of(new NTxArrow((NTxArrowType) element));
        }
        if (element instanceof NTupleElement && ((NTupleElement) element).isNamed()) {
            NTupleElement f = (NTupleElement) element;
            NOptional<NTxArrowType> u = NTxValue.of(f.name().orNull()).asArrowType();
            Double width = null;
            Double height = null;
            if (u.isPresent()) {
                for (NElement arg : f.params()) {
                    NOptional<Number> n = NTxValue.of(arg).asNumber();
                    if (n.isPresent()) {
                        if (width == null) {
                            width = n.get().doubleValue();
                        } else if (height == null) {
                            height = n.get().doubleValue();
                        }
                    }
                }
                return NOptional.of(
                        new NTxArrow(
                                u.get(),
                                width == null ? 0 : width.doubleValue(),
                                height == null ? 0 : height.doubleValue()
                        )
                );
            }
            return NOptional.ofNamedError("HArrow from " + element);
        }
        NOptional<NTxArrowType> s = asArrowType();
        if (s.isPresent()) {
            return NOptional.of(new NTxArrow(s.get()));
        }
        return NOptional.ofNamedError("HArrow from " + element);
    }


    public <T> NOptional<T> as(Class<T> type) {
        switch (type.getName()) {
            case "java.lang.String": {
                return (NOptional<T>) asStringOrName();
            }
            case "boolean": {
                return (NOptional<T>) asBoolean();
            }
            case "double": {
                return (NOptional<T>) asDouble();
            }
            case "int": {
                return (NOptional<T>) asInt();
            }
            case "net.thevpc.ntexup.api.document.elem2d.NtxDouble2": {
                return (NOptional<T>) asDouble2();
            }
            case "[Lnet.thevpc.ntexup.api.document.elem2d.NtxDouble2;": {
                return (NOptional<T>) asDouble2Array();
            }
            case "[Ljava.lang.String;": {
                return (NOptional<T>) asStringArray();
            }
            case "[d": {
                return (NOptional<T>) asDoubleArray();
            }
            default: {
                throw new IllegalArgumentException("unsupported type " + type);
            }
        }
    }


    public NOptional<NTxPoint2D[]> asPoint2DArray() {
        NOptional<NTxDouble2[]> u = asDouble2Array();
        if (u.isPresent()) {
            return NOptional.of(
                    Arrays.stream(u.get()).map(x -> new NTxPoint2D(x.getX(), x.getY())).toArray(NTxPoint2D[]::new)
            );
        } else {
            return (NOptional) u;
        }
    }


    public NOptional<NTxDouble2[]> asDouble2Array() {
        if (element instanceof NTxDouble2[]) {
            return NOptional.of((NTxDouble2[]) element);
        }
        if (element instanceof NTxPoint2D[]) {
            return NOptional.of(
                    Arrays.stream((NTxPoint2D[]) element).map(x -> new NTxDouble2(x.getX(), x.getY())).toArray(NTxDouble2[]::new)
            );
        }
        if (element instanceof NElement[]) {
            NElement[] arr = (NElement[]) element;
            NTxDouble2[] aa = new NTxDouble2[arr.length];
            for (int i = 0; i < aa.length; i++) {
                NOptional<NTxDouble2> d = NTxValue.of(arr[i]).asDouble2();
                if (d.isPresent()) {
                    aa[i] = d.get();
                } else {
                    return NOptional.ofNamedEmpty("Double2[] from " + element);
                }
            }
            return NOptional.of(aa);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                return NTxValue.of(te.toArray().get().children().toArray(new NElement[0])).asDouble2Array();
            }
        }
        return NOptional.ofNamedEmpty("Double2[] from " + element);
    }

    public NOptional<NTxDouble3[]> asDouble3Array() {
        if (element instanceof NTxDouble3[]) {
            return NOptional.of((NTxDouble3[]) element);
        }
        if (element instanceof NElement[]) {
            NElement[] arr = (NElement[]) element;
            NTxDouble3[] aa = new NTxDouble3[arr.length];
            for (int i = 0; i < aa.length; i++) {
                NOptional<NTxDouble3> d = NTxValue.of(arr[i]).asDouble3();
                if (d.isPresent()) {
                    aa[i] = d.get();
                } else {
                    return NOptional.ofNamedEmpty("Double3[] from " + element);
                }
            }
            return NOptional.of(aa);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                return NTxValue.of(te.toArray().get().children().toArray(new NElement[0])).asDouble3Array();
            }
        }
        return NOptional.ofNamedEmpty("Double3[] from " + element);
    }

    public boolean isFunction() {
        return element instanceof NTupleElement && ((NTupleElement) element).type() == NElementType.NAMED_TUPLE;
    }

    public boolean hasName() {
        return !NBlankable.isBlank(name());
    }

    public boolean isPoint2() {
        if (element instanceof NElement) {
            NElement e = (NElement) element;
            e = simplifyContainer(e);
            if(e.isTuple()){
                NTupleElement u = e.asTuple().get();
                if(u.params().size()==2){
                    for (NElement param : u.params()) {
                        if(!NTxValue.of(param).isNumber()){
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isBoolean() {
        if (element instanceof Boolean) {
            return true;
        }
        if (element instanceof NElement) {
            NElement e = (NElement) element;
            e = simplifyContainer(e);
            return e.isBoolean();
        }
        return false;
    }

    public boolean isNumber() {
        if (element instanceof Number) {
            return true;
        }
        if (element instanceof NElement) {
            NElement e = (NElement) element;
            e = simplifyContainer(e);
            return e.isNumber();
        }
        return false;
    }

    public static NElement simplifyContainer(NElement e) {
        if (e.isTuple()) {
            NTupleElement u = e.asTuple().get();
            if (u.params().size() == 1) {
                return simplifyContainer(u.params().get(0));
            }
        }
        if (e.isArray()) {
            NArrayElement u = e.asArray().get();
            if (u.children().size() == 1) {
                return simplifyContainer(u.children().get(0));
            }
        }
        if (e.isObject()) {
            NObjectElement u = e.asObject().get();
            if (u.children().size() == 1) {
                return simplifyContainer(u.children().get(0));
            }
        }
        return e;
    }

    public  NOptional<NElement> asElement() {
        if (element instanceof NElement) {
            return NOptional.of((NElement) element);
        }
        if (element instanceof String) {
            return NOptional.of(NElement.ofString((String) element));
        }
        return NOptional.ofEmpty(NMsg.ofC("not a valid element : %s", element));
    }


    public NOptional<String> asName() {
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            switch (te.type()) {
                case NAME: {
                    return NOptional.of(te.asStringValue().get());
                }
            }
        }
        return NOptional.ofNamedEmpty("name from " + element);
    }

    public boolean isStringOrName() {
        if (element instanceof String) {
            return true;
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            return te.isAnyString();
        }
        return false;
    }

    public boolean isString() {
        if (element instanceof String) {
            return true;
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            return te.isString();
        }
        return false;
    }

    public static class SimplePair {

        private String name;
        private NElement key;
        private NTxValue value;

        public SimplePair(String name, NElement key, NTxValue value) {
            this.name = name;
            this.key = key;
            this.value = value;
        }

        public String getNameId() {
            return NTxUtils.uid(getName());
        }

        public String getName() {
            return name;
        }

        public NElement getKey() {
            return key;
        }

        public NTxValue getValue() {
            return value;
        }
    }

    public Object raw() {
        return element;
    }
}
