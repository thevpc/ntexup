package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.elem.NElement;

public class NTxParserUtils {
    public static boolean isIntOrExprNonCommon(NElement currentArg){
        switch (currentArg.type().typeGroup()) {
            case OPERATOR:{
                return true;
            }
        }
        switch (currentArg.type()) {
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            {
                return true;
            }
            case DOUBLE_QUOTED_STRING:
            case SINGLE_QUOTED_STRING:
            case BACKTICK_STRING:
            case TRIPLE_DOUBLE_QUOTED_STRING:
            case TRIPLE_SINGLE_QUOTED_STRING:
            case TRIPLE_BACKTICK_STRING:
            case LINE_STRING:
            case NAME:
            {
                if (!isCommonStyleProperty(currentArg.asStringValue().get())) {
                    return true;
                }
                break;
            }

            case UPLET:
            case NAMED_UPLET:

            case NULL:
                return true;
        }
        return false;
    }

    public static boolean isCommonStyleFlagProperty(String s) {
        if (NBlankable.isBlank(s)) {
            return false;
        }
        s = NTxUtils.uid(s);
        return NTxStyleParser.COMMON_FLAG_STYLE_PROPS.contains(s);
    }

    public static boolean isCommonStyleProperty(String s) {
        if (NBlankable.isBlank(s)) {
            return false;
        }
        s = NTxUtils.uid(s);
        return NTxStyleParser.COMMON_STYLE_PROPS.contains(s);
    }
}
