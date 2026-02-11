package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.nuts.elem.NElement;

import java.util.Arrays;
import java.util.List;

public class NTxFunctionArgsImpl implements NTxFunctionArgs {
    private NTxFunctionArg[] args;

    public NTxFunctionArgsImpl(NElement[] callArgs, NTxResolutionContext context) {
        this(Arrays.stream(callArgs).map(x -> new NTxFunctionArgImpl(x, context)).toArray(NTxFunctionArg[]::new));
    }
    public NTxFunctionArgsImpl(List<NElement> callArgs, NTxNode node, NTxResolutionContext context) {
        this(callArgs.stream().map(x -> new NTxFunctionArgImpl(x, context)).toArray(NTxFunctionArg[]::new));
    }

    public NTxFunctionArgsImpl(NTxFunctionArg[] args) {
        this.args = args;
    }

    @Override
    public int size() {
        return args.length;
    }

    @Override
    public NTxFunctionArg[] args() {
        return Arrays.copyOf(args, args.length);
    }

    @Override
    public NTxFunctionArg arg(int index) {
        return args[index];
    }

    @Override
    public NElement src(int index) {
        return args[index].src();
    }

    @Override
    public NElement eval(int index) {
        return args[index].eval();
    }

    @Override
    public NElement[] eval() {
        return Arrays.stream(args).map(NTxFunctionArg::eval).toArray(NElement[]::new);
    }
}
