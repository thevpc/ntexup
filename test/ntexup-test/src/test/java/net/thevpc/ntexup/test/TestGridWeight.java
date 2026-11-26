package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxPlaced;
import net.thevpc.ntexup.api.renderer.NTxRenderSessionListener;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestGridWeight {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void gridWeightSizesBothGridAxes() {
        RenderResult r = render("page{"
                + " grid(2,2,size:(200px,200px)){"
                + "  rectangle(name:\"a\",grid-weight:3)"
                + "  rectangle(name:\"b\",grid-weight:1)"
                + "  rectangle(name:\"c\",grid-weight:2)"
                + "  rectangle(name:\"d\")"
                + " }"
                + "}");

        NTxPlaced a = r.placed("a");
        NTxPlaced b = r.placed("b");
        NTxPlaced c = r.placed("c");
        NTxPlaced d = r.placed("d");
        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);
        assertNotNull(d);
        assertEquals(3 * b.bounds.widthX(), a.bounds.widthX(), 0.002);
        assertEquals(c.bounds.widthX(), a.bounds.widthX(), 0.002);
        assertEquals(d.bounds.widthX(), b.bounds.widthX(), 0.002);
        assertEquals(c.bounds.widthY(), d.bounds.widthY(), 0.002);
        assertEquals(3 * c.bounds.widthY(), 2 * a.bounds.widthY(), 0.002);
    }

    @Test
    public void axisWeightsOverrideGridWeightIndependently() {
        RenderResult r = render("page{"
                + " grid(2,2,size:(200px,200px)){"
                + "  rectangle(name:\"a\",grid-weight:3)"
                + "  rectangle(name:\"b\",grid-weight:1,col-weight:1,row-weight:4)"
                + "  rectangle(name:\"c\",row-weight:2)"
                + "  rectangle(name:\"d\")"
                + " }"
                + "}");

        NTxPlaced a = r.placed("a");
        NTxPlaced b = r.placed("b");
        NTxPlaced c = r.placed("c");
        NTxPlaced d = r.placed("d");
        assertEquals(3 * b.bounds.widthX(), a.bounds.widthX(), 0.002);
        assertEquals(2 * d.bounds.widthY(), b.bounds.widthY(), 0.002);
        assertEquals(c.bounds.widthY(), d.bounds.widthY(), 0.002);
        assertEquals(a.bounds.widthY(), b.bounds.widthY(), 0.002);
    }

    @Test
    public void containerArraysOverrideChildGridWeights() {
        RenderResult r = render("page{"
                + " grid(2,2,size:(200px,200px),columns-weight:[2,1],rows-weight:[1,3]){"
                + "  rectangle(name:\"a\",grid-weight:5)"
                + "  rectangle(name:\"b\",grid-weight:5)"
                + "  rectangle(name:\"c\",grid-weight:5)"
                + "  rectangle(name:\"d\",grid-weight:5)"
                + " }"
                + "}");

        NTxPlaced a = r.placed("a");
        NTxPlaced b = r.placed("b");
        NTxPlaced c = r.placed("c");
        NTxPlaced d = r.placed("d");
        assertEquals(2 * b.bounds.widthX(), a.bounds.widthX(), 0.002);
        assertEquals(3 * a.bounds.widthY(), c.bounds.widthY(), 0.002);
        assertEquals(c.bounds.widthY(), d.bounds.widthY(), 0.002);
    }

    @Test
    public void rowAndColReadChildGridWeights() {
        RenderResult row = render("page{"
                + " row(size:(200px,100px)){"
                + "  rectangle(name:\"a\",grid-weight:1)"
                + "  rectangle(name:\"b\",grid-weight:3)"
                + " }"
                + "}");
        assertEquals(3 * row.placed("a").bounds.widthX(), row.placed("b").bounds.widthX(), 0.002);

        RenderResult col = render("page{"
                + " col(size:(100px,200px)){"
                + "  rectangle(name:\"a\",grid-weight:1)"
                + "  rectangle(name:\"b\",grid-weight:3)"
                + " }"
                + "}");
        assertEquals(3 * col.placed("a").bounds.widthY(), col.placed("b").bounds.widthY(), 0.002);
    }

    @Test
    public void nonPositiveGridWeightsUseTheDefaultTrackWeight() {
        RenderResult r = render("page{"
                + " grid(2,1,size:(200px,100px)){"
                + "  rectangle(name:\"a\",grid-weight:0)"
                + "  rectangle(name:\"b\",grid-weight:-2)"
                + " }"
                + "}");
        assertEquals(r.placed("a").bounds.widthX(), r.placed("b").bounds.widthX(), 0.002);
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
        return new RenderResult(placed);
    }

    private static final class RenderResult {
        final List<NTxPlaced> placed;

        RenderResult(List<NTxPlaced> placed) {
            this.placed = placed;
        }

        NTxPlaced placed(String name) {
            NTxPlaced result = placed.stream().filter(p -> name.equals(p.name)).findFirst().orElse(null);
            assertNotNull(result, "missing placed node " + name + " in " + placed);
            return result;
        }
    }
}
