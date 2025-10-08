package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.parser.*;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.util.ToElementHelper;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.nuts.concurrent.NScorableCallable;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;


import java.util.Collections;
import java.util.List;

public abstract class NTxNodeParserBase implements NTxNodeParser {

    private String id;
    private String[] aliases;
    private boolean container;
    private NTxEngine engine;
    private String nodeId;

    public NTxNodeParserBase(boolean container, String id, String... aliases) {
        this.id = id;
        this.aliases = aliases;
        this.container = container;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public NTxEngine engine() {
        return engine;
    }

    public void init(NTxEngine engine) {
        this.engine = engine;
    }

    public boolean isContainer() {
        return container;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String[] aliases() {
        return aliases;
    }

    protected void processImplicitStyles(NTxArgumentReader info) {

    }

    protected boolean isAcceptableArgKeyPair(String s) {
        return false;
    }

    protected boolean processArgument(NTxArgumentReader info) {
        return defaultProcessArgument(info);
    }

    public boolean defaultProcessArgument(NTxArgumentReader info) {
        NElement e = info.peek();
        if (e != null) {
            if (e.isNamedPair()) {
                NPairElement p = e.asPair().get();
                String sid = NTxUtils.uid(p.key().asStringValue().get());
                if (NTxParserUtils.isCommonStyleProperty(sid)) {
                    info.node().setProperty(sid, NTxUtils.addCompilerDeclarationPath(p.value(), info.source()));
                    info.read();
                    return true;
                }
            } else if (e.isName()) {
                String sid = NTxUtils.uid(e.asStringValue().get());
                if (NTxParserUtils.isCommonStyleProperty(sid)) {
                    info.node().setProperty(sid, NTxUtils.addCompilerDeclarationPath(NElement.ofTrue(), info.source()));
                    info.read();
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isAncestorScene3D(NTxNode p0) {
        NTxItem p = p0;
        while (p != null) {
            if (p instanceof NTxNode) {
                if (NTxNodeType.SCENE3D.equals(((NTxNode) p).type())) {
                    return true;
                }
            }
            p = p.parent();
        }
        return false;
    }

    protected String acceptTypeName(NElement e) {
        switch (e.type()) {
            case NAME: {
                if (acceptTypeName(e.asStringValue().get())) {
                    return e.asStringValue().get();
                }
                break;
            }
            case NAMED_UPLET: {
                NUpletElement uplet = e.asUplet().get();
                if (acceptTypeName(uplet.name().orNull())) {
                    return uplet.name().orNull();
                }
                break;
            }
            case NAMED_PARAMETRIZED_OBJECT:
            case NAMED_OBJECT: {
                NObjectElement h = e.toObject().get();
                if (acceptTypeName(h.name().orNull())) {
                    return h.name().orNull();
                }
                break;
            }
            case NAMED_PARAMETRIZED_ARRAY:
            case NAMED_ARRAY: {
                NArrayElement h = e.toArray().get();
                if (acceptTypeName(h.name().orNull())) {
                    return h.name().orNull();
                }
                break;
            }
        }
        return null;
    }

    private boolean acceptTypeName(String id) {
        String oid = NTxUtils.uid(id);
        if (id.equals(oid)) {
            return true;
        }
        for (String s : aliases()) {
            if (s.equals(oid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NScorableCallable<NTxItem> parseNode(NTxNodeFactoryParseContext context) {
        NElement e = context.element();

        String s = acceptTypeName(e);
        if (s != null) {
            return NScorableCallable.ofValid(
                    () -> {
                        NOptional<NTxItem> o = parseItem(s, e, context);
                        if (!o.isPresent()) {
                            return parseItem(s, e, context).get();
                        }
                        return o.get();
                    }
            );
        }
        //context.messages().addError(NMsg.ofC("invalid %s : %s", id(), e), context.source());
        return NScorableCallable.ofInvalid(() -> NMsg.ofC("invalid %s : %s", id(), e));
    }

    public String resolveEffectiveId(String id) {
        String nId = getNodeId();
        if (!NBlankable.isBlank(nId)) {
            return NTxUtils.uid(nId);
        }
        return NTxUtils.uid(id);
    }

    public void onStartParsingItem(String id, NTxNode p, NElement element, NTxNodeFactoryParseContext context) {

    }

    public void onFinishParsingItem(NTxAllArgumentReader info) {

    }

    protected boolean processArgumentAsCommonStyleProperty(NTxArgumentReader info) {
        NElement currentArg = info.peek();
        if (NTxParserUtils.isCommonStyleProperty(info.getId())) {
            NTxValue es = NTxValue.of(currentArg);
            if (es.isFunction()) {
                info.parseContext().messages().log(NMsg.ofC("[%s] invalid argument %s. did you mean %s:%s ?",
                        info.parseContext().source(),
                        currentArg,
                        es.name(), NElement.ofUplet(es.args().toArray(new NElement[0]))
                ).asSevere(), info.parseContext().source());
                // empty result
                return false;
            }
            info.parseContext().messages().log(NMsg.ofC("[%s] invalid argument %s in : %s", info.parseContext().source(), currentArg, info.element()).asSevere(), info.parseContext().source());
            return false;
        } else {
            info.parseContext().messages().log(NMsg.ofC("[%s] invalid argument %s in : %s", NTxUtils.shortName(info.parseContext().source()), NTxUtils.snippet(currentArg), NTxUtils.snippet(info.element())).asSevere(), info.parseContext().source());
            return false;
        }
    }

    protected void processArguments(NTxArgumentReader info) {
        while (!info.isEmpty()) {
            int before = info.availableCount();
            boolean b = processArgument(info);
            int after = info.availableCount();
            if (b) {
                if (after == before) {
                    processArgument(info);
                    NElement skipped = info.peek();
                    engine().log().log(NMsg.ofC("process arg is buggy, skipped %s...", NTxUtils.snippet(skipped)));
                    info.read();
                }
            } else {
                if (!processArgumentAsCommonStyleProperty(info)) {
                    NElement skipped = info.peek();
                    engine().log().log(NMsg.ofC("[%s] invalid argument %s", id,
                                    NTxUtils.snippet(skipped)
                            ).asSevere(), info.source()
                    );
                    info.read();
                }
            }
        }
    }

    public NOptional<NTxItem> parseItem(String id, NElement element, NTxNodeFactoryParseContext context) {
        NTxEngine engine = context.engine();
        NTxDocumentFactory f = context.documentFactory();
        switch (element.type()) {
            case NAMED_UPLET:

            case OBJECT:
            case NAMED_PARAMETRIZED_OBJECT:
            case PARAMETRIZED_OBJECT:
            case NAMED_OBJECT:

            case ARRAY:
            case NAMED_PARAMETRIZED_ARRAY:
            case PARAMETRIZED_ARRAY:
            case NAMED_ARRAY: {
                NTxNode p = context.documentFactory().of(resolveEffectiveId(id));
                p.setSource(context.source());
                NTxNodeFactoryParseContext context2 = context.push(p);
                onStartParsingItem(id, p, element, context);
                NTxParseHelper.fillAnnotations(element, p);
                NTxArgumentReaderImpl info = new NTxArgumentReaderImpl();
                info.setContext(context2);
                info.setId(id);
                info.setUid(NTxUtils.uid(id));
                info.setElement(NTxUtils.addCompilerDeclarationPath(element, info.parseContext().source()));
                info.setNode(p);
                switch (element.type()) {
                    case UPLET:
                    case NAMED_UPLET: {
                        info.setArguments(element.asUplet().get().params().toArray(new NElement[0]));
                        break;
                    }
                    default: {
                        info.setArguments(element.asParametrizedContainer().flatMap(x -> x.params()).orElse(Collections.emptyList()).toArray(new NElement[0]));
                    }
                }
                if (info.allArguments() != null) {
                    NElement[] arguments = info.allArguments();
                    for (int i = 0; i < arguments.length; i++) {
                        info.allArguments()[i] = NTxUtils.addCompilerDeclarationPath(arguments[i], info.parseContext().source());
                    }
                }
                info.setF(f);
                processArguments(info);
                processImplicitStyles(info);
                processChildren(info);
                onFinishParsingItem(info);
                return NOptional.of(p);
            }
        }
        return NOptional.ofNamedEmpty(NMsg.ofC("%s item %s", id(), element));
    }

    protected void processChildren(NTxAllArgumentReader info) {
        NElement element = info.element();
        List<NElement> body = null;
        switch (element.type()) {
            case UPLET:
            case NAMED_UPLET: {
                return;
            }
            default: {
                body = element.asListContainer().map(x -> x.children()).orElse(Collections.emptyList());
            }
        }
        for (NElement e : body) {
            NOptional<NTxItem> u = engine.newNode(e, info.parseContext());
            if (u.isPresent()) {
                info.node().append(u.get());
            } else {
                info.parseContext().messages().log(NMsg.ofC("[%s] error parsing child : %s : %s", info.parseContext().source(), e, u.getMessage().get()).asSevere(), info.parseContext().source());
            }
        }
    }

    @Override
    public NElement toElem(NTxNode item) {
        return ToElementHelper.of((NTxNode) item, engine)
                .build();
    }

    @Override
    public NTxNode newNode() {
        return engine().newDefaultNode(id());
    }

    @Override
    public boolean validateNode(NTxNode node) {
        return false;
    }

    protected NScorableCallable<NTxItem> _invalidSupport(NMsg msg, NTxNodeFactoryParseContext context) {
        msg = msg.asError();
        context.messages().log(msg.asError());
        return NScorableCallable.ofInvalid(msg);
    }

    protected void _logError(NMsg nMsg, NTxNodeFactoryParseContext context) {
        context.messages().log(nMsg.asError(), context.source());
    }
}
