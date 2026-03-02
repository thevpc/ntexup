package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.util.NMaps;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NRef;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class PageView extends JComponent {
    public static Dimension REF_SIZE = new Dimension(1024, 768);
    private NTxCompiledPage page;
    private long pageStartTime;
    private String uuid;
    private NTxEngine engine;
    private NTxCompiledDocument document;
    private final NRef<Dimension> lastSize = NRef.ofNull();
    private final double ratio = 16.0 / 9.0;

    public PageView(
            NTxCompiledDocument document,
            NTxCompiledPage page,
            NTxEngine engine
    ) {
        this.document = document;
        this.page = page;
        this.uuid = UUID.randomUUID().toString();
        this.engine = engine;
    }

    public NTxEngine engine() {
        return engine;
    }

    public JComponent component() {
        return this;
    }

    public String id() {
        return uuid;
    }

    public int index() {
        return page.index();
    }

    void onHide() {

    }

    synchronized void onShow() {
        page.compiledPage();
        this.pageStartTime = System.currentTimeMillis();
    }

    public boolean isLoading() {
        return !page.isCompiled() || !document.isCompiled();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (page.isCompiled()) {
            NChronometer c = NChronometer.startNow();
            Dimension size = getSize();
            Dimension lastSize=null;
            boolean someChange = false;
            synchronized (this) {
                lastSize = this.lastSize.get();
                someChange = !size.equals(lastSize);
                if (someChange) {
                    this.lastSize.set(size);
                }
            }
            Graphics2D g2d = (Graphics2D) g;
            NRef<NTxNode> pageNode = NRef.ofNull();
//            if(true) {
//                int pw = getWidth();
//                int ph = getHeight();
//
//                // 1. compute inner ratio rectangle
//                int w = pw;
//                int h = (int) (w / ratio);
//
//                if (h > ph) {
//                    h = ph;
//                    w = (int) (h * ratio);
//                }
//
//                int x = (pw - w) / 2;
//                int y = (ph - h) / 2;
//                // 2. paint boundary
//                g2d.setColor(Color.DARK_GRAY);  // your letterbox color
//                g2d.fillRect(0, 0, pw, ph);
//
//                // 3. clip to inner rect (optional but cleaner)
//                Shape oldClip = g2d.getClip();
//                g2d.setClip(x, y, w, h);
//
//                // 4. scale your graphics to fit the inner rectangle
//                double scaleX = w / REF_SIZE.getWidth();
//                double scaleY = h / REF_SIZE.getHeight();
//                double scale = Math.min(scaleX, scaleY);
//
//                g2d.translate(x, y);
//                g2d.scale(scale, scale);
//                renderPage(g2d, size.getWidth(), size.getHeight(), someChange, pageNode);
//                g2d.setClip(oldClip);
//            }else{
            renderPage(g2d, size.getWidth(), size.getHeight(), someChange, pageNode);
//            }
            c.stop();
            if (someChange) {
                engine().log().log(NMsg.ofC("[%s] paintComponent %s in %s (%s -> %s)", NTxUtils.sourceOf(pageNode.get()), page.index(), c, lastSize,size));
            }
        }
    }

    private void renderPage(Graphics2D g2d, double width, double height, boolean someChange, NRef<NTxNode> pageNode) {
        NTxNodeRendererConfig config = new NTxNodeRendererConfig();
        config.setWidth(width);
        config.setHeight(height);
        config.setCapabilities(NMaps.of(NTxRendererContext.CAPABILITY_ANIMATE, true));
        config.setStartTime(pageStartTime);
        config.setUseCache(!someChange);
        engine.renderPage(page, config,g2d,this,this::repaint);
        NTxNode p = page.compiledPage();
        pageNode.set(p);
    }


    public Object source() {
        return page.source();
    }

    public NTxCompiledPage page() {
        return page;
    }
}
