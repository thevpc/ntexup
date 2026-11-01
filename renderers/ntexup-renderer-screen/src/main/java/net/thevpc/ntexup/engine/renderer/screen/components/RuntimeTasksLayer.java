package net.thevpc.ntexup.engine.renderer.screen.components;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxPendingTask;
import net.thevpc.ntexup.api.eval.NTxObj;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.engine.renderer.screen.DocumentView;
import net.thevpc.ntexup.engine.renderer.screen.PageView;
import net.thevpc.nuts.elem.NElement;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Corner badge listing the background computations still running, on every slide,
 * so a long solve is visible from pages that do not depend on it.
 */
public class RuntimeTasksLayer implements NTxDocumentLayer {
    private static final int MAX_NAMES = 3;

    /**
     * @return true when the badge has something to show, which also means the
     * view has to keep repainting for the spinner and elapsed time to move
     */
    public static boolean isActive(DocumentView doc) {
        NTxCompiledDocument d = doc.compiledDocument();
        return d != null && d.hasPendingFutures() && isEnabled(d);
    }

    private static boolean isEnabled(NTxCompiledDocument d) {
        NTxObj setting = d.getGlobalObject(NTxCompiledDocument.VAR_SHOW_RUNNING_TASKS).orNull();
        if (setting == null) {
            return true;
        }
        NElement value = setting.toElement();
        return value == null || !value.isBoolean() || value.asBooleanValue().orElse(true);
    }

    public void draw(DocumentView doc, PageView pageView, Dimension size, NTxGraphics g) {
        if (!isActive(doc)) {
            return;
        }
        String text = describe(doc.compiledDocument().pendingTasks());

        Font font = new Font("SansSerif", Font.BOLD, 12);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        double spinner = 14;
        double w = fm.stringWidth(text) + spinner + 28;
        double h = 26;
        double x = size.getWidth() - w - 12;
        // Above the page index pill, which sits on the same right edge
        double y = size.getHeight() - h - 38;

        g.setComposite(AlphaComposite.SrcOver.derive(0.85f));
        g.setColor(new Color(0x0f172a));
        g.fillRoundRect(x, y, w, h, 13, 13);

        double sx = x + 8;
        double sy = y + (h - spinner) / 2.0;
        long angle = (System.currentTimeMillis() / 3) % 360;
        g.setComposite(AlphaComposite.SrcOver.derive(0.25f));
        g.setColor(Color.WHITE);
        g.drawArc(sx, sy, spinner, spinner, 0, 360);
        g.setComposite(AlphaComposite.SrcOver.derive(0.95f));
        g.setColor(new Color(0x38bdf8));
        g.drawArc(sx, sy, spinner, spinner, (int) -angle, 120);

        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString(text, (int) (sx + spinner + 6), (int) (y + (h + fm.getAscent() - fm.getDescent()) / 2.0));
        g.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }

    private static String describe(List<NTxPendingTask> tasks) {
        if (tasks.isEmpty()) {
            // Only futures registered without a name are left
            return "Computing...";
        }
        String names = tasks.stream().limit(MAX_NAMES).map(NTxPendingTask::name).collect(Collectors.joining(", "));
        if (tasks.size() > MAX_NAMES) {
            names += " +" + (tasks.size() - MAX_NAMES);
        }
        long seconds = tasks.get(0).elapsedMillis() / 1000;
        return tasks.size() + " running: " + names + String.format(" · %d:%02d", seconds / 60, seconds % 60);
    }
}
