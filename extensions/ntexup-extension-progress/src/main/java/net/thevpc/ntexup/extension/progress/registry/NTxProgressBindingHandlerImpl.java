package net.thevpc.ntexup.extension.progress.registry;

import net.thevpc.ntexup.api.engine.NTxProgressBindingHandler;
import net.thevpc.ntexup.api.eval.NTxFuture;
import net.thevpc.ntexup.extension.progress.NTxProgressRegistry;

/**
 * Progress extension's implementation of NTxProgressBindingHandler.
 * Registered via SPI; discovered by the engine at runtime.
 */
public class NTxProgressBindingHandlerImpl implements NTxProgressBindingHandler {

    @Override
    public void register(Object documentKey, String name, NTxFuture<?> future) {
        NTxProgressRegistry registry = NTxProgressRegistryHolder.getOrCreate(documentKey);
        registry.register(name, new NTxPendingBinding(name, future));
    }
}
