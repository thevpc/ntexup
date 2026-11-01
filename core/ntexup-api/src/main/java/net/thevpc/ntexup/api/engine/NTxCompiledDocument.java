package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.security.NTxManifest;
import net.thevpc.ntexup.api.document.security.NTxManifestOptions;
import net.thevpc.ntexup.api.eval.NTxFuture;
import net.thevpc.ntexup.api.eval.NTxObj;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.Iterator;
import java.util.List;

public interface NTxCompiledDocument {
    NTxSource source();

    NTxDocument document();

    boolean isCompiled();

    NTxDocument rawDocument();

    String title();

    NTxEngine engine();

    Iterator<NTxCompiledPage> pagesIterator();

    NOptional<NTxCompiledPage> page(int index);

    List<NTxCompiledPage> pages();

    Throwable currentThrowable();

    NElement toElement(boolean semantic);

    NTxSourceMonitor sourceMonitor();

    NTxManifest computeManifest(NTxManifestOptions options);

    NOptional<NTxObj> getGlobalObject(String name);

    NTxCompiledDocument setGlobalObject(String name, NTxObj obj);

    NTxDependencyGraph dependencyGraph();

    void registerFuture(Object future);

    boolean hasPendingFutures();

    /**
     * Same as {@link #registerFuture(Object)}, and also lists the future by name
     * in {@link #pendingTasks()} until it completes. Registering a future again
     * keeps its original name and start time.
     *
     * @param name   name to report the task under
     * @param future the future to track
     */
    void registerTask(String name, Object future);

    /**
     * Document variable that hides the screen renderer's running-tasks badge when false.
     */
    String VAR_SHOW_RUNNING_TASKS = "documentShowRunningTasks";

    /**
     * @return the named tasks still running or queued, oldest first. Futures
     * registered without a name count for {@link #hasPendingFutures()} only.
     */
    java.util.List<NTxPendingTask> pendingTasks();

    void awaitFutures();

    void awaitFutures(long timeout, java.util.concurrent.TimeUnit unit);

    /**
     * Register a named future with the document's progress tracking system.
     * When the progress extension is loaded, this binds the future to a
     * progress-monitor component that can query its state by name.
     * No-op when the progress extension is not available.
     *
     * @param name   binding name (must match the name used in progress-monitor's select)
     * @param future the future to track
     */
    default void registerProgressBinding(String name, NTxFuture<?> future) {
        // no-op by default; progress extension overrides via engine implementation
    }
}
