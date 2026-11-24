package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEngine {
    @BeforeAll
    static void init() {
        Nuts.require();
    }

    public static void main(String[] args) {
        Nuts.require();
        NTxEngine e=new DefaultNTxEngine();
        String document="\n" +
                "$a=1\n" +
                "if($a==1){\n" +
                " rectangle()\n" +
//                " } elseif($a==2) {\n" +
//                "  triangle()\n" +
                " } else {\n" +
                "  circle()\n" +
                "}\n"
                ;
        System.out.println(document);
        NTxCompiledDocument d = e.loadDocument(
                new ByteArrayInputStream(document.getBytes())
        );
    }

    @Test
    public void defaultBodyWrapsDirectContentAndPreservesExplicitLayouts() {
        String document = "" +
                "@default-body(body, body2a)\n" +
                "@define demo { group { slot } }\n" +
                "@define body { text(\"BODY\") slot }\n" +
                "@define body2a { text(\"COLUMN\") slot }\n" +
                "page {\n" +
                "    demo { text(\"direct\") }\n" +
                "    demo { body { text(\"explicit\") } }\n" +
                "    demo { body2a { text(\"two-columns\") } }\n" +
                "    demo { text(\"loose\") body2a { text(\"mixed\") } }\n" +
                "    demo()\n" +
                "}\n";
        assertEquals(List.of(
                "BODY", "direct",
                "BODY", "explicit",
                "COLUMN", "two-columns",
                "loose", "COLUMN", "mixed"
        ), textValues(document));
    }

    @Test
    public void defaultBodyIsOptInAndDoesNotWrapForwardedSlots() {
        String unannotated = "" +
                "@define body { text(\"BODY\") slot }\n" +
                "@define demo { slot }\n" +
                "page { demo { text(\"plain\") } }\n";
        assertEquals(List.of("plain"), textValues(unannotated));

        String forwarded = "" +
                "@define body { text(\"BODY\") slot }\n" +
                "@default-body(body)\n" +
                "@define target { text(\"TARGET\") slot }\n" +
                "@define forward { target { slot } }\n" +
                "page { forward { text(\"payload\") } }\n";
        assertEquals(List.of("TARGET", "payload"), textValues(forwarded));

        String mixed = "" +
                "@define body { text(\"BODY\") slot }\n" +
                "@define target { text(\"TARGET\") slot }\n" +
                "@default-body(body)\n" +
                "@define forward { target { slot } }\n" +
                "@define outer { forward { text(\"direct\") slot } }\n" +
                "page { outer { text(\"payload\") } }\n";
        assertEquals(List.of("TARGET", "BODY", "direct", "payload"), textValues(mixed));
    }

    private static List<String> textValues(String document) {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument compiled = engine.loadDocument(new ByteArrayInputStream(document.getBytes()));
        assertEquals(1, compiled.pages().size());
        List<String> values = new ArrayList<>();
        collectTextValues(compiled.pages().get(0).compiledPage(), values);
        return values;
    }

    private static void collectTextValues(NTxNode node, List<String> values) {
        if (NTxNodeType.TEXT.equals(node.type())) {
            NElement value = node.getPropertyValue("value").orNull();
            if (value != null) {
                values.add(value.asStringValue().orElse(value.toString()));
            }
        }
        for (NTxNode child : node.children()) {
            collectTextValues(child, values);
        }
    }
}
