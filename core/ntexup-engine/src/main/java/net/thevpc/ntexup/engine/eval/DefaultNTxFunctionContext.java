//package net.thevpc.ntexup.engine.eval;
//
//import net.thevpc.ntexup.api.document.node.NTxNode;
//import net.thevpc.ntexup.api.engine.NTxEngine;
//import net.thevpc.ntexup.api.eval.NTxCompilePageContext;
//import net.thevpc.ntexup.api.eval.NTxVar;
//import net.thevpc.ntexup.api.log.NTxLogger;
//import net.thevpc.ntexup.api.eval.NTxFunctionContext;
//import net.thevpc.nuts.elem.NElement;
//import net.thevpc.nuts.util.NOptional;
//
//public class DefaultNTxFunctionContext implements NTxFunctionContext {
//    private NTxEngine engine;
//    private NTxNode node;
//    private NTxCompilePageContext context;
//
//    public DefaultNTxFunctionContext(NTxEngine engine, NTxNode node, NTxCompilePageContext context) {
//        this.engine = engine;
//        this.node = node;
//        this.context = context;
//    }
//
//    public NOptional<NElement> eval(NElement expr) {
//        return context.evalExpression(expr);
//    }
//
//    @Override
//    public NOptional<NTxVar> findVar(String varName) {
//        return context.getVar(varName);
//    }
//
//    @Override
//    public NTxNode node() {
//        return node;
//    }
//
//    public NTxEngine engine() {
//        return engine;
//    }
//
//    public NTxLogger log() {
//        return engine.log();
//    }
//}
