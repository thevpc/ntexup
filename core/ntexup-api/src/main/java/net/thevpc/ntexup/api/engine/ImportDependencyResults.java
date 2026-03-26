package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.renderer.NTxImageTypeRendererFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImportDependencyResults {
    private List<ImportDependencyResult> importDependencies;

    public ImportDependencyResults(ImportDependencyResult[] importDependencies) {
        this.importDependencies = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(importDependencies)));
    }

    public List<ImportDependencyResult> all() {
        return importDependencies;
    }
}
