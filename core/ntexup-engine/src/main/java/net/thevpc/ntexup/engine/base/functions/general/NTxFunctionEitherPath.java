package net.thevpc.ntexup.engine.base.functions.general;

import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.ntexup.api.eval.NTxFunctionContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.eval.NTxGitHelper;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.text.NMsg;

public class NTxFunctionEitherPath implements NTxFunction {
    @Override
    public String name() {
        return "eitherPath";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxFunctionContext context) {
        for (NTxFunctionArg arg : args.args()) {
            NElement u = arg.eval();
            if (!NBlankable.isBlank(u)) {
                String sp = u.asStringValue().orNull();
                if (!NBlankable.isBlank(u)) {
                    if (NTxGitHelper.isGithubFolder(sp)) {
                        try {
                            NPath pp = NTxGitHelper.resolveGithubPath(sp, context.log());
                            return NElement.ofString(pp.toString());
                        } catch (Exception ex) {
                            //just ignore
                            context.log().log(NMsg.ofC("include git folder not found, ignored %s : %s", NMsg.ofStyledPath(sp),ex).asSevere(), NTxUtils.sourceOf(context.node()));
                        }
                    } else {
                        NPath p = NPath.of(sp);
                        if (p.isAbsolute()) {
                            if (p.exists()) {
                                return u;
                            }
                        } else {
                            //should we not get some engine context?
                            if (p.exists()) {
                                return u;
                            }
                        }
                        context.log().log(NMsg.ofC("include folder not found, ignored %s", p).asSevere(), NTxUtils.sourceOf(context.node()));
                    }
                }
            }
        }
        return NElement.ofNull();
    }
}
