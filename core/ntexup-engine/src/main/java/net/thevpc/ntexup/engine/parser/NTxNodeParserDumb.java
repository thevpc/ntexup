package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.parser.NTxArgumentReader;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPairElement;

public class NTxNodeParserDumb extends NTxNodeParserBase {
    public NTxNodeParserDumb(boolean container, String id, String... aliases) {
        super(container, id, aliases);
    }

    @Override
    public void compileNode(NTxNode node, NTxResolutionContext context) {

    }

    protected boolean processArgument(NTxArgumentReader info) {
        if (defaultProcessArgument(info)) {
            return true;
        }
        NElement e = info.peek();
        if (e != null) {
            if (e.isNamedPair()) {
                NPairElement p = e.asPair().get();
                String sid = NTxUtils.uid(p.key());
                info.node().setProperty(sid, NTxUtils.addCompilerDeclarationPath(p.value(), info.source()));
                info.read();
                return true;
            } else if (e.isName()) {
                String sid = NTxUtils.uid(e);
                info.node().setProperty(sid, NTxUtils.addCompilerDeclarationPath(NElement.ofTrue(), info.source()));
                info.read();
                return true;
            }
        }
        return false;
    }
}
