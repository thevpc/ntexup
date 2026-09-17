package net.thevpc.ntexup.extension.progress.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Document-scoped implementation of the progress registry.
 * Attach one instance per NTxCompiledDocument.
 */
public class NTxProgressRegistryImpl implements NTxProgressRegistry {

    private final Map<String, NTxPendingBinding> bindings = new LinkedHashMap<>();

    @Override
    public void register(String bindingName, NTxPendingBinding binding) {
        bindings.put(bindingName, binding);
    }

    @Override
    public void unregister(String bindingName) {
        bindings.remove(bindingName);
    }

    @Override
    public NTxPendingBinding get(String bindingName) {
        return bindings.get(bindingName);
    }

    @Override
    public List<NTxPendingBinding> select(NTxProgressSelect filter) {
        return bindings.values().stream()
                .filter(b -> filter.matches(b.name()))
                .collect(Collectors.toList());
    }

    @Override
    public List<NTxPendingBinding> all() {
        return new ArrayList<>(bindings.values());
    }

    @Override
    public void clear() {
        bindings.clear();
    }
}
