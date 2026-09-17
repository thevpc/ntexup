package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.*;

public class NTxFunctionRelativeError implements NTxFunction {
    private static final Map<String, NTxObj> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public String name() {
        return "relativeError";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.checkTooFewArgs(2)) {
            return NElement.ofNull();
        }
        args.checkTooManyArgs(4);

        NElement a0 = args.arg(0).eval();
        NElement a1 = args.arg(1).eval();

        String mode = "rel";
        NElement customX = null;
        if (args.size() == 3) {
            NElement a2 = args.arg(2).eval();
            String s = NTxValue.of(a2).asString().orNull();
            if (s != null && (s.equalsIgnoreCase("rel") || s.equalsIgnoreCase("db") || s.equalsIgnoreCase("diff") || s.equalsIgnoreCase("percent"))) {
                mode = s.toLowerCase();
            } else {
                customX = a2;
            }
        } else if (args.size() >= 4) {
            customX = args.arg(2).eval();
            NElement a3 = args.arg(3).eval();
            String s = NTxValue.of(a3).asString().orNull();
            if (s != null) {
                mode = s.toLowerCase();
            }
        }

        final String finalMode = mode;
        final NElement finalCustomX = customX;

        String cacheKey = System.identityHashCode(a0) + ":" + System.identityHashCode(a1) + ":" + (finalCustomX == null ? "" : System.identityHashCode(finalCustomX)) + ":" + finalMode;
        NTxObj cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached.toElement();
        }

        boolean isFuture = NTxFutureUtils.isFuture(a0) || NTxFutureUtils.isFuture(a1)
                || (finalCustomX != null && NTxFutureUtils.isFuture(finalCustomX));

        if (isFuture) {
            List<Object> deps = new ArrayList<>();
            if (NTxFutureUtils.isFuture(a0)) deps.add(a0);
            if (NTxFutureUtils.isFuture(a1)) deps.add(a1);
            if (finalCustomX != null && NTxFutureUtils.isFuture(finalCustomX)) deps.add(finalCustomX);

            NTxCombinedFutureObj futureObj = new NTxCombinedFutureObj("relativeError", () -> {
                NElement ev0 = toElement(NTxFutureUtils.await(a0));
                NElement ev1 = toElement(NTxFutureUtils.await(a1));
                NElement evX = finalCustomX != null ? toElement(NTxFutureUtils.await(finalCustomX)) : null;
                return computeRelativeError(ev0, ev1, evX, finalMode);
            }, deps.toArray());
            CACHE.put(cacheKey, futureObj);
            return futureObj.toElement();
        }

        NTxObj res = computeRelativeError(a0, a1, finalCustomX, finalMode);
        if (res != null) {
            CACHE.put(cacheKey, res);
            return res.toElement();
        }
        return NElement.ofNull();
    }

    private static NElement toElement(Object obj) {
        if (obj == null) return NElement.ofNull();
        if (obj instanceof NElement) return (NElement) obj;
        if (obj instanceof NTxObj) return ((NTxObj) obj).toElement();
        return NElement.ofNull();
    }

    public static NTxObj computeRelativeError(NElement a0, NElement a1, NElement customX, String mode) {
        double[] x0 = toDoubleArray(getProperty(a0, "x"));
        double[] y0 = toDoubleArray(getProperty(a0, "y"));
        if (y0 == null) y0 = toDoubleArray(getProperty(a0, "mag"));
        if (y0 == null) y0 = toDoubleArray(getProperty(a0, "abs"));
        if (y0 == null) y0 = toDoubleArray(a0);
        double[] db0 = toDoubleArray(getProperty(a0, "db"));

        double[] x1 = toDoubleArray(getProperty(a1, "x"));
        double[] y1 = toDoubleArray(getProperty(a1, "y"));
        if (y1 == null) y1 = toDoubleArray(getProperty(a1, "mag"));
        if (y1 == null) y1 = toDoubleArray(getProperty(a1, "abs"));
        if (y1 == null) y1 = toDoubleArray(a1);
        double[] db1 = toDoubleArray(getProperty(a1, "db"));

        if (y0 == null || y1 == null || y0.length == 0 || y1.length == 0) {
            return NTxObjs.map();
        }

        int n = Math.min(y0.length, y1.length);
        double[] x = null;
        if (customX != null) {
            x = toDoubleArray(customX);
        }
        if (x == null && x0 != null && x0.length >= n) {
            x = x0;
        } else if (x == null && x1 != null && x1.length >= n) {
            x = x1;
        }
        if (x == null) {
            x = new double[n];
            for (int i = 0; i < n; i++) x[i] = i;
        }

        double[] rel = new double[n];
        double[] diff = new double[n];
        double[] diffDb = new double[n];

        double maxRel = -Double.MAX_VALUE, minRel = Double.MAX_VALUE, sumRel = 0;
        double maxDb = -Double.MAX_VALUE, minDb = Double.MAX_VALUE, sumDb = 0;

        for (int i = 0; i < n; i++) {
            double v0 = y0[i];
            double v1 = y1[i];
            double d = Math.abs(v0 - v1);
            diff[i] = d;

            double denom = Math.max(Math.abs(v1), 1e-6);
            double r = (d / denom) * 100.0;
            rel[i] = r;
            sumRel += r;
            if (r > maxRel) maxRel = r;
            if (r < minRel) minRel = r;

            double d0 = db0 != null && i < db0.length ? db0[i] : 20.0 * Math.log10(Math.max(1e-12, Math.abs(v0)));
            double d1 = db1 != null && i < db1.length ? db1[i] : 20.0 * Math.log10(Math.max(1e-12, Math.abs(v1)));
            double dDb = Math.abs(d0 - d1);
            diffDb[i] = dDb;
            sumDb += dDb;
            if (dDb > maxDb) maxDb = dDb;
            if (dDb < minDb) minDb = dDb;
        }

        double meanRel = n > 0 ? sumRel / n : 0.0;
        double meanDb = n > 0 ? sumDb / n : 0.0;
        if (minRel == Double.MAX_VALUE) minRel = 0.0;
        if (maxRel == -Double.MAX_VALUE) maxRel = 0.0;
        if (minDb == Double.MAX_VALUE) minDb = 0.0;
        if (maxDb == -Double.MAX_VALUE) maxDb = 0.0;

        double[] plotY = "db".equalsIgnoreCase(mode) ? diffDb : ("diff".equalsIgnoreCase(mode) ? diff : rel);

        NTxObjFromMap map = NTxObjs.map();
        map.set("x", NTxObjs.elem(NElement.ofArray(Arrays.stream(x).limit(n).mapToObj(NElement::ofDouble).toArray(NElement[]::new))));
        map.set("y", NTxObjs.elem(NElement.ofArray(Arrays.stream(plotY).mapToObj(NElement::ofDouble).toArray(NElement[]::new))));
        map.set("rel", NTxObjs.elem(NElement.ofArray(Arrays.stream(rel).mapToObj(NElement::ofDouble).toArray(NElement[]::new))));
        map.set("percent", NTxObjs.elem(NElement.ofArray(Arrays.stream(rel).mapToObj(NElement::ofDouble).toArray(NElement[]::new))));
        map.set("db", NTxObjs.elem(NElement.ofArray(Arrays.stream(diffDb).mapToObj(NElement::ofDouble).toArray(NElement[]::new))));
        map.set("diffDb", NTxObjs.elem(NElement.ofArray(Arrays.stream(diffDb).mapToObj(NElement::ofDouble).toArray(NElement[]::new))));
        map.set("diff", NTxObjs.elem(NElement.ofArray(Arrays.stream(diff).mapToObj(NElement::ofDouble).toArray(NElement[]::new))));

        map.set("max", NTxObjs.elem(NElement.ofDouble(maxRel)));
        map.set("mean", NTxObjs.elem(NElement.ofDouble(meanRel)));
        map.set("min", NTxObjs.elem(NElement.ofDouble(minRel)));

        map.set("maxDb", NTxObjs.elem(NElement.ofDouble(maxDb)));
        map.set("meanDb", NTxObjs.elem(NElement.ofDouble(meanDb)));
        map.set("minDb", NTxObjs.elem(NElement.ofDouble(minDb)));

        map.set("meanFormatted", NTxObjs.elem(NElement.ofString(String.format(Locale.US, "%.2f%%", meanRel))));
        map.set("maxFormatted", NTxObjs.elem(NElement.ofString(String.format(Locale.US, "%.2f%%", maxRel))));
        map.set("meanDbFormatted", NTxObjs.elem(NElement.ofString(String.format(Locale.US, "%.2f dB", meanDb))));
        map.set("maxDbFormatted", NTxObjs.elem(NElement.ofString(String.format(Locale.US, "%.2f dB", maxDb))));

        return map;
    }

    private static NElement getProperty(NElement elem, String name) {
        if (elem == null) return null;
        if (elem.isCustom()) {
            Object val = elem.asCustom().get().value();
            if (val instanceof NTxObj) {
                NOptional<NTxObj> prop = ((NTxObj) val).get(name);
                if (prop.isPresent()) {
                    return prop.get().toElement();
                }
            }
        }
        if (elem.isObject()) {
            NOptional<NElement> opt = elem.asObject().get().get(name);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        if (elem.asListContainer().isPresent()) {
            NOptional<NElement> opt = elem.asListContainer().get().get(name);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

    private static double[] toDoubleArray(NElement elem) {
        if (elem == null || elem.isNull()) {
            return null;
        }
        NOptional<double[]> direct = NTxValue.of(elem).asDoubleArray();
        if (direct.isPresent()) {
            return direct.get();
        }
        if (elem.isListContainer()) {
            List<Double> list = new ArrayList<>();
            for (NElement c : elem.asListContainer().get().children()) {
                NOptional<Double> d = NTxValue.of(c).asDouble();
                if (d.isPresent()) {
                    list.add(d.get());
                } else if (c.isNumber()) {
                    list.add(c.asDoubleValue().orElse(0.0));
                }
            }
            if (!list.isEmpty()) {
                return list.stream().mapToDouble(Double::doubleValue).toArray();
            }
        }
        return null;
    }
}
