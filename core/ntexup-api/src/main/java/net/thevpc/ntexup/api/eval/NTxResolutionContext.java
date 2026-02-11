package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NOptional;

public interface NTxResolutionContext {
    NTxDocument document();

    NTxLogger messages();

    NTxEngine engine();

    NTxResolutionContext copy();

    NTxResolutionContext pushNode(NTxNode parent);

    NTxResolutionContext popNode();

    NTxResolutionContext resolveNode(NTxNode parent);

    NTxResolutionContext setElement(NElement element);
    NTxResolutionContext setNode(NTxNode parent);

    NTxResolutionContext withNode(NTxNode parent);

    NTxResolutionContext withElement(NElement element);

    NTxResolutionContext setInPage(boolean isInPage);

    NTxResolutionContext withInPage(boolean isInPage);

    NElement element();

    NTxLogger log();

    NTxNode[] path();

    NTxNode node();

    NTxNode parent();

    NTxSource source();

    NTxNodeDef def();

    boolean inPage();

    NTxResolutionContext withParentOnly(NTxNode parent);

    NTxResolutionContext withParentOnly();

    NTxResolutionContext withDef(NTxNodeDef def);

    /**
     * update current
     *
     * @param name
     * @param value
     * @return
     */
    NTxResolutionContext setVar(String name, NTxVar value);

    /**
     * create new context
     *
     * @param name
     * @param value
     * @return
     */
    NTxResolutionContext withVar(String name, NTxVar value);

    NTxResolutionContext withRemoveNamedDef(String name);

    NTxResolutionContext removeNamedDef(String name);

    NTxResolutionContext setNamedDef(NTxNodeDef value);

    NTxResolutionContext withNamedDef(NTxNodeDef value);

    NOptional<NTxNodeDef> getNamedDef(String name);

    NOptional<NTxVar> getVar(String name);

    NOptional<NElement> getVarValue(String name);

    NTxResolutionContext withRemoveFunction(String name);

    NTxResolutionContext removeFunction(String name);

    NOptional<NTxFunction> getFunction(String name);

    NTxResolutionContext setFunction(NTxFunction value);

    NTxResolutionContext withFunction(NTxFunction value);

    boolean isDef();

    NOptional<NElement> evalExpression(NElement expression);

    NPath resolvePath(NElement path);

    NTxDocumentFactory documentFactory();
}
