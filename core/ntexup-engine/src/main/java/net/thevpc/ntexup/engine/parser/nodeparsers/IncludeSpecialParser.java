package net.thevpc.ntexup.engine.parser.nodeparsers;

import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeInclude;
import net.thevpc.nuts.concurrent.NScorableCallable;
import net.thevpc.nuts.elem.NUpletElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.elem.NElement;

public class IncludeSpecialParser extends NTxNodeParserBase {
    public IncludeSpecialParser() {
        super(true, NTxNodeType.CTRL_INCLUDE);
    }


    @Override
    public NScorableCallable<NTxItem> parseNode(NTxNodeFactoryParseContext context) {
        NElement tsonElement = context.element();
        switch (tsonElement.type()) {
            case NAMED_UPLET: {
                NUpletElement uplet = tsonElement.asUplet().get();
                if (uplet.isNamed("include")) {
                    return NScorableCallable.ofValid( () -> new CtrlNTxNodeInclude(context.source(),uplet.params()));
                }
                break;
            }
        }
        return NScorableCallable.ofInvalid(NMsg.ofC("missing include from %s", NTxUtils.snippet(tsonElement)).asError());
    }



}

