package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NNameFormat;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

public enum NTxTextWrap implements NToElement {
    VERBATIM,
    WRAP;

    public static NOptional<NTxTextWrap> parse(String value) {
        if (NBlankable.isBlank(value)) {
            return NOptional.ofEmpty();
        }
        String s = NNameFormat.CONST_NAME.format(NStringUtils.strip(value));
        switch (s) {
            case "VERBATIM":
            case "NONE":
            case "NOWRAP":
            case "NO_WRAP":
            case "FALSE":
                return NOptional.of(VERBATIM);
            case "WRAP":
            case "AUTO":
            case "WORD":
            case "TRUE":
                return NOptional.of(WRAP);
        }
        try {
            return NOptional.of(valueOf(s));
        } catch (Exception e) {
            return NOptional.ofNamedEmpty("TextWrap " + value);
        }
    }

    @Override
    public NElement toElement() {
        return NElement.ofName(name().toLowerCase());
    }
}
