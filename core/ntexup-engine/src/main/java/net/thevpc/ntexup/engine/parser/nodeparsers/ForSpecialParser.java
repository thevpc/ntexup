package net.thevpc.ntexup.engine.parser.nodeparsers;

import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxItemList;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeFor;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;
import net.thevpc.nuts.elem.NUpletElement;
import net.thevpc.nuts.text.NMsg;

import java.util.ArrayList;
import java.util.List;

public class ForSpecialParser extends NTxNodeParserBase {
    public ForSpecialParser() {
        super(true, NTxNodeType.CTRL_FOR);
    }

    @Override
    public NScoredCallable<NTxItem> parseNode(NTxNodeFactoryParseContext context) {
        NElement tsonElement = context.element();
        switch (tsonElement.type()) {
            case NAMED_PARAMETRIZED_OBJECT: {
                NObjectElement obj = tsonElement.asObject().get();
                if(obj.isNamed(id())) {
                    return NScoredCallable.ofValid(()-> {
                        NElement __varName = null;
                        NElement __varDomain = null;
                        List<NElement> block = new ArrayList<>();
                        for (NElement child : obj.children()) {
                            block.add(child);
                        }
                        for (NElement e : obj.params().get()) {
                            if (e.isPair()) {
                                __varName = e.asPair().get().key();
                                __varDomain = e.asPair().get().value();
                            } else {
                                _logError(NMsg.ofC("unexpected %s", NTxUtils.snippet(tsonElement)),context);
                            }
                        }
                        CtrlNTxNodeFor cc = new CtrlNTxNodeFor(context.source(), __varName, __varDomain, block);
                        cc.setParent(context.node());
                        return cc;
                    });
                }
                break;
            }
            case NAMED_OBJECT: {
                NObjectElement obj = tsonElement.asObject().get();
                if (obj.isNamed(id())) {
                    return NScoredCallable.ofValid( () -> {
                        _logError(NMsg.ofC("missing for condition from %s", NTxUtils.snippet(tsonElement)), context);
                        return new NTxItemList();
                    });
                }
                break;
            }
            case NAMED_UPLET: {
                NUpletElement obj = tsonElement.asUplet().get();
                if (obj.isNamed(id())) {
                    return NScoredCallable.ofValid( () -> {
                        _logError(NMsg.ofC("missing for body from %s", NTxUtils.snippet(tsonElement)), context);
                        return new NTxItemList();
                    });
                }
                break;
            }
        }
        return NScoredCallable.ofInvalid(NMsg.ofC("missing for construct from %s", NTxUtils.snippet(tsonElement)).asError());
    }




}

