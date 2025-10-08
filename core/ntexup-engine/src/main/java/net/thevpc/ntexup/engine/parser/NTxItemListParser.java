package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxItemList;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.elem.NElement;

public class NTxItemListParser {
    public static NOptional<NTxItemList> readHItemList(NElement ff, NTxDocumentFactory f, NTxNodeFactoryParseContext context) {
        NTxItemList pg = new NTxItemList();

        switch (ff.type()) {
            case NAMED_UPLET:

            case OBJECT:
            case NAMED_PARAMETRIZED_OBJECT:
            case PARAMETRIZED_OBJECT:
            case NAMED_OBJECT:

            case ARRAY:
            case NAMED_PARAMETRIZED_ARRAY:
            case PARAMETRIZED_ARRAY:
            case NAMED_ARRAY:
            {
                NTxValue ee = NTxValue.of(ff);
                for (NElement e : ee.args()) {
                    NOptional<NTxProp[]> u = NTxStyleParser.parseStyle(e, context);
                    if (u.isPresent()) {
                        for (NTxProp s : u.get()) {
                            pg.add(s);
                        }
                    } else {
                        return NOptional.ofNamedError("invalid " + e + " for page-group");
                    }
                }
                for (NElement e : ee.body()) {
                    NOptional<NTxItem> u = context.engine().newNode(e, context);
                    if (u.isPresent()) {
                        pg.add(u.get());
                    } else {
                        return NOptional.ofError(
                                () -> NMsg.ofC("Error parsing page group : %s", e)
                        );
                    }
                }
            }
        }
        return NOptional.of(pg);
    }
}
