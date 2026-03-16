/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.eval;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
public class NTxNodeEval implements NTxObjectEvalContext {

    private final NTxResolutionContext context;

    public NTxNodeEval(NTxResolutionContext context) {
        this.context = context;
    }

    public NElement evalVar(String varName) {
        NOptional<NTxVar> v = context.getVar(varName);
        if (!v.isPresent()) {
            context.engine().log().log(NMsg.ofC("var not found %s", varName).asWarning(), context.parent().source());
            return null;
        }
        return v.get().get();
    }


    public NElement evalArray(NElement element, NElement[] indices) {
        if (element == null || indices.length == 0) return NElement.ofNull();
        NElement current = element;
        for (int k = 0; k < indices.length; k++) {
            // Evaluate the current index in the loop
            NElement u = eval(indices[k]);
            NOptional<Integer> i = NTxValue.of(u).asInt();
            if (!i.isPresent()) return NElement.ofNull();

            int ii0 = i.get();
            int ii = ii0;

            // Try to see the current level as an array
            NOptional<Object[]> asObjectArray = NTxValue.of(current).asObjectArray();
            if (!asObjectArray.isPresent()) {
                // Die Never: User tried to index into something that isn't an array
                context.engine().log().log(NMsg.ofC("Cannot index into non-array element: %s",
                        current), NTxUtils.sourceOf(context.node()));
                return NElement.ofNull();
            }

            Object[] obj = asObjectArray.get();
            int len = obj.length;

            // Apply 1-based or 0-based logic based on the CURRENT element's metadata
            if (NTxUtils.isOneIndexed(current)) {
                if (ii > 0) ii--;
                else if (ii < 0) ii = len + ii;
                else ii = -1; // Force 0 to fail
            } else {
                if (ii < 0) ii = len + ii;
            }

            // Check bounds for this specific level
            if (ii >= 0 && ii < len) {
                current = (NElement) obj[ii];
                // If there are more indices, the loop continues using 'current'
            } else {
                // Log and break
                String msg = NTxUtils.isOneIndexed(current) ? "invalid (one based) index" : "invalid index";
                context.engine().log().log(NMsg.ofC("%s %s for array of length %s",
                        msg, ii0, len), NTxUtils.sourceOf(context.node()));
                return NElement.ofNull();
            }
        }

        return current; // Return the final leaf element
    }

    @Override
    public NElement eval(NElement elementExpr) {
        if (elementExpr == null) {
            return NElement.ofNull();
        }
        switch (elementExpr.type()) {
            case BACKTICK_STRING:
            case SINGLE_QUOTED_STRING:
            case DOUBLE_QUOTED_STRING:
            case TRIPLE_BACKTICK_STRING:
            case TRIPLE_SINGLE_QUOTED_STRING:
            case TRIPLE_DOUBLE_QUOTED_STRING:
            case LINE_STRING:
            case BLOCK_STRING: {
                String u = elementExpr.asStringValue().get();
                if (u.indexOf("$") >= 0) {
                    NPrimitiveElementBuilder b = elementExpr.asPrimitive().get().builder();
                    b.setString(NMsg.ofV(u, new Function<String, Object>() {
                        @Override
                        public Object apply(String s) {
                            NElement ss = evalVar(s);
                            if (ss != null) {
                                ss = NTxUtils.removeCompilerDeclarationPathAnnotations(ss);
                            }
                            if (ss != null && ss.isAnyString()) {
                                return ss.asStringValue().get();
                            }
                            return ss;
                        }
                    }).toString());
                    return b.build();
                }
                return elementExpr;

            }
            case NAME: {
                String u = elementExpr.asStringValue().get();
                NOptional<NTxVar> vv = context.getVar(u);
                if (vv.isPresent()) {
                    return vv.get().get();
                }
                // not a variable, perhaps some enum value like red, south; etc...?
                return elementExpr;
            }
            case NAMED_UPLET: {
                NUpletElement ff = ((NUpletElement) elementExpr);
                String functionName = ff.name().get();
                NTxFunctionCallContext args = context.engine().createFunctionArgs(functionName, ff.params().toArray(new NElement[0]), context);
                NOptional<NTxFunction> f = context.getFunction(functionName/*, args.args()*/);
                if (f.isPresent()) {
                    NElement u = f.get().invoke(args);
                    if(u.equals(args.callExpression())){
                        // function could not be evaluated in the current context
                        // perhaps needs some timeout or rendering context or....
                        return u;
                    }
                    return eval(u);
                }else{
                    if(context.inPage()){
                        context.engine().log().log(NMsg.ofC("unsupported function %s in %s", functionName,ff).asError(), context.source());
                    }
                }
                List<NElement> r = ff.params()
                        .stream().map(x -> eval(x)).collect(Collectors.toList());
                return ff.builder().setParams(r).build();
            }
            case EMPTY: {
                return elementExpr;
            }
            case FLAT_EXPR: {
                NFlatExprElement ff1 = ((NFlatExprElement) elementExpr);
                NElement reshaped = ff1.reshape();
                return eval(reshaped);
            }
            case BINARY_OPERATOR: {
                NBinaryOperatorElement ff1 = ((NBinaryOperatorElement) elementExpr);
                return _evalBinaryOperator(ff1);
            }
            case UNARY_OPERATOR: {
                NUnaryOperatorElement ff1 = ((NUnaryOperatorElement) elementExpr);
                return _evalUnaryOperator(ff1);
            }
            case UPLET: {
                NUpletElement ff = ((NUpletElement) elementExpr);
                if (ff.params().size() == 1) {
                    //this is a plain par
                    return eval(ff.params().get(0));
                }
                List<NElement> r = ff.params()
                        .stream().map(x -> eval(x)).collect(Collectors.toList());
                return ff.builder().setParams(r).build();
            }
            case PAIR: {
                NPairElement ff = ((NPairElement) elementExpr);
                return ff.builder()
                        .key(eval(ff.key()))
                        .value(eval(ff.value()))
                        .build();
            }
            case ARRAY:
            case FULL_ARRAY:
            case PARAM_ARRAY:
            case NAMED_ARRAY: {
                NArrayElement r = elementExpr.asArray().get();
                String u = r.name().orNull();
                if (u != null) {
                    NOptional<NTxVar> v = context.getVar(u);
                    if (v.isPresent()) {
                        NElement arrVal = v.get().get();
                        return evalArray(arrVal, r.children().toArray(new NElement[0]));
                    }
                } else if (u == null) {
                    // this is an implicit array
                    List<NElement> children = r.children();
                    if (children.isEmpty()) {
                        return NElement.ofArray();
                    } else {
                        List<NElement> newChildren = new ArrayList<>();
                        for (NElement c : children) {
                            NElement[] zz = interpretAsArrayItems_interval(c);
                            if (zz != null) {
                                newChildren.addAll(Arrays.asList(zz));
                            } else {
                                newChildren.add(eval(c));
                            }
                        }
                        return NElement.ofArray(newChildren.toArray(new NElement[0]));
                    }
                }
                break;
            }
            case PARAM_OBJECT:
            case OBJECT:
            case NAMED_OBJECT: {
                // this is a complex object
                break;
            }
            default: {
                NElementTypeGroup nElementTypeGroup = elementExpr.type().group();
                if (nElementTypeGroup == NElementTypeGroup.NUMBER || nElementTypeGroup == NElementTypeGroup.NULL || nElementTypeGroup == NElementTypeGroup.STRING || nElementTypeGroup == NElementTypeGroup.BOOLEAN || nElementTypeGroup == NElementTypeGroup.CUSTOM) {

                } else if (nElementTypeGroup == NElementTypeGroup.OPERATOR) {
                    context.engine().log().log(NMsg.ofC("unsupported operator %s in %s", elementExpr.asOperator().get().position(), NTxUtils.snippet(elementExpr)).asWarning(), context.source());
                } else {
                    context.engine().log().log(NMsg.ofC("unsupported expression %s", NTxUtils.snippet(elementExpr)).asWarning(), context.source());
                }
            }
        }
        return elementExpr;
    }

