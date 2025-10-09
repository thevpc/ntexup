package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.parser.NTxAllArgumentReader;
import net.thevpc.ntexup.api.parser.NTxArgumentReader;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavorParseContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextToken;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.elem.NElement;

import java.util.List;
import java.util.function.Predicate;

public interface NTxNodeBuilderContext {
    String id();

    NTxNodeBuilderContext id(String id);

    NTxNodeBuilderContext alias(String... aliases);

    public NTxNodeBuilderContext parseAny(NTxItemSpecialParser a);

    public NTxNodeBuilderContext parseAny(Predicate<NElement> a);

    public NTxNodeBuilderContext parseAny(Predicate<NElement> a, int support);

    NTxNodeBuilderContext sizeRequirements(SizeRequirementsAction e);

    NTxNodeBuilderContext selfBounds(SelfBoundsAction e);

    NTxNodeBuilderContext withToElem(ToElemAction e);

    NTxNodeBuilderContext withToElem(String... props);

    NTxNodeBuilderContext parseDefaultParams();

    NTxNodeBuilderContext renderComponent(RenderAction e);

    RenderTextAction renderText();

    NTxNodeBuilderContext renderConvert(RenderConvertAction renderConvertAction);

    NamedParamAction parseParam();

    NTxNodeBuilderContext parseParam(ProcessParamAction e);

    NTxNodeBuilderContext afterParsingAllParams(ProcessNodeAction e);

    NTxNodeBuilderContext processChildren(ProcessNodeAction e);

    NTxEngine engine();

    boolean isAncestorScene3D(NTxNode p0);

    NTxNodeBuilderContext addParamName(String points);

    String[] aliases();

    String[] idAndAliases();


    interface NTxItemSpecialParser {
        NScoredCallable<NTxItem> parseElement(String id, NElement element, NTxNodeFactoryParseContext context);
    }

    interface RenderTextAction {
        RenderTextAction buildText(RenderTextBuildAction a);

        RenderTextAction parseTokens(RenderTextParseTokensAction embeddedTextAction);

        RenderTextAction startSeparators(String... startSeparators);

        NTxNodeBuilderContext then();

        NTxNodeBuilderContext end();
    }

    interface NamedParamAction {
        NTxNodeBuilderContext.NamedParamAction ignoreDuplicates();

        NamedParamAction ignoreDuplicates(boolean ignoreDuplicates);

        NamedParamAction matchesLeading(boolean ignoreDuplicates);

        NamedParamAction matchesLeading();

        NamedParamAction matchesNamedPair(String... names);

        NamedParamAction matches(Predicate<NTxArgumentReader> predicate);

        NamedParamAction matchesMissingProperties(String... names);

        NamedParamAction matchesString();

        NamedParamAction matchesName();

        NamedParamAction matchesStringOrName();

        NamedParamAction matchesAny();

        NamedParamAction matchesAnyNonPair();

        NamedParamAction storeName(String newName);

        NamedParamAction storeName(ToNameResolver newName);

        NamedParamAction storeFirstMissingName(String... names);

        NamedParamAction store(StoreAction newName);

        NamedParamAction resolvedAs(PropResolver a);

        NTxNodeBuilderContext then();

        NTxNodeBuilderContext end();

        NamedParamAction asFlags();

        NamedParamAction matchesNonCommonFlags();
    }

    interface PropResolver {
        NTxProp resolve(String uid, NElement value, NTxArgumentReader info, NTxNodeBuilderContext buildContext);
    }

    interface ToElemAction {
        NElement toElem(NTxNode item, NTxNodeBuilderContext buildContext);
    }

    interface RenderAction {
        void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext);
    }

    interface RenderTextBuildAction {
        void buildText(String text, NTxTextOptions options, NTxNode p, NTxNodeRendererContext rendererContext, NTxTextRendererBuilder builder, NTxNodeBuilderContext buildContext);
    }

    interface RenderConvertAction {
        NTxNode convert(NTxNode p, NTxNodeRendererContext ctx, NTxNodeBuilderContext buildContext);
    }

    interface RenderTextParseTokensAction {
        List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext parseContext, NTxNodeBuilderContext buildContext);
    }

    interface SizeRequirementsAction {
        NTxSizeRequirements sizeRequirements(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext);
    }

    interface SelfBoundsAction {
        NTxBounds2 selfBounds(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext);
    }

    interface ProcessParamAction {
        boolean processParam(NTxArgumentReader info, NTxNodeBuilderContext buildContext);
    }

    interface ToNameResolver {
        String resolveName(String visitedName, NTxArgumentReader info, NTxNodeBuilderContext buildContext);
    }

    interface StoreAction {
        void store(NTxArgumentReader info, NTxNodeBuilderContext buildContext);
    }

    interface ProcessNodeAction {
        void processNode(NTxAllArgumentReader info, NTxNodeBuilderContext buildContext);
    }
}
