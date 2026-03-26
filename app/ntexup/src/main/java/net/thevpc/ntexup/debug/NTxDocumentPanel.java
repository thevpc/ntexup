package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.nuts.elem.NElement;

import java.util.function.Supplier;

public class NTxDocumentPanel extends NTxTsonPanel {
    private Supplier<NTxDocument> model;

    public NTxDocumentPanel(String name,NTxEngine engine, Supplier<NTxDocument> model) {
        super(name,engine);
        this.model = model;
    }

    @Override
    public NElement createTson() {
        NTxDocument m = null;
        m = model.get();
        if (m != null) {
            return engine.toElement(m, false);
        }
        return null;
    }

}
