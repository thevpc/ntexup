package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NTxFutureUtils {

    public static boolean isFuture(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof NTxFuture || obj instanceof Future || obj instanceof NTxFutureObj) {
            return true;
        }
        if (obj instanceof NElement) {
            NElement e = (NElement) obj;
            if (e.isCustom()) {
                Object v = e.asCustom().get().value();
                if (v instanceof NTxFuture || v instanceof Future || v instanceof NTxFutureObj) {
                    return true;
                }
            }
            if (e.asListContainer().isPresent()) {
                NOptional<NElement> ref = e.asListContainer().get().get("ref");
                if (ref.isPresent() && ref.get().isCustom()) {
                    Object v = ref.get().asCustom().get().value();
                    if (v instanceof NTxFuture || v instanceof Future || v instanceof NTxFutureObj) {
                        return true;
                    }
                }
                String n = e.asNamed().flatMap(x -> x.name()).orElseGet(() ->
                        e.asObject().flatMap(x -> x.name()).orElse("")
                );
                if ("pending".equalsIgnoreCase(n)) {
                    return true;
                }
                if (e.asListContainer().get().get("pending").isPresent()) {
                    return true;
                }
            } else if (e.isName() || e.isString()) {
                if ("pending".equalsIgnoreCase(e.asStringValue().orElse(""))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isReady(Object obj) {
        if (!isFuture(obj)) {
            return true;
        }
        if (obj instanceof NTxFuture) {
            return ((NTxFuture<?>) obj).isDone();
        }
        if (obj instanceof Future) {
            return ((Future<?>) obj).isDone();
        }
        if (obj instanceof NTxFutureObj) {
            return ((NTxFutureObj) obj).isDone();
        }
        if (obj instanceof NElement) {
            NElement e = (NElement) obj;
            if (e.isCustom()) {
                Object v = e.asCustom().get().value();
                return isReady(v);
            }
            if (e.asListContainer().isPresent()) {
                NOptional<NElement> ref = e.asListContainer().get().get("ref");
                if (ref.isPresent() && ref.get().isCustom()) {
                    return isReady(ref.get().asCustom().get().value());
                }
                String n = e.asNamed().flatMap(x -> x.name()).orElseGet(() ->
                        e.asObject().flatMap(x -> x.name()).orElse("")
                );
                if ("pending".equalsIgnoreCase(n)) {
                    return false;
                }
            } else if (e.isName() || e.isString()) {
                if ("pending".equalsIgnoreCase(e.asStringValue().orElse(""))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Object await(Object obj) {
        if (!isFuture(obj)) {
            return obj;
        }
        try {
            if (obj instanceof NTxFuture) {
                return ((NTxFuture<?>) obj).get();
            }
            if (obj instanceof Future) {
                return ((Future<?>) obj).get();
            }
            if (obj instanceof NTxFutureObj) {
                NTxObj r = ((NTxFutureObj) obj).get();
                return r != null ? r.toElement() : null;
            }
            if (obj instanceof NElement) {
                NElement e = (NElement) obj;
                if (e.isCustom()) {
                    Object v = e.asCustom().get().value();
                    Object r = await(v);
                    if (r instanceof NTxObj) {
                        return ((NTxObj) r).toElement();
                    }
                    return r;
                }
                if (e.asListContainer().isPresent()) {
                    NOptional<NElement> ref = e.asListContainer().get().get("ref");
                    if (ref.isPresent() && ref.get().isCustom()) {
                        Object r = await(ref.get().asCustom().get().value());
                        if (r instanceof NTxObj) {
                            return ((NTxObj) r).toElement();
                        }
                        return r;
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return obj;
    }

    public static Object await(Object obj, long timeout, TimeUnit unit) {
        if (!isFuture(obj)) {
            return obj;
        }
        try {
            if (obj instanceof NTxFuture) {
                return ((NTxFuture<?>) obj).get(timeout, unit);
            }
            if (obj instanceof Future) {
                return ((Future<?>) obj).get(timeout, unit);
            }
            if (obj instanceof NTxFutureObj) {
                NTxObj r = ((NTxFutureObj) obj).get(timeout, unit);
                return r != null ? r.toElement() : null;
            }
            if (obj instanceof NElement) {
                NElement e = (NElement) obj;
                if (e.isCustom()) {
                    Object v = e.asCustom().get().value();
                    Object r = await(v, timeout, unit);
                    if (r instanceof NTxObj) {
                        return ((NTxObj) r).toElement();
                    }
                    return r;
                }
                if (e.asListContainer().isPresent()) {
                    NOptional<NElement> ref = e.asListContainer().get().get("ref");
                    if (ref.isPresent() && ref.get().isCustom()) {
                        Object r = await(ref.get().asCustom().get().value(), timeout, unit);
                        if (r instanceof NTxObj) {
                            return ((NTxObj) r).toElement();
                        }
                        return r;
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return obj;
    }

    public static double progress(Object obj) {
        if (!isFuture(obj)) {
            return 1.0;
        }
        if (obj instanceof NTxFuture) {
            return ((NTxFuture<?>) obj).progress();
        }
        if (obj instanceof NTxFutureObj) {
            return ((NTxFutureObj) obj).progress();
        }
        return isReady(obj) ? 1.0 : 0.0;
    }

    public static void addListener(Object obj, Runnable listener) {
        if (listener == null || obj == null) {
            return;
        }
        if (obj instanceof NTxFuture) {
            ((NTxFuture<?>) obj).addListener(listener);
        } else if (obj instanceof NTxFutureObj) {
            ((NTxFutureObj) obj).addListener(listener);
        } else if (obj instanceof NElement) {
            NElement e = (NElement) obj;
            if (e.isCustom()) {
                Object v = e.asCustom().get().value();
                addListener(v, listener);
            } else if (e.asListContainer().isPresent()) {
                NOptional<NElement> ref = e.asListContainer().get().get("ref");
                if (ref.isPresent() && ref.get().isCustom()) {
                    addListener(ref.get().asCustom().get().value(), listener);
                }
            }
        }
    }
}
