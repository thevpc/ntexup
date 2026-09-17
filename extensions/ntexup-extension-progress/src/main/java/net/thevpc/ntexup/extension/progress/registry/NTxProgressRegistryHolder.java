package net.thevpc.ntexup.extension.progress.registry;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Holds document-scoped NTxProgressRegistry instances.
 * Keyed by compiled document identity — registries are garbage-collected
 * when their owning document is no longer referenced.
 */
public class NTxProgressRegistryHolder {

    private static final Map<Object, NTxProgressRegistryImpl> REGISTRIES = new WeakHashMap<>();

    /**
     * Get or create the registry for a given document.
     *
     * @param documentKey the compiled document instance (used as identity key)
     */
    public static synchronized NTxProgressRegistry getOrCreate(Object documentKey) {
        return REGISTRIES.computeIfAbsent(documentKey, k -> new NTxProgressRegistryImpl());
    }

    /**
     * Get the registry for a given document, or null if none exists.
     */
    public static synchronized NTxProgressRegistry get(Object documentKey) {
        return REGISTRIES.get(documentKey);
    }

    /**
     * Remove the registry for a given document.
     */
    public static synchronized void remove(Object documentKey) {
        REGISTRIES.remove(documentKey);
    }
}
