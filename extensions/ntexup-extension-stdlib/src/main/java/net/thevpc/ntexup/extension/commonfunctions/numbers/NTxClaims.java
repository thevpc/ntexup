package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;

/**
 * Outcome rendering shared by the claim functions. A claim that holds renders as the value
 * it was checked on; one that does not renders as a bracketed {@code [CLAIM FAILED: ...]}
 * marker in the text itself and is logged as an error, so a stale sentence cannot survive
 * a re-render unnoticed.
 */
final class NTxClaims {

    static final String FAILED = "CLAIM FAILED";

    static NElement holds(String shown) {
        return NElement.ofString(shown);
    }

    static NElement fails(NTxNumericFunction.Call c, String why) {
        String call = String.valueOf(c.args.callExpression());
        c.args.log(NMsg.ofC("%s: %s (%s)", FAILED, why, call).asError(), c.args.scopedContext().source());
        return NElement.ofString("[" + FAILED + ": " + why + "]");
    }

    private NTxClaims() {
    }
}
