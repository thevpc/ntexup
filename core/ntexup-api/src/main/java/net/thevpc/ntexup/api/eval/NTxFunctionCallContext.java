package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.function.Function;
import java.util.function.Supplier;

public interface NTxFunctionCallContext {
    int size();

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
