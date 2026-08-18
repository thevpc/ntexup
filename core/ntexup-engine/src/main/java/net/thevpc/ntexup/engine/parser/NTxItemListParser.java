package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxItemList;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrNTxNodelUncompiled;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.elem.NElement;

public class NTxItemListParser {
    public static NOptional<NTxItemList> readHItemList(NElement ff, NTxDocumentFactory f, NTxResolutionContext context) {
        NTxItemList pg = new NTxItemList();

        switch (ff.type()) {
            case NAMED_TUPLE:

            case OBJECT:
            case FULL_OBJECT:
            case PARAM_OBJECT:
            case NAMED_OBJECT:

            case ARRAY:
            case FULL_ARRAY:
            case PARAM_ARRAY:
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
                    pg.add(
                            new CtrNTxNodelUncompiled(e,context.source())
                    );
                }
            }
        }
        return NOptional.of(pg);
    }
}
