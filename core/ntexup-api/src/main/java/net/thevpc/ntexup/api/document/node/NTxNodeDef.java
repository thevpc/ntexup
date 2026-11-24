package net.thevpc.ntexup.api.document.node;

public interface NTxNodeDef extends NTxNode{
    NTxNodeDefParam[] params();
    NTxNode[] body();
    NTxNode bodyContainer();

    default String[] defaultBody() {
        return new String[0];
    }
}
