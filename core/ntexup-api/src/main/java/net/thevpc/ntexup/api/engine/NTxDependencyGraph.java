package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.node.NTxNode;

import java.util.Set;

public interface NTxDependencyGraph {
    void addDependency(String bindingName, NTxNode consumerNode, int pageIndex);

    Set<Integer> getDependentPages(String bindingName);

    Set<NTxNode> getDependentNodes(String bindingName);

    void notifyBindingUpdated(String bindingName);

    void addBindingListener(String bindingName, Runnable listener);

    void removeBindingListener(String bindingName, Runnable listener);

    void addPageInvalidationListener(java.util.function.IntConsumer listener);

    void removePageInvalidationListener(java.util.function.IntConsumer listener);
}
