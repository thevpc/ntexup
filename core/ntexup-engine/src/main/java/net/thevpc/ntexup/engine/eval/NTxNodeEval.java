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

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
public class NTxNodeEval implements NTxObjectEvalContext {

    private NTxEngine engine;
    private NTxVarProvider varProvider;

    public NTxNodeEval(NTxEngine engine, NTxVarProvider varProvider) {
        this.engine = engine;
        this.varProvider = varProvider;
    }

    @Override
    public NOptional<NTxVar> findVar(String varName, NTxNode node) {
        if (varProvider != null) {
            NOptional<NTxVar> r = varProvider.findVar(varName, node);
            if (r != null && r.isPresent()) {
                return r;
            }
        }
        NTxItem nn = (node);
        while (nn != null) {
            if (nn instanceof NTxNode) {
                NTxNode nd = (NTxNode) nn;
                NOptional<NElement> v = nd.getVar(varName);
                if (v.isPresent()) {
                    return NTxVarImpl.ofOptional(varName, () -> v.get());
                }
                if (NTxUtils.isAnyDefVarName(varName)) {
                    if (nd.templateDefinition() != null) {
                        // do not go up in hierarchy
                        break;
                    }
                }
            }
            nn = nn.parent();
        }
        switch (varName) {
            case "HOME": {
                return NTxVarImpl.ofOptional(varName, () -> NElement.ofString(System.getProperty("user.home")));
            }
            case "USERNAME": {
                return NTxVarImpl.ofOptional(varName, () -> NElement.ofString(System.getProperty("user.name")));
            }
        }
        String v = System.getProperty(varName);
        if (v != null) {
            return NTxVarImpl.ofOptional(varName, () -> NElement.ofString(System.getProperty(varName)));
        }
        return NOptional.ofNamedEmpty("var " + varName);
    }

    public NElement evalVar(NTxNode node, String varName) {
        NOptional<NTxVar> v = findVar(varName, node);
        if (v.isPresent()) {
            return v.get().get();
        }
        engine.log().log(NMsg.ofC("var not found %s", varName).asWarning(), NTxUtils.sourceOf(node));
        return null;
    }

    public NElement evalArray(NTxNode node, NElement element, NElement[] indices) {
        if (element == null) {
            return NElement.ofNull();
        }
        if (indices.length == 0) {
            return element;
        }
        NElement u = eval(indices[0], node);
        NOptional<Integer> i = NTxValue.of(u).asInt();
        if (i.isPresent()) {
            int ii = i.get();
            NOptional<Object[]> asObjectArray = NTxValue.of(element).asObjectArray();
            if (asObjectArray.isPresent()) {
                Object[] obj = asObjectArray.get();
                int len = obj.length;
                if (ii == 0) {
                    ii = 1;
                }
                if (ii < 0) {
                    ii = len + len;
                }
                if (ii - 1 >= 0 && ii - 1 < len) {
                    return (NElement) obj[ii - 1];
                }
            }
        }
        return null;
    }

