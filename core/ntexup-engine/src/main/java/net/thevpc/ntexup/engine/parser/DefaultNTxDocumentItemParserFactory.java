package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.parser.NTxNodeParserFactory;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrNTxNodelUncompiled;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeCall;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeName;
import net.thevpc.ntexup.engine.document.NTxItemBag;
import net.thevpc.ntexup.engine.util.NTxNodeUtils;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NScorableContext;
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
    public NScoredCallable<NTxItem> parseNode(NTxResolutionContext context) {
        NElement c = context.element();
//        NTxEngine engine = context.engine();
        if (c.annotations().stream().anyMatch(x -> NTxNodeType.CTRL_DEFINE.equals(x.name()))) {
            return parseNodeAsDefine(c, context);
        }
        if (c.type() == NElementType.FLAT_EXPR) {
            c = c.asFlatExpression().get().reshape();
        }
        switch (c.type()) {
            case BINARY_OPERATOR: {
                NBinaryOperatorElement bo = c.asBinaryOperator().get();
                switch (bo.operatorSymbol()) {
                    case EQ: {
                        return parseNodeAsOpEq(c, context);
                    }
                    case COLON_EQ: {
                        return parseNodeAsOpColonEq(c, context);
                    }
                }
                return parseNodeAsOpSpecial(c, context);
            }
            case OBJECT:
            case ARRAY: {
                return parseNodeAsNoNameBloc(context);
            }
            case UPLET: {
                return parseNodeAsUplet(context);
            }
            case FULL_OBJECT:
            case NAMED_OBJECT:
            case NAMED_UPLET:
            case FULL_ARRAY:
            case NAMED_ARRAY: {
                return parseNodeAsNamedListContainer(c,context);
            }
            case PAIR: {
                return parseNodeAsPair(c,context);
            }
            case UNORDERED_LIST:
            case ORDERED_LIST: {
                return parseNodeAsListCallable(c, context);
            }
            case EMPTY:{
                return NScoredCallable.ofValid(new NTxItemList());
            }
        }
        switch (c.type().group()) {
            case NUMBER:
            case TEMPORAL:
            case STRING:
            case NULL:
            case BOOLEAN: {
                return parseNodeAsLiteral(c, context);
            }
        }
        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
    }

    private NScoredCallable<NTxItem> parseNodeAsOpSpecial(NElement c, NTxResolutionContext context) {
        NBinaryOperatorElement bo = c.asBinaryOperator().get();
        NOptional<NTxNodeParser> ff = context.itemParser().nodeTypeParser(bo.operatorSymbol().lexeme());
        if (ff.isPresent()) {
            NScoredCallable<NTxItem> uu = ff.get().parseNode(context);
            if (NScorable.isValidScore(uu, NScorableContext.of())) {
                return uu;
            }
        }
        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
    }

    private NScoredCallable<NTxItem> parseNodeAsLiteral(NElement c, NTxResolutionContext context) {
        if (c.type() == NElementType.NAME) {
            NElement finalC = c;
            return NScoredCallable.ofValid(() -> new CtrlNTxNodeName(context.source(), finalC));
        }
        NTxNodeParser p = context.itemParser().nodeTypeParser(NTxNodeType.TEXT).orNull();
        if (p != null) {
            return p.parseNode(context);
        }
        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
    }

    private NScoredCallable<NTxItem> parseNodeAsPair(NElement c, NTxResolutionContext context) {
        if (c.isNamedPair()) {
            NPairElement p = c.asPair().get();
            String name = p.key().asStringValue().get();
            NOptional<NTxNodeParser> ff = context.itemParser().nodeTypeParser(name);
            if (ff.isPresent()) {
                NScoredCallable<NTxItem> uu = ff.get().parseNode(context);
                if (NScorable.isValidScore(uu, NScorableContext.of())) {
                    return uu;
                }
            }
        }
        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node from pair : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);
    }

    private NScoredCallable<NTxItem> parseNodeAsNamedListContainer(NElement c, NTxResolutionContext context) {
        String name = NTxUtils.uid(c.asNamed().get().name().get());
        switch (name){
            case NTxNodeType.PAGE_GROUP:
            case NTxNodeType.PAGE:
            case NTxNodeType.GROUP:
            case NTxNodeType.FRAGMENT:
            case NTxNodeType.BLOCK:
            case NTxNodeType.CTRL_FOR:
            case NTxNodeType.CTRL_IF:
            case NTxNodeType.CTRL_INCLUDE:
            case NTxNodeType.CTRL_IMPORT:
            case "styles":
            {
                NTxNodeParser p = context.itemParser().nodeTypeParser(name).orNull();
                return p.parseNode(context);
            }
        }

        NTxValue ee = NTxValue.of(c);
        String uid = NTxUtils.uid(ee.name());
        if(context.inPage()){
            NTxNodeParser p = context.itemParser().nodeTypeParser(uid).orNull();
            if (p != null) {
                return p.parseNode(context);
            }
        }
        if (c.isNamedUplet() || c.isAnyObject()) {
            NElement finalC2 = c;
            return NScoredCallable.ofValid(() -> createCtrlNodeCall(finalC2, context));
        }
        return _invalidSupport(NMsg.ofC("[%s] unable to resolve node : %s", NTxUtils.shortName(context.source()), NTxUtils.snippet(c)), context);

//        //just do not compile if still not in page mode!!!
//        if(context.node() instanceof CtrNTxNodelUncompiled && context.node().getRaw()==c){
//            return NScoredCallable.ofValid(context.node());
//        }
//        return NScoredCallable.ofValid(new CtrNTxNodelUncompiled(c, context.source()));
    }

    private NScoredCallable<NTxItem> parseNodeAsUplet(NTxResolutionContext context) {
        NTxNodeParser p = context.itemParser().nodeTypeParser(NTxNodeType.TEXT).orNull();
        return p.parseNode(context);
    }

    private NScoredCallable<NTxItem> parseNodeAsDefine(NElement c, NTxResolutionContext context) {
        NTxEngine engine = context.engine();
        //this is a node definition
        if (c.isAnyObject() || c.isNamed()) {
            NElement finalC1 = c;
            return NScoredCallable.ofValid(() -> {
                NObjectElement object = finalC1.asObject().get();
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
                                    context.log().log(NMsg.ofC("invalid definition param, expected var name %s in %s", x, object).asError());
                                    return null;
                                }
                            }).filter(x -> x != null).collect(Collectors.toList());
                } else {
                    params = new ArrayList<>();
                }
                NTxSource source = context.source();
                List<NTxNode> defBody = new ArrayList<>();
                for (NElement child : object.children()) {
                    NTxItem item = new CtrNTxNodelUncompiled(child,source);
                    NTxItemBag b = new NTxItemBag(Arrays.asList(item));
                    if (b.isNodes()) {
                        defBody.addAll(b.nodes());
                    } else {
                        context.log().log(NMsg.ofC("expected nodes, but got other items when creating node from %s", NTxUtils.snippet(child)).asError());
                    }
                }
                DefaultNTxNode bodyContainer = new DefaultNTxNode(NTxNodeType.GROUP);
                bodyContainer.addAll(defBody.toArray(new NTxItem[0]));
                NTxNodeDefImpl d = new NTxNodeDefImpl(
                        context.parent(),
                        templateName,
                        params.toArray(new NTxNodeDefParam[0]),
                        bodyContainer,
                        source
                );
                d.setRaw(c);
                return d;
            });
        } else {
            return _invalidSupport(NMsg.ofC("invalid defineNode syntax, expected @define <NAME>(...){....}"), context);
        }
    }

    private NScoredCallable<NTxItem> parseNodeAsOpColonEq(NElement c, NTxResolutionContext context) {
        NBinaryOperatorElement p = c.asBinaryOperator().get();
        NElement k = p.firstOperand();
        NElement v = p.secondOperand();
        NTxValue kh = NTxValue.of(k);
        if (k.isName()) {
            NOptional<String> nn = kh.asStringOrName();
            if (nn.isPresent()) {
                String nnn = NStringUtils.trim(nn.get());
                return NScoredCallable.ofValid(() -> DefaultNTxNode.ofAssignDefault(nnn, v, context.source()));
            } else {
                return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
            }
        } else {
            return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
        }
    }

    private NScoredCallable<NTxItem> parseNodeAsOpEq(NElement c, NTxResolutionContext context) {
        NBinaryOperatorElement p = c.asBinaryOperator().get();
        NElement k = p.firstOperand();
        NElement v = p.secondOperand();
        NTxValue kh = NTxValue.of(k);
        if (k.isName()) {
            NOptional<String> nn = kh.asStringOrName();
            if (nn.isPresent()) {
                String nnn = NStringUtils.trim(nn.get());
                return NScoredCallable.ofValid(() -> DefaultNTxNode.ofAssign(nnn, v, context.source()));
            } else {
                return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
            }
        } else {
            return _invalidSupport(NMsg.ofC("unable to interpret left hand of assignment as a valid var : %s", k), context);
        }
    }

    private NScoredCallable<NTxItem> parseNodeAsListCallable(NElement c, NTxResolutionContext context) {
        return NScoredCallable.ofValid(parseNodeAsList(c,context));
    }

    private NTxNode parseNodeAsList(NElement c, NTxResolutionContext context) {
        NListElement list = c.asList().get();
        DefaultNTxNode n = new DefaultNTxNode(
                list.type() == NElementType.ORDERED_LIST ? NTxNodeType.ORDERED_LIST : NTxNodeType.UNORDERED_LIST
                , context.source());
        for (NListItemElement item : list.items()) {
            n.addAll(Arrays.asList(parseListItem(item, context)).toArray(new NTxItem[0]));
        }
        return n;
    }

    private NTxItem parseListItem(NListItemElement c, NTxResolutionContext context) {
        List<NTxNode> nodes = new ArrayList<>();
        NElement v = c.value().orNull();
        if (v != null) {
            nodes.add(new CtrNTxNodelUncompiled(v,context.source()));
        }
        NListElement li = c.subList().orNull();
        if (li != null) {
            nodes.add(parseNodeAsList(li,context));
        }
        return NTxNodeUtils.ofNTxItem(nodes);
    }

    private NScoredCallable<NTxItem> _invalidSupport(NMsg msg, NTxResolutionContext context) {
        msg = msg.asError();
        context.log().log(msg.asError());
        return NScoredCallable.ofInvalid(msg);
    }


    private CtrlNTxNodeCall createCtrlNodeCall(NElement c, NTxResolutionContext context) {
        CtrlNTxNodeCall cc = new CtrlNTxNodeCall(context.source());
        NTxSource source = context.source();
        String __name = c.asNamed().get().name().get();
        cc.setProperty(NTxPropName.NAME, NTxUtils.addCompilerDeclarationPath(NElement.ofString(NTxUtils.uid(__name)), context.source()));
        List<NElement> __callBody = new ArrayList<>();
        List<NElement> __args = new ArrayList<>();
//        Map<String, NElement> __bodyVars = new HashMap<>();

        //inline current file path in the NElements
        if (source != null && source.path().orNull() != null) {
            NPath sourcePath = source.path().orNull();
            c = NTxUtils.addCompilerDeclarationPath(c, sourcePath.toString());
            if (c.isNamedUplet()) {
                NUpletElementBuilder fb = (NUpletElementBuilder) c.builder();
                for (int i = 0; i < fb.params().size(); i++) {
                    NElement u = NTxUtils.addCompilerDeclarationPath(fb.get(i).orNull(), sourcePath.toString());
                    fb.setAt(i, u);
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
                __callBody.addAll(fb.children());
                c = fb.build();
            } else {
                context.log().log(NMsg.ofC("unexpected call : %s (ignored)", c).asError(), context.source());
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
                __callBody.addAll(fb.children());
            } else {
                context.log().log(NMsg.ofC("unexpected call : %s (ignored)", c).asError(), context.source());
            }
        }
        cc.setCallName(__name);
        cc.setCallBody(__callBody);
        cc.setArgs(__args);
        cc.setRaw(c);
        cc.setProperty(NTxPropName.VALUE, c);
        cc.setSource(context.source());
        cc.setParent(context.parent());
        return cc;
    }

    private boolean isRootBloc(NTxResolutionContext context) {
        NTxNode[] nodes = context.path();
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
            List<NElement> params = a.params().orNull();
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

    private NScoredCallable<NTxItem> parseNodeAsNoNameBloc(NTxResolutionContext context) {
        NElement c = context.element();
        NTxEngine engine = context.engine();
        NTxDocumentFactory f = engine.documentFactory();
        HashSet<String> allAncestors = null;
        HashSet<String> allStyles = null;
        NTxValue ee = NTxValue.of(c);
        for (NElementAnnotation a : c.annotations()) {
            String nn = a.name();
            if (!NBlankable.isBlank(nn)) {
                if (allAncestors == null) {
                    allAncestors = new HashSet<>();
                }
                allAncestors.add(NTxUtils.uid(nn));
            }
            // add classes as well
            List<NElement> params = a.params().orNull();
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
//        NTxNode node = context.node();
        if ((allStyles != null || allAncestors != null) && !isRootBloc(context)) {
            NTxNode pg = f.ofGroup().setSource(context.source());
            pg.setStyleClasses(allStyles == null ? null : allStyles.toArray(new String[0]));
            for (NElement child : ee.body()) {
                pg.append(new CtrNTxNodelUncompiled(child, context.source()));
            }
            return NScoredCallable.ofValid(pg);
        } else {
            NTxItemList pg = new NTxItemList();
            for (NElement child : ee.body()) {
                pg.add(new CtrNTxNodelUncompiled(child, context.source()));
            }
            return NScoredCallable.ofValid(pg);
        }
    }

}
