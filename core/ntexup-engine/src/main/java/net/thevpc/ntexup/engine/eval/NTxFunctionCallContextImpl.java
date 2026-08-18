package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class NTxFunctionCallContextImpl implements NTxFunctionCallContext {
    private String functionName;
    private NTxResolutionContext context;
    private NTxFunctionArg[] args;

    public NTxFunctionCallContextImpl(String functionName, NElement[] callArgs, NTxResolutionContext context) {
        this(functionName, Arrays.stream(callArgs).map(x -> new NTxFunctionArgImpl(x, context)).toArray(NTxFunctionArg[]::new), context);
    }

    public NTxFunctionCallContextImpl(String functionName, List<NElement> callArgs, NTxNode node, NTxResolutionContext context) {
        this(functionName, callArgs.stream().map(x -> new NTxFunctionArgImpl(x, context)).toArray(NTxFunctionArg[]::new), context);
    }

    public NTxFunctionCallContextImpl(String functionName, NTxFunctionArg[] args, NTxResolutionContext context) {
        this.args = args;
        this.context = context;
        this.functionName = functionName;
    }

    @Override
    public void log(NMsg message, NTxSource source) {
        scopedContext().log(message,source);
    }

    @Override
    public void log(NMsg message) {
        scopedContext().log(message);
    }

    @Override
    public int size() {
        return args.length;
    }

    @Override
    public NTxFunctionArg[] args() {
        return Arrays.copyOf(args, args.length);
    }

    public NElement callExpression() {
        return NElement.ofTuple(name(),argExpressions());
    }

    @Override
    public NElement[] argExpressions() {
        return Arrays.stream(args).map(x->x.src()).toArray(NElement[]::new);
    }

    @Override
    public NTxFunctionArg arg(int index) {
        return args[index];
    }

    @Override
    public NElement argExpression(int index) {
        if (index < 0 || index >= args.length) {
            context.log(NMsg.ofC("%s: source for arg at %s could not be evaluated : %s", NMsg.ofStyledKeyword(functionName), index,
                    NMsg.ofC("invalid index, should be in [%s...%s]", 0, args.length)).asError());
            return null;
        }
        return args[index].src();
    }

    @Override
    public NElement evalArg(int index) {
        return evalArg(index, NOptional::of, "element", NElement::ofNull);
    }

    @Override
    public boolean checkTooManyArgs(int maxArgs) {
        if (size() > maxArgs) {
            context.log(NMsg.ofC("%s: too many arguments, got %s > %s", NMsg.ofStyledKeyword(functionName), size(), maxArgs).asError());
            return true;
        }
        return false;
    }

    @Override
    public boolean checkTooFewArgs(int minArgs) {
        if (size() < minArgs) {
            context.log(NMsg.ofC("%s: too few arguments, got %s < %s", NMsg.ofStyledKeyword(functionName), size(), minArgs).asError());
            return true;
        }
        return false;
    }

    @Override
    public <T> T evalArg(int index, Function<NElement, NOptional<T>> converter, String convertName, Supplier<T> whenError) {
        T c;
        Supplier<T> safeSupplier = new Supplier<T>() {
            @Override
            public T get() {
                try {
                    if (whenError != null) {
                        return whenError.get();
                    }
                } catch (Exception ex) {
                    context.log(NMsg.ofC("%s: arg at %s could not be evaluated as safe %s : %s", NMsg.ofStyledKeyword(functionName), index, convertName,
                            NMsg.ofC("error evaluating : %s", ex)).asError());
                }
                return null;
            }
        };
        if (index < 0 || index >= args.length) {
            c = safeSupplier.get();
            context.log(NMsg.ofC("%s: arg at %s could not be evaluated as %s : %s", NMsg.ofStyledKeyword(functionName), index, convertName,
                    NMsg.ofC("invalid index, should be in [%s...%s]", 0, args.length)).asError());
            return c;
        }
        NTxFunctionArg arg = args[index];
        NElement arg0;
        try {
            arg0 = args[index].eval();
        } catch (Exception ex) {
            c = safeSupplier.get();
            context.log(NMsg.ofC("%s: arg at %s (as %s) could not be evaluated as %s : %s", NMsg.ofStyledKeyword(functionName), index, arg, convertName,
                    NMsg.ofC("error evaluating : %s", ex)).asError());
            return c;
        }
        NOptional<T> oc;
        try {
            oc = converter.apply(arg0);
        } catch (Exception ex) {
            c = safeSupplier.get();
            context.log(NMsg.ofC("%s: arg at %s (as %s) could not be converted as %s (from %s) : %s", NMsg.ofStyledKeyword(functionName), index, arg, convertName,
                    arg0,
                    NMsg.ofC("error converting : %s", ex)).asError());
            return c;
        }
        if (!oc.isPresent()) {
            c = safeSupplier.get();
            context.log(NMsg.ofC("%s: arg at %s as %s could not be evaluated as %s : %s", NMsg.ofStyledKeyword(functionName), index, arg, convertName,
                    oc.getMessage().get()).asError());
        } else {
            c = oc.get();
        }
        return c;
    }

    public String name(){
        return functionName;
    }
    public NTxResolutionContext scopedContext(){
        return context;
    }

    @Override
    public NElement[] eval() {
        return Arrays.stream(args).map(NTxFunctionArg::eval).toArray(NElement[]::new);
    }
}
