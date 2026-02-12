package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.engine.NTxCompiledPage;

public interface NTxPageCompileListener {
    void onBeforeCompile(NTxCompiledPage a);
    void onAfterCompile(NTxCompiledPage a);
}
