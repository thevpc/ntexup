package net.thevpc.ntexup.engine.ext;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.parser.*;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.NTxArgumentReaderImpl;
import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.engine.util.ToElementHelper;
import net.thevpc.ntexup.engine.parser.NTxParseHelper;
import net.thevpc.nuts.concurrent.NScorableCallable;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

class CustomNTxNodeParserFromBuilder extends NTxNodeParserBase {
    private final NTxNodeBuilderContextImpl ctx;

    public CustomNTxNodeParserFromBuilder(NTxNodeBuilderContextImpl ctx) {
        super(true, ctx.id, ctx.aliases == null ? new String[0] : ctx.aliases);
        this.ctx = ctx;
    }

    @Override
    public NElement toElem(NTxNode item) {
        if (ctx.toElem != null) {
            NElement u = ctx.toElem.toElem(item, ctx);
            if (u != null) {
                return u;
            }
        }
        if (ctx.knownArgNames != null && !ctx.knownArgNames.isEmpty()) {
            return ToElementHelper.of(
                            item,
                            engine()
                    )
                    .addChildrenByName(ctx.knownArgNames.toArray(new String[0]))
                    .build();
        }
        return super.toElem(item);
    }

    @Override
    protected String acceptTypeName(NElement e) {
        String s = super.acceptTypeName(e);
        if (s != null) {
            return s;
        }
        return s;
    }

    protected void processChildren(NTxAllArgumentReader info) {
        if (ctx.processChildren != null) {
            ctx.processChildren.processNode(info, ctx);
            return;
        }
        super.processChildren(info);
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
        NTxNodeBuilderContext.NTxItemSpecialParser pp = ctx.extraElementSupport;
        if (pp == null && ctx.extraElementSupportByPredicate != null) {
            if (ctx.extraElementSupportByPredicate.test(e)) {
                pp = new NTxNodeBuilderContext.NTxItemSpecialParser() {
                    @Override
                    public NScorableCallable<NTxItem> parseElement(String id, NElement element, NTxNodeFactoryParseContext context) {
                        NTxNode p = context.documentFactory().of(resolveEffectiveId(id));
                        String compilerDeclarationPath = NTxUtils.getCompilerDeclarationPath(element);
                        NTxSource source = context.source();
                        if (compilerDeclarationPath != null) {
                            NTxSource s = context.document().sourceMonitor().find(compilerDeclarationPath).orNull();
                            if (s != null) {
                                source = s;
                            }
                        }
                        p.setSource(source);
                        NTxNodeFactoryParseContext context2 = context.push(p);
                        onStartParsingItem(id, p, element, context);
                        NTxParseHelper.fillAnnotations(element, p);
                        NTxArgumentReaderImpl info = new NTxArgumentReaderImpl();
                        info.setContext(context2);
                        info.setId(id);
                        info.setUid(NTxUtils.uid(id));
                        info.setElement(NTxUtils.addCompilerDeclarationPath(element, info.parseContext().source()));
                        info.setNode(p);
                        info.setArguments(new NElement[]{element});
                        info.setF(context.documentFactory());
                        processArguments(info);
                        processImplicitStyles(info);
                        onFinishParsingItem(info);
                        return NScorableCallable.ofValid(ctx.extraElementSupportByPredicateSupport, p);
                    }
                };
            }
        }
        if (pp != null) {
            NScorableCallable<NTxItem> y = pp.parseElement(id(), e, context);
            if (y != null) {
                return y;
            }
        }
        //context.messages().addError(NMsg.ofC("invalid %s : %s", id(), e), context.source());
        return NScorableCallable.ofInvalid(() -> NMsg.ofC("invalid %s : %s", id(), e));
    }

    @Override
    protected boolean processArgument(NTxArgumentReader info) {
        if (ctx.processSingleArgumentList != null) {
            for (NTxNodeBuilderContext.ProcessParamAction a : ctx.processSingleArgumentList) {
                if (a.processParam(info, ctx)) {
                    return true;
                }
            }
            if (ctx.createdParser.defaultProcessArgument(info)) {
                return true;
            }
            return false;
        }
        return super.processArgument(info);
    }

    @Override
    public void onFinishParsingItem(NTxAllArgumentReader info) {
        super.onFinishParsingItem(info);
        if (ctx.afterProcessAllArgumentsList != null) {
            for (NTxNodeBuilderContext.ProcessNodeAction a : ctx.afterProcessAllArgumentsList) {
                a.processNode(info, ctx);
            }
        }
    }
}
