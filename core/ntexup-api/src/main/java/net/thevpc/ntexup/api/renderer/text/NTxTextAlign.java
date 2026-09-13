package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NNameFormat;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

public enum NTxTextAlign implements NToElement {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY;

    public static NOptional<NTxTextAlign> parse(String value) {
        if (NBlankable.isBlank(value)) {
            return NOptional.ofEmpty();
        }
        String s = NNameFormat.CONST_NAME.format(NStringUtils.strip(value));
        switch (s) {
            case "LEFT":
            case "START":
                return NOptional.of(LEFT);
            case "CENTER":
            case "MIDDLE":
                return NOptional.of(CENTER);
            case "RIGHT":
            case "END":
                return NOptional.of(RIGHT);
            case "JUSTIFY":
            case "JUSTIFIED":
                return NOptional.of(JUSTIFY);
        }
        try {
            return NOptional.of(valueOf(s));
        } catch (Exception e) {
            return NOptional.ofNamedEmpty("TextAlign " + value);
        }
    }

    @Override
    public NElement toElement() {
        return NElement.ofName(name().toLowerCase());
    }
}
