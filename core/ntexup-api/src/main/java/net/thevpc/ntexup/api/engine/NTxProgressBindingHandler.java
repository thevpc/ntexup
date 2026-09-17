package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.eval.NTxFuture;

/**
 * Handler for registering futures with the document's progress tracking system.
 * Implemented by the progress extension; discovered via SPI when available.
 * <p>
 * When the progress extension is loaded, the engine delegates
 * {@link NTxCompiledDocument#registerProgressBinding(String, NTxFuture)} to this handler.
 */
public interface NTxProgressBindingHandler {

    /**
     * Register a named future for progress tracking.
     *
     * @param documentKey the compiled document instance (identity key)
     * @param name        binding name (must match the name used in progress-monitor's select)
     * @param future      the future to track
     */
    void register(Object documentKey, String name, NTxFuture<?> future);
}
