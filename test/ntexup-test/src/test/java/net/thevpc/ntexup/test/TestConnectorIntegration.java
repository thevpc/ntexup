package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxPlaced;
import net.thevpc.ntexup.api.renderer.NTxRenderSessionListener;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.renderer.dump.DumpDocumentRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration checks for connector resolution across the container/layout
 * constructs that are easy to get wrong: looped nodes, dissolved fragments and
 * the row footprint (a connector must not consume a layout cell).
 */
public class TestConnectorIntegration {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void loopedNodesCanBeReferencedByChainedConnectors() throws Exception {
        RenderResult r = render("page{\n"
                + "  @for (i in [1,2,3]){\n"
                + "    rectangle(name: \"r\" + i, at:(10,10), size:(10,8),\n"
                + "              background-color:white, draw-contour)\n"
                + "  }\n"
                + "  connector(\"r1\", \"r2\", end-arrow:simple())\n"
                + "  connector(\"r2\", \"r3\", end-arrow:simple())\n"
                + "}\n");

        assertNotNull(r.placed("r1"));
        assertNotNull(r.placed("r2"));
        assertNotNull(r.placed("r3"));
        String dump = r.dump();
        assertTrue(dump.contains("route"), "chained connectors should be dumped: " + dump);
    }

    @Test
    public void fragmentDissolvesButItsChildrenStayReferenceable() throws Exception {
        RenderResult r = render("page{\n"
                + "  fragment{\n"
                + "    rectangle(name:\"fa\", at:(10,10), size:(12,8))\n"
                + "    rectangle(name:\"fb\", at:(50,10), size:(12,8))\n"
                + "  }\n"
                + "  connector(\"fa\", \"fb\", end-arrow:simple())\n"
                + "}\n");

        assertNotNull(r.placed("fa"));
        assertNotNull(r.placed("fb"));
        assertTrue(r.placed.stream().noneMatch(p -> "fragment".equals(p.node.type())),
                "fragment must dissolve out of the render tree");
        assertTrue(r.dump().contains("route"), "connector across a fragment should resolve");
    }

    @Test
    public void connectorDoesNotConsumeARowCell() throws Exception {
        RenderResult r = render("page{\n"
                + "  row(at:(10,10), size:(80,20)){\n"
                + "    rectangle(name:\"ra\", background-color:white, draw-contour)\n"
                + "    connector(\"ra\", \"rb\", end-arrow:simple())\n"
                + "    rectangle(name:\"rb\", background-color:white, draw-contour)\n"
                + "  }\n"
                + "}\n");

        NTxPlaced ra = r.placed("ra");
        NTxPlaced rb = r.placed("rb");
        assertNotNull(ra);
        assertNotNull(rb);
        assertTrue(ra.bounds.widthX() > 30,
                "row cell collapsed to " + ra.bounds.widthX() + "; connector probably consumed a cell");
        assertTrue(rb.bounds.widthX() > 30,
                "row cell collapsed to " + rb.bounds.widthX() + "; connector probably consumed a cell");
        assertEquals(ra.bounds.widthX(), rb.bounds.widthX(), 1.5,
                "two row cells should share the row equally");
        assertTrue(rb.bounds.minX() >= ra.bounds.minX() + ra.bounds.widthX() - 1.0,
                "row cells should not overlap: " + ra.bounds + " vs " + rb.bounds);
    }

    private static RenderResult render(String deck) {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));
        List<NTxPlaced> placed = new ArrayList<>();
        NTxNodeRendererConfig config = new NTxNodeRendererConfig(400, 300)
                .withAnimate(false)
                .withPrint(true);
        config.setCapability(NTxRenderSessionListener.CAPABILITY,
                (NTxRenderSessionListener) session -> placed.addAll(session.geometry().placed()));
        BufferedImage image = engine.renderImage(doc.page(0).get(), config);
        assertNotNull(image);
        return new RenderResult(engine, doc, placed);
    }

    private static final class RenderResult {
        final NTxEngine engine;
        final NTxCompiledDocument doc;
        final List<NTxPlaced> placed;
        private String dumpText;

        RenderResult(NTxEngine engine, NTxCompiledDocument doc, List<NTxPlaced> placed) {
            this.engine = engine;
            this.doc = doc;
            this.placed = placed;
        }

        NTxPlaced placed(String name) {
            return placed.stream().filter(p -> name.equals(p.name)).findFirst().orElse(null);
        }

        String dump() {
            if (dumpText == null) {
                NTxDocumentStreamRendererConfig cfg = new NTxDocumentStreamRendererConfig()
                        .setPageWidth(400)
                        .setPageHeight(300)
                        .setDpi(72);
                NElement element = new DumpDocumentRenderer(engine, cfg).renderDump(doc);
                dumpText = element.toString();
            }
            return dumpText;
        }
    }
}