    private NElement _evalUnaryOperator(NUnaryOperatorElement elem) {
        switch (elem.operatorSymbol()) {
            case MINUS: {
                NElement a = eval(elem.operand());
                return NTxEvalUtils.negate(a).orElse(elem);
            }
            case PLUS: {
                return eval(elem.operand());
            }
        }
        context.engine().log().log(NMsg.ofC("unsupported operator %s in %s", elem.asOperator().get().position(), NTxUtils.snippet(elem)).asWarning(), NTxUtils.sourceOf(context.node()));
        return NElement.ofNull();
    }

    private NElement _evalBinaryOperator(NBinaryOperatorElement elem) {
        switch (elem.operatorSymbol()) {
            case MINUS: {
                NElement a = eval(elem.firstOperand());
                NElement b = eval(elem.secondOperand());
                return NTxEvalUtils.substruct(a, b, MathContext.DECIMAL128).orElse(elem);
            }
            case EQ2: {
                NElement a = eval(elem.firstOperand());
                NElement b = eval(elem.secondOperand());
                return NTxEvalUtils.eq(a, b);
            }
            case REM: {
                NElement a = eval(elem.firstOperand());
                NElement b = eval(elem.secondOperand());
                return NTxEvalUtils.remainder(a, b, MathContext.DECIMAL128).orElse(elem);
            }
            case PLUS: {
                NElement a = eval(elem.firstOperand());
                NElement b = eval(elem.secondOperand());
                return NTxEvalUtils.add(a, b, MathContext.DECIMAL128).orElse(elem);
            }
            case MUL: {
                NElement a = eval(elem.firstOperand());
                NElement b = eval(elem.secondOperand());
                return NTxEvalUtils.mul(a, b, MathContext.DECIMAL128).orElse(elem);
            }
            case DIV: {
                NElement a = eval(elem.firstOperand());
                NElement b = eval(elem.secondOperand());
                return NTxEvalUtils.div(a, b, MathContext.DECIMAL128).orElse(elem);
            }
        }
        context.engine().log().log(NMsg.ofC("unsupported operator %s in %s", elem.asOperator().get().position(), NTxUtils.snippet(elem)).asWarning(), NTxUtils.sourceOf(context.node()));
        return NElement.ofNull();
    }

    private NElement[] interpretAsArrayItems_interval(NElement c) {
        if (c.isBinaryOperator()
                && c.asBinaryOperator().get().operatorSymbol() == NOperatorSymbol.MINUS_GT) {
            NBinaryOperatorElement g = c.asBinaryOperator().get();
            NElement f = eval(g.firstOperand());
            NElement s = eval(g.secondOperand());
            return NTxEvalUtils.evalInterval(f, s);
        }
        return null;
    }

}
