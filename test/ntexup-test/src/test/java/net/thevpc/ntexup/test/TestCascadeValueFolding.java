package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.eval.DefaultNTxDependencyGraph;
import net.thevpc.ntexup.engine.document.DefaultNTxDocument;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.eval.NTxResolutionContextImpl;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.renderer.special.NTxAssignDefaultRenderer;
import net.thevpc.ntexup.engine.renderer.special.NTxAssignRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCascadeValueFolding {

    @BeforeAll
    static void initializeNuts() {
        Nuts.require();
    }

    @Test
    void reusedEvaluatorReadsCurrentVariableBinding() {
        NTxResolutionContextImpl context = context();
        context.setVar("name", NTxVar.ofEvaluatedExpression(NElement.ofString("first")));
        NElement expression = NElement.ofString("hello $name");

        assertEquals("hello first", context.evalExpression(expression).get().asStringValue().get());

        context.setVar("name", NTxVar.ofEvaluatedExpression(NElement.ofString("second")));
        assertEquals("hello second", context.evalExpression(expression).get().asStringValue().get());
    }

    @Test
    void copiedContextsShareVariablesUntilOneScopeMutates() {
        NTxResolutionContextImpl parent = context();
        parent.setVar("name", NTxVar.ofEvaluatedExpression(NElement.ofString("parent")));
        NTxResolutionContextImpl child = (NTxResolutionContextImpl) parent.pushContext();
        parent.setVar("name", NTxVar.ofEvaluatedExpression(NElement.ofString("parent-updated")));

        assertEquals("parent", child.getVar("name").get().get().asStringValue().get());
        child.setVar("name", NTxVar.ofEvaluatedExpression(NElement.ofString("child")));

        assertEquals("child", child.getVar("name").get().get().asStringValue().get());
        assertEquals("parent-updated", parent.getVar("name").get().get().asStringValue().get());
    }

    @Test
    void primitiveValuesAreReturnedUnchanged() {
        NTxResolutionContextImpl context = context();
        NElement[] literals = {
                NElement.ofNumber(42),
                NElement.ofBoolean(true),
                NElement.ofNull()
        };
        for (NElement literal : literals) {
            assertSame(literal, context.evalExpression(literal).get());
        }
    }

    @Test
    void bindingUpdatesInvalidateOnlyNodesThatReadTheBinding() {
        DefaultNTxDependencyGraph graph = new DefaultNTxDependencyGraph();
        NTxCompiledDocument document = (NTxCompiledDocument) Proxy.newProxyInstance(
                NTxCompiledDocument.class.getClassLoader(),
                new Class<?>[]{NTxCompiledDocument.class},
                (proxy, method, args) -> {
                    if ("dependencyGraph".equals(method.getName())) {
                        return graph;
                    }
                    return null;
                });
        DefaultNTxNode dependent = new DefaultNTxNode(NTxNodeType.TEXT);
        DefaultNTxNode unrelated = new DefaultNTxNode(NTxNodeType.TEXT);
        NTxResolutionContextImpl context = new NTxResolutionContextImpl(
                new NTxNode[]{dependent}, null, null, false,
                new DefaultNTxEngine(), new DefaultNTxDocument(null),
                null, null, null, document, null, null, null
        );
        context.setVar("answer", NTxVar.ofEvaluatedExpression(NElement.ofNumber(42)));
        assertEquals(42, context.evalExpression(NElement.ofName("answer")).get()
                .asIntValue().get());

        Object dependentCache = new Object();
        Object unrelatedCache = new Object();
        dependent.setRenderCache("test", dependentCache);
        unrelated.setRenderCache("test", unrelatedCache);
        graph.notifyBindingUpdated("answer");

        assertFalse(dependent.getRenderCache("test").isPresent());
        assertTrue(unrelated.getRenderCache("test").isPresent());
        assertSame(unrelatedCache, unrelated.getRenderCache("test").get());
    }

    @Test
    void assignmentRendererNotifiesDependentNodes() {
        DefaultNTxDependencyGraph graph = new DefaultNTxDependencyGraph();
        NTxCompiledDocument document = (NTxCompiledDocument) Proxy.newProxyInstance(
                NTxCompiledDocument.class.getClassLoader(),
                new Class<?>[]{NTxCompiledDocument.class},
                (proxy, method, args) -> {
                    if ("dependencyGraph".equals(method.getName())) {
                        return graph;
                    }
                    if ("getGlobalObject".equals(method.getName())) {
                        return NOptional.ofEmpty();
                    }
                    return null;
                });
        DefaultNTxNode dependent = new DefaultNTxNode(NTxNodeType.TEXT);
        DefaultNTxNode unrelated = new DefaultNTxNode(NTxNodeType.TEXT);
        NTxResolutionContextImpl parentContext = new NTxResolutionContextImpl(
                new NTxNode[]{dependent}, null, null, false,
                new DefaultNTxEngine(), new DefaultNTxDocument(null),
                null, null, null, document, null, null, null
        );
        parentContext.setVar("answer", NTxVar.ofEvaluatedExpression(NElement.ofNumber(1)));
        parentContext.getVar("answer");
        Object previous = new Object();
        Object unrelatedCache = new Object();
        dependent.setRenderCache("test", previous);
        unrelated.setRenderCache("test", unrelatedCache);

        NTxNode assignment = new DefaultNTxNode(NTxNodeType.CTRL_ASSIGN)
                .setProperty(NTxProp.ofString("name", "answer"))
                .setProperty(NTxProp.ofString("value", "42"));
        new NTxAssignRenderer().renderMain(rendererContext(assignment, parentContext, document));

        assertFalse(dependent.getRenderCache("test").isPresent());
        assertSame(unrelatedCache, unrelated.getRenderCache("test").get());

        parentContext.getVar("defaultAnswer");
        dependent.setRenderCache("test", previous);
        NTxNode defaultAssignment = new DefaultNTxNode(NTxNodeType.CTRL_ASSIGN_DEFAULT)
                .setProperty(NTxProp.ofString("name", "defaultAnswer"))
                .setProperty(NTxProp.ofString("value", "42"));

        new NTxAssignDefaultRenderer().renderMain(
                rendererContext(defaultAssignment, parentContext, document));

        assertFalse(dependent.getRenderCache("test").isPresent());
        assertSame(unrelatedCache, unrelated.getRenderCache("test").get());
    }

    private static NTxResolutionContextImpl context() {
        NTxNode root = new DefaultNTxNode(NTxNodeType.PAGE_GROUP);
        return new NTxResolutionContextImpl(
                new NTxNode[]{root}, null, null, false,
                new DefaultNTxEngine(), new DefaultNTxDocument(null),
                null, null, null, null, null, null, null
        );
    }

    private static NTxRendererContext rendererContext(
            NTxNode node, NTxResolutionContextImpl parentContext, NTxCompiledDocument document) {
        return (NTxRendererContext) Proxy.newProxyInstance(
                NTxRendererContext.class.getClassLoader(),
                new Class<?>[]{NTxRendererContext.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "node":
                            return node;
                        case "evalExpression":
                            return NOptional.of(args[0]);
                        case "setVar":
                            parentContext.setVar((String) args[0], (NTxVar) args[1]);
                            return parentContext;
                        case "parentContext":
                            return parentContext;
                        case "compiledDocument":
                            return document;
                        default:
                            return null;
                    }
                });
    }
}
