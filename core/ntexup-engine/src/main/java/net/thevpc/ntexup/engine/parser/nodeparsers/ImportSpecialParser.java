package net.thevpc.ntexup.engine.parser.nodeparsers;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeImport;
import net.thevpc.nuts.concurrent.NCallableSupport;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NUpletElement;
import net.thevpc.nuts.util.NMsg;

public class ImportSpecialParser extends NTxNodeParserBase {
    public ImportSpecialParser() {
        super(true, NTxNodeType.CTRL_IMPORT);
    }


    @Override
    public NCallableSupport<NTxItem> parseNode(NTxNodeFactoryParseContext context) {
        NElement tsonElement = context.element();
        switch (tsonElement.type()) {
            case NAMED_UPLET: {
                NUpletElement uplet = tsonElement.asUplet().get();
                if (uplet.isNamed("import")) {
                    return NCallableSupport.ofValid( () -> new CtrlNTxNodeImport(context.source(),uplet.params()));
                }
                break;
            }
        }
        return NCallableSupport.ofInvalid(NMsg.ofC("missing import from %s", NTxUtils.snippet(tsonElement)).asError());
    }



}

