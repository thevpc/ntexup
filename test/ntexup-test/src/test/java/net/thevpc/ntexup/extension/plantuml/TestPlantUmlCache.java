package net.thevpc.ntexup.extension.plantuml;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TestPlantUmlCache {

    @Test
    void cachesRenderedDiagramBySourceAndSize() {
        DefaultNTxNode node = new DefaultNTxNode(NTxNodeType.TEXT);
        NTxRendererContext context = (NTxRendererContext) Proxy.newProxyInstance(
                NTxRendererContext.class.getClassLoader(),
                new Class<?>[]{NTxRendererContext.class},
                (proxy, method, args) -> null);
        String source = "@startuml\nAlice -> Bob\n@enduml";

        BufferedImage first = PlantUmlBuilderBase.renderDiagramImage(source, node, 640, 480, context);
        BufferedImage second = PlantUmlBuilderBase.renderDiagramImage(source, node, 640, 480, context);

        assertNotNull(first);
        assertSame(first, second);
        assertNotSame(first,
                PlantUmlBuilderBase.renderDiagramImage(source, node, 800, 600, context));
        assertNotSame(first,
                PlantUmlBuilderBase.renderDiagramImage("@startuml\nAlice -> Carol\n@enduml",
                        node, 640, 480, context));
    }
}
