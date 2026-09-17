package net.thevpc.ntexup.api.eval;

import java.util.concurrent.TimeUnit;

public interface NTxFuture<T> {
    boolean isDone();

    boolean isCancelled();

    T get();

    T get(long timeout, TimeUnit unit);

    double progress();

    void addListener(Runnable listener);
}
