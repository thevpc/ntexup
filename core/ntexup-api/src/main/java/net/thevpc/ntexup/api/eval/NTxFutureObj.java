package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class NTxFutureObj implements NTxObj, NTxFuture<NTxObj> {
    private final String name;
    private final Future<?> future;
    private final Supplier<NTxObj> resolver;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile NTxObj cachedResolved;

    public NTxFutureObj(String name, Future<?> future, Supplier<NTxObj> resolver) {
        this.name = name == null ? "future" : name;
        this.future = future;
        this.resolver = resolver;
    }

    public String name() {
        return name;
    }

    public Future<?> rawFuture() {
        return future;
    }

    @Override
    public boolean isDone() {
        if (cachedResolved != null) {
            return true;
        }
        if (future != null) {
            return future.isDone();
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return future != null && future.isCancelled();
    }

    @Override
    public NTxObj get() {
        if (cachedResolved != null) {
            return cachedResolved;
        }
        if (future != null) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Future resolution failed for " + name + ": " + e.getMessage(), e);
            }
        }
        if (resolver != null) {
            NTxObj resolved = resolver.get();
            if (resolved != null) {
                cachedResolved = resolved;
            }
            return resolved;
        }
        return cachedResolved;
    }

    @Override
    public NTxObj get(long timeout, TimeUnit unit) {
        if (cachedResolved != null) {
            return cachedResolved;
        }
        if (future != null) {
            try {
                future.get(timeout, unit);
            } catch (Exception e) {
                throw new RuntimeException("Future resolution failed for " + name + ": " + e.getMessage(), e);
            }
        }
        if (resolver != null) {
            NTxObj resolved = resolver.get();
            if (resolved != null) {
                cachedResolved = resolved;
            }
            return resolved;
        }
        return cachedResolved;
    }

    @Override
    public double progress() {
        return isDone() ? 1.0 : 0.0;
    }

    @Override
    public void addListener(Runnable listener) {
        if (listener != null) {
            if (isDone()) {
                listener.run();
            } else {
                listeners.add(listener);
            }
        }
    }

    public void triggerListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public Set<String> properties() {
        NTxObj r = isDone() ? get() : null;
        if (r != null) {
            return r.properties();
        }
        return Collections.emptySet();
    }

    @Override
    public NOptional<NTxObj> get(String property) {
        if (isDone()) {
            NTxObj r = get();
            return r == null ? NOptional.ofEmpty() : r.get(property);
        }
        String childName = name + "." + property;
        NTxFutureObj child = new NTxFutureObj(childName, future, () -> {
            NTxObj parentObj = NTxFutureObj.this.get();
            if (parentObj != null) {
                return parentObj.get(property).orNull();
            }
            return null;
        });
        this.addListener(child::triggerListeners);
        return NOptional.of(child);
    }

    @Override
    public NElement toElement() {
        if (isDone()) {
            NTxObj r = get();
            return r == null ? NElement.ofNull() : r.toElement();
        }
        return NElement.ofObjectBuilder("pending")
                .add("name", NElement.ofString(name))
                .add("ready", NElement.ofBoolean(false))
                .add("ref", NElement.ofCustom(this))
                .build();
    }
}