    @Override
    public NElement eval(NElement elementExpr, NTxNode node) {
        if (elementExpr == null) {
            return NElement.ofNull();
        }
        if (elementExpr instanceof NElement) {
            NElement ee = ((NElement) elementExpr);
            switch (ee.type()) {
                case ANTI_QUOTED_STRING:
                case SINGLE_QUOTED_STRING:
                case DOUBLE_QUOTED_STRING:
                case TRIPLE_ANTI_QUOTED_STRING:
                case TRIPLE_SINGLE_QUOTED_STRING:
                case TRIPLE_DOUBLE_QUOTED_STRING:
                case LINE_STRING: {
                    String u = ee.asStringValue().get();
                    if (u.indexOf("$") >= 0) {
                        NPrimitiveElementBuilder b = ee.asPrimitive().get().builder();
                        b.setString(NMsg.ofV(u, new Function<String, Object>() {
                            @Override
                            public Object apply(String s) {
                                NElement ss = evalVar(node, s);
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
                    NOptional<NTxVar> vv = findVar(u, node);
                    if (vv.isPresent()) {
                        return vv.get().get();
                    }
                    // not a variable, perhaps some enum value like red, south; etc...?
                    return ee;
                }
                case NAMED_UPLET: {
                    NUpletElement ff = ((NUpletElement) elementExpr);
                    NTxFunctionArgsImpl args = new NTxFunctionArgsImpl(ff.params(), node, engine, varProvider);
                    NOptional<NTxFunction> f = engine.findFunction(node, ff.name().get(), args.args());
                    if (f.isPresent()) {
                        return eval(f.get().invoke(
                                args
                                , new DefaultNTxFunctionContext(engine, node, varProvider)), node);
                    }
                    List<NElement> r = ff.params()
                            .stream().map(x -> eval(x, node)).collect(Collectors.toList());
                    return ff.builder().setParams(r).build();
                }
                case OP_MINUS: {
                    NOperatorElement ff = ((NOperatorElement) elementExpr);
                    if (ff.isBinaryOperator()) {
                        NElement a = eval(ff.first().get(), node);
                        NElement b = eval(ff.second().get(), node);
                        return NTxEvalUtils.substruct(a, b);
                    } else if (ff.isUnaryOperator()) {
                        NElement a = ff.first().get();
                        return NTxEvalUtils.negate(a);
                    } else {
                        return ff;
                    }
                }
                case OP_EQ2: {
                    NOperatorElement ff = ((NOperatorElement) elementExpr);
                    if (ff.isBinaryOperator()) {
                        NElement a = eval(ff.first().get(), node);
                        NElement b = eval(ff.second().get(), node);
                        return NTxEvalUtils.eq(a,b);
                    } else {
                        return ff;
                    }
                }
                case OP_REM: {
                    NOperatorElement ff = ((NOperatorElement) elementExpr);
                    if (ff.isBinaryOperator()) {
                        NElement a = eval(ff.first().get(), node);
                        NElement b = eval(ff.second().get(), node);
                        return NTxEvalUtils.remainder2(a,b);
                    } else {
                        return ff;
                    }
                }
                case OP_PLUS: {
                    NOperatorElement ff = ((NOperatorElement) elementExpr);
                    if (ff.isBinaryOperator()) {
                        NElement a = eval(ff.first().get(), node);
                        NElement b = eval(ff.second().get(), node);
                        return NTxEvalUtils.add(a, b);
                    } else if (ff.isUnaryOperator()) {
                        NElement a = eval(ff.first().get(), node);
                        return a;
                    } else {
                        return ff;
                    }
                }
                case OP_MUL: {
                    NOperatorElement ff = ((NOperatorElement) elementExpr);
                    if (ff.isBinaryOperator()) {
                        NElement a = eval(ff.first().get(), node);
                        NElement b = eval(ff.second().get(), node);
                        return NTxEvalUtils.mul(a, b, MathContext.DECIMAL128);
                    } else {
                        return ff;
                    }
                }
                case OP_DIV: {
                    NOperatorElement ff = ((NOperatorElement) elementExpr);
                    if (ff.isBinaryOperator()) {
                        NElement a = eval(ff.first().get(), node);
                        NElement b = eval(ff.second().get(), node);
                        return NTxEvalUtils.div(a, b, MathContext.DECIMAL128);
                    } else {
                        return ff;
                    }
                }
                case UPLET: {
                    NUpletElement ff = ((NUpletElement) elementExpr);
                    List<NElement> r = ff.params()
                            .stream().map(x -> eval(x, node)).collect(Collectors.toList());
                    return ff.builder().setParams(r).build();
                }
                case PAIR: {
                    NPairElement ff = ((NPairElement) elementExpr);
                    return ff.builder()
                            .key(eval(ff.key(), node))
                            .value(eval(ff.value(), node))
                            .build();
                }
                case ARRAY:
                case NAMED_PARAMETRIZED_ARRAY:
                case PARAMETRIZED_ARRAY:
                case NAMED_ARRAY: {
                    NArrayElement r = ee.asArray().get();
                    String u = r.name().orNull();
                    if (u != null) {
                        NOptional<NTxVar> v = findVar(u, node);
                        if (v.isPresent()) {
                            NElement arrVal = v.get().get();
                            return evalArray(node, arrVal, r.children().toArray(new NElement[0]));
                        }
                    } else if (u == null) {
                        // this is an implicit array
                        List<NElement> children = r.children();
                        if (children.isEmpty()) {
                            return NElement.ofArray();
                        } else {
                            List<NElement> newChildren = new ArrayList<>();
                            for (NElement c : children) {
                                NElement[] zz = interpretAsArrayItems_interval(node, c);
                                if (zz != null) {
                                    newChildren.addAll(Arrays.asList(zz));
                                } else {
                                    newChildren.add(eval(c, node));
                                }
                            }
                            return NElement.ofArray(newChildren.toArray(new NElement[0]));
                        }
                    }
                    break;
                }
                case PARAMETRIZED_OBJECT:
                case OBJECT:
                case NAMED_OBJECT:
                {
                    // this is a complex object
                    break;
                }
                default: {
                    NElementTypeGroup nElementTypeGroup = ee.type().typeGroup();
                    if (nElementTypeGroup == NElementTypeGroup.NUMBER || nElementTypeGroup == NElementTypeGroup.NULL || nElementTypeGroup == NElementTypeGroup.STRING || nElementTypeGroup == NElementTypeGroup.BOOLEAN || nElementTypeGroup == NElementTypeGroup.CUSTOM) {

                    } else if (nElementTypeGroup == NElementTypeGroup.OPERATOR) {
                        engine.log().log(NMsg.ofC("unsupported operator %s in %s", ee.asOperator().get().operatorType(), NTxUtils.snippet(ee)).asWarning(), NTxUtils.sourceOf(node));
                    } else {
                        engine.log().log(NMsg.ofC("unsupported expression %s", NTxUtils.snippet(ee)).asWarning(), NTxUtils.sourceOf(node));
                    }
                }
            }
        }
        return elementExpr;
    }

    private NElement[] interpretAsArrayItems_interval(NTxNode node, NElement c) {
        if (c.type() == NElementType.OP_MINUS_GT && c.isBinaryOperator()) {
            NOperatorElement g = c.asOperator().get();
            NElement f = eval(g.first().get(), node);
            NElement s = eval(g.second().get(), node);
            return NTxEvalUtils.evalInterval(f, s);
        }
        return null;
    }

}
