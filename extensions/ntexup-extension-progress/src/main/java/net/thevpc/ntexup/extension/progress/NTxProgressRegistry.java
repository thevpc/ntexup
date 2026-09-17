package net.thevpc.ntexup.extension.progress;

import net.thevpc.ntexup.extension.progress.registry.NTxPendingBinding;
import net.thevpc.ntexup.extension.progress.registry.NTxProgressSelect;

import java.util.List;

/**
 * Document-scoped registry for tracking pending futures by binding name.
 * Components query this to get current progress state for selected bindings.
 */
public interface NTxProgressRegistry {

    /**
     * Register a future under a binding name.
     * If a binding with the same name already exists, it is replaced.
     */
    void register(String bindingName, NTxPendingBinding binding);

    /**
     * Remove a binding by name.
     */
    void unregister(String bindingName);

    /**
     * Get a specific binding by name, or null if not found.
     */
    NTxPendingBinding get(String bindingName);

    /**
     * Select bindings matching the given filter.
     */
    List<NTxPendingBinding> select(NTxProgressSelect filter);

    /**
     * Return all registered bindings.
     */
    List<NTxPendingBinding> all();

    /**
     * Remove all bindings.
     */
    void clear();
}
