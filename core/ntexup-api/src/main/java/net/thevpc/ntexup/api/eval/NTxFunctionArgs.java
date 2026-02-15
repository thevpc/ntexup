package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.function.Function;
import java.util.function.Supplier;

public interface NTxFunctionArgs {
    int size();

    NTxFunctionArg[] args();

    NTxFunctionArg arg(int index);

    NElement src(int index);

    NElement[] eval();

    NElement eval(int index);

    <T> T eval(int index, Function<NElement, NOptional<T>> converter, String converterName, Supplier<T> whenError);

    boolean checkTooFewArgs(int minArgs);

    boolean checkTooManyArgs(int maxArgs);
}
