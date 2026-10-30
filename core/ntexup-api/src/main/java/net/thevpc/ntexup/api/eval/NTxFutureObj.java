package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class NTxFutureObj implements NTxObj, NTxFuture<NTxObj> {
    private final String name;
    private final Future<?> future;
    private final Supplier<NTxObj> resolver;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile NTxObj cachedResolved;

    /**
     * Objects waiting for a listener to be released. A plain {@link Future} exposes
     * no completion hook, so an object built on one would never notify anyone:
     * an interactive page that renders while a solver is still running asks for a
     * repaint on completion and then sits there showing "pending" until the user
     * resizes the window. One shared daemon poller covers all of them and stops
     * as soon as nothing is registered.
     */
    private static final Set<NTxFutureObj> WATCHED = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static ScheduledExecutorService watcher;

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

    public Future<?> future() {
        return future;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxFutureObj that = (NTxFutureObj) o;
        return java.util.Objects.equals(name, that.name) && java.util.Objects.equals(future, that.future);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, future);
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
                watch();
            }
        }
    }

    private void watch() {
        if (future == null) {
            // Resolver-only objects are never "done" on their own (subclasses such
            // as the combined future drive their listeners from their dependencies),
            // so there is nothing to poll.
            return;
        }
        WATCHED.add(this);
        synchronized (NTxFutureObj.class) {
            if (watcher == null || watcher.isShutdown()) {
                watcher = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "NTxFutureObj-Watchdog");
                    thread.setDaemon(true);
                    return thread;
                });
                watcher.scheduleWithFixedDelay(NTxFutureObj::pollWatched, 50, 50, TimeUnit.MILLISECONDS);
            }
        }
    }

    private static void pollWatched() {
        Iterator<NTxFutureObj> it = WATCHED.iterator();
        while (it.hasNext()) {
            NTxFutureObj watched = it.next();
            if (watched.isDone()) {
                it.remove();
                watched.triggerListeners();
            }
        }
        if (WATCHED.isEmpty()) {
            synchronized (NTxFutureObj.class) {
                if (WATCHED.isEmpty() && watcher != null) {
                    watcher.shutdown();
                    watcher = null;
                }
            }
        }
    }

    public void triggerListeners() {
        List<Runnable> pending = new ArrayList<>(listeners);
        listeners.removeAll(pending);
        for (Runnable listener : pending) {
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
