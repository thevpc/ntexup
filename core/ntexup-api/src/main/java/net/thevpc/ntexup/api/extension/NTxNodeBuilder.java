package net.thevpc.ntexup.api.extension;

import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.nuts.spi.NComponent;

public interface NTxNodeBuilder extends NComponent {

    void build(NTxNodeBuilderContext builderContext);

}
