package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.engine.document.DefaultNTxDocument;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.eval.NTxResolutionContextImpl;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    private static NTxResolutionContextImpl context() {
        NTxNode root = new DefaultNTxNode(NTxNodeType.PAGE_GROUP);
        return new NTxResolutionContextImpl(
                new NTxNode[]{root}, null, null, false,
                new DefaultNTxEngine(), new DefaultNTxDocument(null),
                null, null, null, null, null, null, null
        );
    }
}
