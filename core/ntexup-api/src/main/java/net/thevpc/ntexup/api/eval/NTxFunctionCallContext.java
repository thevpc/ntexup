package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.log.NLogger;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.function.Function;
import java.util.function.Supplier;

public interface NTxFunctionCallContext extends NLogger {
    int size();

    void log(NMsg message, NTxSource source);

    NElement[] argExpressions();

    NElement callExpression();

    NTxFunctionArg[] args();

    NTxFunctionArg arg(int index);

    NElement argExpression(int index);

    NElement[] eval();

    NElement evalArg(int index);

    <T> T evalArg(int index, Function<NElement, NOptional<T>> converter, String converterName, Supplier<T> whenError);

    boolean checkTooFewArgs(int minArgs);

    boolean checkTooManyArgs(int maxArgs);

    String name();

    NTxResolutionContext scopedContext();
}
