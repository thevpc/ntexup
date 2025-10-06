package net.thevpc.ntexup.engine.parser.nodeparsers;

import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxItemList;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeIf;
import net.thevpc.nuts.concurrent.NCallableSupport;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;
import net.thevpc.nuts.elem.NUpletElement;
import net.thevpc.nuts.util.NMsg;

import java.util.ArrayList;
import java.util.List;

public class IfSpecialParser extends NTxNodeParserBase {
    public IfSpecialParser() {
        super(true, NTxNodeType.CTRL_IF);
    }

    @Override
    public NCallableSupport<NTxItem> parseNode(NTxNodeFactoryParseContext context) {
        NElement tsonElement = context.element();
        switch (tsonElement.type()) {
            case NAMED_PARAMETRIZED_OBJECT: {
                NObjectElement obj = tsonElement.asObject().get();
                if (obj.isNamed(id())) {
                    return NCallableSupport.ofValid(()->{
                        NElement __cond = null;
                        List<NTxNode> __trueBloc = new ArrayList<>();
                        List<NTxNode> __falseBloc = new ArrayList<>();
                        for (NElement child : obj.children()) {
                            __trueBloc.add((NTxNode) context.engine().newNode(child, context).get());
                        }
                        for (NElement e : obj.params().get()) {
                            if (e.isNamedPair("$condition")) {
                                if (__cond != null) {
                                    _logError(NMsg.ofC("unexpected %s", NTxUtils.snippet(tsonElement)),context);
                                    return new NTxItemList();
                                }
                                __cond = e.asPair().get().value();
                            } else if (e.isNamedPair("$else")) {
                                NElement ee = e.asPair().get().value();
                                if (ee.isAnyObject()) {
                                    for (NElement child : ee.asObject().get().children()) {
                                        __falseBloc.add((NTxNode) context.engine().newNode(child, context).get());
                                    }
                                }
                            } else {
                                if (__cond != null) {
                                    _logError(NMsg.ofC("unexpected %s", NTxUtils.snippet(tsonElement)),context);
                                }else {
                                    __cond = e;
                                }
                            }
                        }
                        CtrlNTxNodeIf cc = new CtrlNTxNodeIf(context.source(), __cond, __trueBloc, __falseBloc);
                        cc.setParent(context.node());
                        return cc;
                    });
                }
                break;
            }
            case NAMED_OBJECT: {
                NObjectElement obj = tsonElement.asObject().get();
                if (obj.isNamed(id())) {
                    return NCallableSupport.ofValid( () -> {
                        _logError(NMsg.ofC("missing if condition from %s", NTxUtils.snippet(tsonElement)), context);
                        return new NTxItemList();
                    });
                }
                break;
            }
            case NAMED_UPLET: {
                NUpletElement obj = tsonElement.asUplet().get();
                if (obj.isNamed(id())) {
                    return NCallableSupport.ofValid( () -> {
                        _logError(NMsg.ofC("missing if body from %s", NTxUtils.snippet(tsonElement)), context);
                        return new NTxItemList();
                    });
                }
            }
        }
        return NCallableSupport.ofInvalid(NMsg.ofC("missing if construct from %s", NTxUtils.snippet(tsonElement)).asError());
    }

}

