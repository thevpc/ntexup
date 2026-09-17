package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxDependencyGraph;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultNTxDependencyGraph implements NTxDependencyGraph {
    private final Map<String, Set<Integer>> pageDependencies = new ConcurrentHashMap<>();
    private final Map<String, Set<NTxNode>> nodeDependencies = new ConcurrentHashMap<>();
    private final Map<String, List<Runnable>> bindingListeners = new ConcurrentHashMap<>();

    private final List<java.util.function.IntConsumer> pageInvalidationListeners = new CopyOnWriteArrayList<>();

    private String rootName(String name) {
        if (name == null) return "";
        int dot = name.indexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private boolean isMatchingBinding(String registeredKey, String updatedBinding) {
        if (registeredKey == null || updatedBinding == null) return false;
        if (registeredKey.equals(updatedBinding)) return true;
        if (registeredKey.startsWith(updatedBinding + ".") || updatedBinding.startsWith(registeredKey + ".")) return true;
        if (registeredKey.endsWith("." + updatedBinding) || updatedBinding.endsWith("." + registeredKey)) return true;
        if (registeredKey.contains("." + updatedBinding + ".") || updatedBinding.contains("." + registeredKey + ".")) return true;
        return false;
    }

    @Override
    public void addDependency(String bindingName, NTxNode consumerNode, int pageIndex) {
        if (bindingName == null || bindingName.isEmpty()) {
            return;
        }
        pageDependencies.computeIfAbsent(bindingName, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(pageIndex);
        if (consumerNode != null) {
            nodeDependencies.computeIfAbsent(bindingName, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(consumerNode);
        }
        String root = rootName(bindingName);
        if (!root.equals(bindingName)) {
            pageDependencies.computeIfAbsent(root, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(pageIndex);
            if (consumerNode != null) {
                nodeDependencies.computeIfAbsent(root, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(consumerNode);
            }
        }
        if (bindingName.contains(".")) {
            String[] parts = bindingName.split("\\.");
            for (int i = 1; i < parts.length; i++) {
                String prefix = String.join(".", Arrays.copyOfRange(parts, 0, i));
                pageDependencies.computeIfAbsent(prefix, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(pageIndex);
                if (consumerNode != null) {
                    nodeDependencies.computeIfAbsent(prefix, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(consumerNode);
                }
            }
            for (String part : parts) {
                if (!part.isEmpty()) {
                    pageDependencies.computeIfAbsent(part, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(pageIndex);
                    if (consumerNode != null) {
                        nodeDependencies.computeIfAbsent(part, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(consumerNode);
                    }
                }
            }
        }
    }

    @Override
    public Set<Integer> getDependentPages(String bindingName) {
        Set<Integer> result = new LinkedHashSet<>();
        if (bindingName == null || bindingName.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, Set<Integer>> entry : pageDependencies.entrySet()) {
            if (isMatchingBinding(entry.getKey(), bindingName)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    @Override
    public Set<NTxNode> getDependentNodes(String bindingName) {
        Set<NTxNode> result = Collections.newSetFromMap(new IdentityHashMap<>());
        if (bindingName == null || bindingName.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, Set<NTxNode>> entry : nodeDependencies.entrySet()) {
            if (isMatchingBinding(entry.getKey(), bindingName)) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    @Override
    public void notifyBindingUpdated(String bindingName) {
        for (NTxNode node : getDependentNodes(bindingName)) {
            try {
                node.invalidateRenderCache();
            } catch (Throwable ignored) {}
        }
        triggerListeners(bindingName);
        for (int pageIndex : getDependentPages(bindingName)) {
            for (java.util.function.IntConsumer l : pageInvalidationListeners) {
                try {
                    l.accept(pageIndex);
                } catch (Throwable ignored) {}
            }
        }
    }

    private void triggerListeners(String bindingName) {
        if (bindingName == null || bindingName.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<Runnable>> entry : bindingListeners.entrySet()) {
            if (isMatchingBinding(entry.getKey(), bindingName)) {
                for (Runnable r : entry.getValue()) {
                    try {
                        r.run();
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    @Override
    public void addBindingListener(String bindingName, Runnable listener) {
        if (bindingName != null && listener != null) {
            bindingListeners.computeIfAbsent(bindingName, k -> new CopyOnWriteArrayList<>()).add(listener);
        }
    }

    @Override
    public void removeBindingListener(String bindingName, Runnable listener) {
        if (bindingName != null && listener != null) {
            List<Runnable> list = bindingListeners.get(bindingName);
            if (list != null) {
                list.remove(listener);
            }
        }
    }

    @Override
    public void addPageInvalidationListener(java.util.function.IntConsumer listener) {
        if (listener != null) {
            pageInvalidationListeners.add(listener);
        }
    }

    @Override
    public void removePageInvalidationListener(java.util.function.IntConsumer listener) {
        if (listener != null) {
            pageInvalidationListeners.remove(listener);
        }
    }
}
