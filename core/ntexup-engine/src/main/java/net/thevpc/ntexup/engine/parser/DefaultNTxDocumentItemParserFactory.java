package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.parser.NTxNodeParserFactory;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeCall;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeName;
import net.thevpc.ntexup.engine.document.NTxItemBag;
import net.thevpc.nuts.concurrent.DefaultNScorableCallable;
import net.thevpc.nuts.concurrent.NScorableCallable;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.spi.NScorableContext;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author vpc
 */
public class DefaultNTxDocumentItemParserFactory
        implements NTxNodeParserFactory {

    public DefaultNTxDocumentItemParserFactory() {
    }

    @Override
    public NScorableCallable<NTxItem> parseNode(NTxNodeFactoryParseContext context) {
        NElement c = context.element();
        NTxEngine engine = context.engine();
        if (c.annotations().stream().anyMatch(x -> NTxNodeType.CTRL_DEFINE.equals(x.name()))) {
            //this is a node definition
            if (c.isAnyObject() || c.isNamed()) {
                return NScorableCallable.ofValid(() -> {
                    NObjectElement object = c.asObject().get();
                    String templateName = object.name().get();
                    List<NTxNodeDefParam> params;
                    if (object.isParametrized()) {
                        params = object.asParametrizedContainer().get().params().get()
                                .stream().map(x -> {
                                    if (x.isNamedPair()) {
                                        NPairElement p = x.asPair().get();
                                        return new NTxNodeDefParamImpl(
                                                p.key().asStringValue().get(),
                                                p.value()
                                        );
                                    } else if (x.isName()) {
                                        return new NTxNodeDefParamImpl(
                                                x.asStringValue().get(),
                                                null
                                        );
                                    } else {
                                        context.messages().log(NMsg.ofC("invalid definition param, expected var name %s in %s", x, object).asError());
                                        return null;
                                    }
                                }).filter(x -> x != null).collect(Collectors.toList());
                    } else {
                        params = new ArrayList<>();
                    }
                    NTxSource source = context.source();
                    List<NTxNode> defBody=new ArrayList<>();
                    for (NElement child : object.children()) {
                        NTxItem item = engine.newNode(child, context).get();
                        NTxItemBag b=new NTxItemBag(Arrays.asList(item));
                        if(b.isNodes()){
                            defBody.addAll(b.nodes());
                        }else{
                            context.messages().log(NMsg.ofC("expected nodes, but got other items when creating node from %s", NTxUtils.snippet(child)).asError());
                        }
                    }
                    return new NTxNodeDefImpl(
                            context.node(),
                            templateName,
                            params.toArray(new NTxNodeDefParam[0]),
                            defBody.toArray(new NTxNode[0]),
                            source
                    );
                });
            } else {
                return _invalidSupport(NMsg.ofC("invalid defineNode syntax, expected @define <NAME>(...){....}"), context);
            }
        }

        switch (c.type().typeGroup()) {
            case OPERATOR: {
                switch (c.type()) {
                    case OP_EQ: {
                        NOperatorElement p = c.asOperator().get();
                        NElement k = p.first().get();
                        NElement v = p.second().get();
                        NTxValue kh = NTxValue.of(k);
                        if (k.isName()) {
                            NOptional<String> nn = kh.asStringOrName();
                            if (nn.isPresent()) {
                                String nnn = NStringUtils.trim(nn.get());
                                return NScorableCallable.ofValid(() -> DefaultNTxNode.ofAssign(nnn, v, context.source()));
                            } else {
                                return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
                            }
                        } else {
                            return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
                        }
                    }
                    case OP_COLON_EQ: {
                        NOperatorElement p = c.asOperator().get();
                        NElement k = p.first().get();
                        NElement v = p.second().get();
                        NTxValue kh = NTxValue.of(k);
                        if (k.isName()) {
                            NOptional<String> nn = kh.asStringOrName();
                            if (nn.isPresent()) {
                                String nnn = NStringUtils.trim(nn.get());
                                return NScorableCallable.ofValid(() -> DefaultNTxNode.ofAssignIfEmpty(nnn, v, context.source()));
                            } else {
                                return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
                            }
                        } else {
                            return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
                        }
                    }
                }
                NOptional<NTxNodeParser> ff = engine.nodeTypeParser(c.type().opSymbol());
                if (ff.isPresent()) {
                    NScorableCallable<NTxItem> uu = ff.get().parseNode(context);
                    if (uu.isValid(NScorableContext.of())) {
                        return uu;
                    }
                }
                break;
            }
            case CONTAINER: {
                switch (c.type()) {
                    case OBJECT:
                    case ARRAY:
                    {
                        return parseNoNameBloc(context);
                    }
                    case UPLET: {
                        NTxNodeParser p = engine.nodeTypeParser(NTxNodeType.TEXT).orNull();
                        return p.parseNode(context);
                    }
                    case NAMED_PARAMETRIZED_OBJECT:
                    case NAMED_OBJECT:
                    case NAMED_UPLET:
                    case NAMED_PARAMETRIZED_ARRAY:
                    case NAMED_ARRAY: {
                        NTxValue ee = NTxValue.of(c);
                        String uid = NTxUtils.uid(ee.name());
                        NTxNodeParser p = engine.nodeTypeParser(uid).orNull();
                        if (p != null) {
                            return p.parseNode(context);
                        }
                        if (c.isNamedUplet() || c.isAnyObject()) {
                            return NScorableCallable.ofValid(() -> createCtrlNodeCall(c, context));
                        }
                        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
                    }
                    case PAIR: {
                        if (c.isNamedPair()) {
                            NPairElement p = c.asPair().get();
                            String name = p.key().asStringValue().get();
                            NOptional<NTxNodeParser> ff = engine.nodeTypeParser(name);
                            if (ff.isPresent()) {
                                NScorableCallable<NTxItem> uu = ff.get().parseNode(context);
                                if (uu.isValid(NScorableContext.of())) {
                                    return uu;
                                }
                            }
                        }
                        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node from pair : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
                    }
                    default: {
                        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node from %s : %s", c.type().id(), NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
                    }
                }
            }
            case NUMBER:
            case TEMPORAL:
            case STRING:
            case REGEX:
            case NULL:
            case BOOLEAN: {
                NTxNodeParser p = engine.nodeTypeParser(NTxNodeType.TEXT).orNull();
                if (p != null) {
                    return p.parseNode(context);
                }
                break;
            }
            case NAME: {
                return NScorableCallable.ofValid(()->new CtrlNTxNodeName(context.source(),c));
            }
        }
        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
    }

    private NScorableCallable<NTxItem> _invalidSupport(NMsg msg, NTxNodeFactoryParseContext context) {
        msg = msg.asError();
        context.messages().log(msg.asError());
        return NScorableCallable.ofInvalid(msg);
    }


    private CtrlNTxNodeCall createCtrlNodeCall(NElement c, NTxNodeFactoryParseContext context) {
        CtrlNTxNodeCall cc = new CtrlNTxNodeCall(context.source());
        NTxSource source = context.source();
        String __name = c.asNamed().get().name().get();
        cc.setProperty(NTxPropName.NAME, NTxUtils.addCompilerDeclarationPath(NElement.ofString(NTxUtils.uid(__name)), context.source()));
        List<NElement> __callBody = new ArrayList<>();
        List<NElement> __args = new ArrayList<>();
        Map<String, NElement> __bodyVars = new HashMap<>();

        //inline current file path in the NElements
        if (source != null && source.path().orNull() != null) {
            NPath sourcePath = source.path().orNull();
            c = NTxUtils.addCompilerDeclarationPath(c, sourcePath.toString());
            if (c.isNamedUplet()) {
                NUpletElementBuilder fb = (NUpletElementBuilder) c.builder();
                for (int i = 0; i < fb.params().size(); i++) {
                    NElement u = NTxUtils.addCompilerDeclarationPath(fb.get(i).orNull(), sourcePath.toString());
                    fb.set(i, u);
                    __args.add(u);
                }
                c = fb.build();
            } else if (c.isAnyObject()) {
                NObjectElementBuilder fb = (NObjectElementBuilder) c.builder();
                List<NElement> args = fb.params().orNull();
                if (args != null) {
                    for (int i = 0; i < args.size(); i++) {
                        NElement u = NTxUtils.addCompilerDeclarationPath(args.get(i), sourcePath.toString());
                        fb.setParamAt(i, u);
                        __args.add(u);
                    }
                }
                for (NElement child : fb.children()) {
                    if (child.isNamedPair() && child.asPair().get().key().isAnnotated("let")) {
                        NPairElement pe = child.asPair().get();
                        __bodyVars.put(pe.key().asStringValue().get(), pe.value());
                    } else {
                        __callBody.add(child);
                    }
                }
                c = fb.build();
            } else {
                context.messages().log(NMsg.ofC("unexpected call : %s (ignored)", c).asError(), context.source());
            }
        } else {
            if (c.isNamedUplet()) {
                NUpletElementBuilder fb = (NUpletElementBuilder) c.builder();
                for (int i = 0; i < fb.params().size(); i++) {
                    __args.add(fb.get(i).orNull());
                }
            } else if (c.isAnyObject()) {
                NObjectElementBuilder fb = (NObjectElementBuilder) c.builder();
                List<NElement> args = fb.params().orNull();
                if (args != null) {
                    __args.addAll(args);
                }
                for (NElement child : fb.children()) {
                    if (child.isNamedPair() && child.asPair().get().key().isAnnotated("let")) {
                        NPairElement pe = child.asPair().get();
                        __bodyVars.put(pe.key().asStringValue().get(), pe.value());
                    } else {
                        __callBody.add(child);
                    }
                }
            } else {
                context.messages().log(NMsg.ofC("unexpected call : %s (ignored)", c).asError(), context.source());
            }
        }
        cc.setCallName(__name);
        cc.setCallBody(__callBody);
        cc.setArgs(__args);
        cc.setCallExpr(c);
        cc.setBodyVars(__bodyVars);
        cc.setProperty(NTxPropName.VALUE, c);
        cc.setSource(context.source());
        cc.setParent(context.node());
        return cc;
    }

    private boolean isRootBloc(NTxNodeFactoryParseContext context) {
        NTxNode[] nodes = context.nodePath();
        if (nodes.length == 0) {
            return true;
        }
        if (nodes.length > 1) {
            return false;
        }
        if (nodes.length == 1) {
            if (!Objects.equals(nodes[0].type(), NTxNodeType.PAGE_GROUP)) {
                return false;
            }
        }
        NElement c = context.element();
//        HEngine engine = context.engine();
        for (NElementAnnotation a : c.annotations()) {
            String nn = a.name();
            if (!NBlankable.isBlank(nn)) {
                return false;
            }
            boolean foundNtexup = false;
            boolean foundVersion = false;
            boolean foundOther = false;
            List<NElement> params = a.params();
            if (params != null) {
                for (NElement cls : params) {
                    if (cls.isAnyString()) {
                        if (cls.asStringValue().get().equalsIgnoreCase("ntexup")) {
                            foundNtexup = true;
                        } else if (isVersionString(cls.asStringValue().get())) {
                            foundVersion = true;
                        } else {
                            foundOther = true;
                        }
                    } else {
                        if (cls.type().isAnyNumber()) {
                            BigDecimal bi = cls.asNumber().get().bigDecimalValue();
                            foundVersion = true;
                        } else {
                            foundOther = true;
                        }
                        break;

                    }
                }
            }
            if (foundNtexup) {
                return true;
            }
        }
        return false;
    }

    private boolean isVersionString(String value) {
        if (value != null) {
            value = value.trim();
            if (value.length() > 0) {
                if (value.charAt(0) >= '0' && value.charAt(0) >= '9') {
                    for (char c : value.toCharArray()) {
                        if (!Character.isAlphabetic(c) && !Character.isDigit(c)
                                && c != '.' && c != '_' && c != '-'

                        ) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private NScorableCallable<NTxItem> parseNoNameBloc(NTxNodeFactoryParseContext context) {
        NElement c = context.element();
        NTxEngine engine = context.engine();
        NTxDocumentFactory f = engine.documentFactory();
        HashSet<String> allAncestors = null;
        HashSet<String> allStyles = null;
        NTxValue ee = NTxValue.of(c);
        for (NElementAnnotation a : c.annotations()) {
            String nn = a.name();
            if (!NNameFormat.equalsIgnoreFormat(nn, NTxUtils.COMPILER_DECLARATION_PATH)) {
                if (!NBlankable.isBlank(nn)) {
                    if (allAncestors == null) {
                        allAncestors = new HashSet<>();
                    }
                    allAncestors.add(NTxUtils.uid(nn));
                }
                // add classes as well
                List<NElement> params = a.params();
                if (params != null) {
                    for (NElement cls : params) {
                        if (allStyles == null) {
                            allStyles = new HashSet<>();
                        }
                        NOptional<String[]> ss = NTxValue.of(cls).asStringArrayOrString();
                        if (ss.isPresent()) {
                            allStyles.addAll(Arrays.asList(ss.get()));
                        }
                    }
                }
            }
        }
        NTxNode node = context.node();
        if ((allStyles != null || allAncestors != null) && !isRootBloc(context)) {
            NTxNode pg = f.ofGroup().setSource(context.source());
            pg.setStyleClasses(allStyles == null ? null : allStyles.toArray(new String[0]));
            for (NElement child : ee.body()) {
                NOptional<NTxItem> u = context.engine().newNode(child, context);
                if (u.isPresent()) {
                    pg.append(u.get());
                } else {
                    return _invalidSupport(NMsg.ofC("invalid %s for %s", NTxUtils.snippet(child),
                            node == null ? "document" : node.type()
                    ), context);
                }
            }
            return NScorableCallable.ofValid(pg);
        } else {
            NTxItemList pg = new NTxItemList();
            for (NElement child : ee.body()) {
                NOptional<NTxItem> u = context.engine().newNode(child, context);
                if (u.isPresent()) {
                    pg.add(u.get());
                } else {
                    return _invalidSupport(NMsg.ofC("invalid %s for %s : %s", NTxUtils.snippet(child),
                            node == null ? "document" : node.type(),
                            u.getMessage().get()
                    ), context);
                }
            }
            return NScorableCallable.ofValid(pg);
        }
    }

}
