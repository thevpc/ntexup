package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCascadeScopeSemantics {

    @BeforeAll
    static void initializeNuts() {
        Nuts.require();
    }

    @Test
    void loopExpandsNodesInItsChildScope() {
        NTxEngine engine = new DefaultNTxEngine();
        String source = "page{\n"
                + "  for(i:[1,2]){\n"
                + "    rectangle(name:\"loop-item\", size:(10,10))\n"
                + "    if(i>1){\n"
                + "      text(\"nested\")\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        NTxCompiledDocument document = engine.loadDocument(new ByteArrayInputStream(
                source.getBytes(StandardCharsets.UTF_8)));

        NTxNode root = document.page(0).get().compiledPage();
        assertEquals(2, nodesOfType(root, "rectangle").size(), root.toString());
        assertEquals(1, nodesOfType(root, "text").size(), root.toString());
    }

    private static List<NTxNode> nodesOfType(NTxNode root, String type) {
        List<NTxNode> result = new ArrayList<>();
        collect(root, type, result);
        return result;
    }

    private static void collect(NTxNode node, String type, List<NTxNode> result) {
        if (type.equals(node.type())) {
            result.add(node);
        }
        for (NTxNode child : node.children()) {
            collect(child, type, result);
        }
    }
}
