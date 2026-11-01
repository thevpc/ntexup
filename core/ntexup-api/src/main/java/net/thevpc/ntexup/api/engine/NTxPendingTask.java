package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.eval.NTxFutureUtils;

/**
 * A named background computation the document is waiting on, as listed by
 * {@link NTxCompiledDocument#pendingTasks()}.
 */
public final class NTxPendingTask {
    private final String name;
    private final Object future;
    private final long startMillis;

    public NTxPendingTask(String name, Object future) {
        this.name = name;
        this.future = future;
        this.startMillis = System.currentTimeMillis();
    }

    public String name() {
        return name;
    }

    /**
     * Time since the task was registered, which includes any time it spent queued.
     */
    public long elapsedMillis() {
        return System.currentTimeMillis() - startMillis;
    }

    /**
     * @return completion in [0, 1]; stays at 0 until done for a future that does
     * not report progress
     */
    public double progress() {
        return NTxFutureUtils.progress(future);
    }

    public boolean isDone() {
        return NTxFutureUtils.isReady(future);
    }
}
