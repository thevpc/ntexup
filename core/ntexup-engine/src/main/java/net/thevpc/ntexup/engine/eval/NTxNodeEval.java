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

    private NTxResolutionContext context;

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
                        current),NTxUtils.sourceOf(context.node()));
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
        if (elementExpr instanceof NElement) {
            NElement ee = ((NElement) elementExpr);
            switch (ee.type()) {
                case BACKTICK_STRING:
                case SINGLE_QUOTED_STRING:
                case DOUBLE_QUOTED_STRING:
                case TRIPLE_BACKTICK_STRING:
                case TRIPLE_SINGLE_QUOTED_STRING:
                case TRIPLE_DOUBLE_QUOTED_STRING:
                case LINE_STRING: {
                    String u = ee.asStringValue().get();
                    if (u.indexOf("$") >= 0) {
                        NPrimitiveElementBuilder b = ee.asPrimitive().get().builder();
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
                    return ee;

                }
                case NAME: {
                    String u = ee.asStringValue().get();
                    NOptional<NTxVar> vv = context.getVar(u);
                    if (vv.isPresent()) {
                        return vv.get().get();
                    }
                    // not a variable, perhaps some enum value like red, south; etc...?
                    return ee;
                }
                case NAMED_UPLET: {
                    NUpletElement ff = ((NUpletElement) elementExpr);
                    String functionName = ff.name().get();
                    NTxFunctionArgsImpl args = new NTxFunctionArgsImpl(functionName, ff.params().toArray(new NElement[0]), context);
                    NOptional<NTxFunction> f = context.getFunction(functionName/*, args.args()*/);
                    if (f.isPresent()) {
                        return eval(f.get().invoke(args, context));
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
                    switch (ff1.operatorSymbol()) {
                        case MINUS: {
                            NBinaryOperatorElement ff = ((NBinaryOperatorElement) elementExpr);
                            if (ff.isBinaryOperator()) {
                                NElement a = eval(ff.firstOperand());
                                NElement b = eval(ff.secondOperand());
                                return NTxEvalUtils.substruct(a, b);
                            } else if (ff.isUnaryOperator()) {
                                NElement a = ff.firstOperand();
                                return NTxEvalUtils.negate(a);
                            } else {
                                return ff;
                            }
                        }
                        case EQ2: {
                            NBinaryOperatorElement ff = ((NBinaryOperatorElement) elementExpr);
                            if (ff.isBinaryOperator()) {
                                NElement a = eval(ff.firstOperand());
                                NElement b = eval(ff.secondOperand());
                                return NTxEvalUtils.eq(a, b);
                            } else {
                                return ff;
                            }
                        }
                        case REM: {
                            NBinaryOperatorElement ff = ((NBinaryOperatorElement) elementExpr);
                            if (ff.isBinaryOperator()) {
                                NElement a = eval(ff.firstOperand());
                                NElement b = eval(ff.secondOperand());
                                return NTxEvalUtils.remainder2(a, b);
                            } else {
                                return ff;
                            }
                        }
                        case PLUS: {
                            NBinaryOperatorElement ff = ((NBinaryOperatorElement) elementExpr);
                            if (ff.isBinaryOperator()) {
                                NElement a = eval(ff.firstOperand());
                                NElement b = eval(ff.secondOperand());
                                return NTxEvalUtils.add(a, b);
                            } else if (ff.isUnaryOperator()) {
                                NElement a = eval(ff.firstOperand());
                                return a;
                            } else {
                                return ff;
                            }
                        }
                        case MUL: {
                            NBinaryOperatorElement ff = ((NBinaryOperatorElement) elementExpr);
                            if (ff.isBinaryOperator()) {
                                NElement a = eval(ff.firstOperand());
                                NElement b = eval(ff.secondOperand());
                                return NTxEvalUtils.mul(a, b, MathContext.DECIMAL128);
                            } else {
                                return ff;
                            }
                        }
                        case DIV: {
                            NBinaryOperatorElement ff = ((NBinaryOperatorElement) elementExpr);
                            if (ff.isBinaryOperator()) {
                                NElement a = eval(ff.firstOperand());
                                NElement b = eval(ff.secondOperand());
                                return NTxEvalUtils.div(a, b, MathContext.DECIMAL128);
                            } else {
                                return ff;
                            }
                        }
                    }
                    context.engine().log().log(NMsg.ofC("unsupported operator %s in %s", ee.asOperator().get().position(), NTxUtils.snippet(ee)).asWarning(), NTxUtils.sourceOf(context.node()));
                    return NElement.ofNull();
                }
                case UPLET: {
                    NUpletElement ff = ((NUpletElement) elementExpr);
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
                    NArrayElement r = ee.asArray().get();
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
                    NElementTypeGroup nElementTypeGroup = ee.type().group();
                    if (nElementTypeGroup == NElementTypeGroup.NUMBER || nElementTypeGroup == NElementTypeGroup.NULL || nElementTypeGroup == NElementTypeGroup.STRING || nElementTypeGroup == NElementTypeGroup.BOOLEAN || nElementTypeGroup == NElementTypeGroup.CUSTOM) {

                    } else if (nElementTypeGroup == NElementTypeGroup.OPERATOR) {
                        context.engine().log().log(NMsg.ofC("unsupported operator %s in %s", ee.asOperator().get().position(), NTxUtils.snippet(ee)).asWarning(), context.source());
                    } else {
                        context.engine().log().log(NMsg.ofC("unsupported expression %s", NTxUtils.snippet(ee)).asWarning(), context.source());
                    }
                }
            }
        }
        return elementExpr;
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

//    public NOptional<NTxNode> findNodeByProperty(String propertyName, String propertyValue) {
//        NTxItem nn = (node);
//        while (nn != null) {
//            if (nn instanceof NTxNode) {
//                NTxNode nd = (NTxNode) nn;
//                NOptional<NElement> v = nd.getPropertyValue(propertyName);
//                if (v.isPresent()) {
//                    NElement vv = v.get();
//                    String vvn = vv.asStringValue().orNull();
//                    if (vvn != null && vvn.equals(propertyValue)) {
//                        return NOptional.of(nd);
//                    }
//                }
//                if (NTxUtils.isComponentBody(propertyName)) {
//                    if (nd.templateDefinition() != null) {
//                        // do not go up in hierarchy
//                        break;
//                    }
//                }
//            }
//            nn = nn.parent();
//        }
//        return NOptional.ofNamedEmpty("node with propertyName " + propertyName);
//    }
}
