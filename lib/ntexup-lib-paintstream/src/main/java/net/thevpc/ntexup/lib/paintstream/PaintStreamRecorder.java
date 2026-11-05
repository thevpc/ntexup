package net.thevpc.ntexup.lib.paintstream;

import net.thevpc.nuts.elem.NArrayElementBuilder;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered tree of recorded paint operations for a single page.
 * <p>
 * A new group is opened each time the graphics is cloned
 * ({@code Graphics2D.create()} / {@code NTxGraphics.copy()}) which is exactly
 * what the engine does for rotation scopes, shadow scopes and custom graphics
 * contexts. The group tree therefore mirrors the component paint hierarchy
 * observed at render time, while every leaf operation carries its resolved
 * absolute state (transform, clip, paint...).
 */
public class PaintStreamRecorder {

    private final List<Object> entries;
    private final PaintStreamRecorder parent;
    private PaintStreamImageAssets images;
    private boolean textOutlines;

    public PaintStreamRecorder() {
        this(null);
    }

    private PaintStreamRecorder(PaintStreamRecorder parent) {
        this.parent = parent;
        this.entries = new ArrayList<>();
        this.images = parent == null ? null : parent.images;
        this.textOutlines = parent != null && parent.textOutlines;
        if (parent != null) {
            parent.entries.add(this);
        }
    }

    /**
     * When enabled, text operations also carry the java2d glyph outline as a
     * path so that a consumer (a canvas player, a vector writer) can reproduce
     * the painted text without any font, shaping or text layout of its own.
     * Disabled by default: the human-readable dump keeps text as strings.
     */
    public boolean isTextOutlines() {
        return textOutlines;
    }

    public void setTextOutlines(boolean textOutlines) {
        this.textOutlines = textOutlines;
        for (Object e : entries) {
            if (e instanceof PaintStreamRecorder r) {
                r.setTextOutlines(textOutlines);
            }
        }
    }

    /**
     * Image content-addressing registry shared with every group of this pass.
     */
    public PaintStreamImageAssets images() {
        return images;
    }

    /**
     * Enables image references on this recorder and on the groups already opened.
     */
    public void setImages(PaintStreamImageAssets images) {
        this.images = images;
        for (Object e : entries) {
            if (e instanceof PaintStreamRecorder r) {
                r.setImages(images);
            }
        }
    }

    /**
     * Opens a nested group bound to a cloned graphics and returns its recorder.
     */
    public PaintStreamRecorder branch() {
        return new PaintStreamRecorder(this);
    }

    public void record(NElement op) {
        entries.add(op);
    }

    public boolean hasContent() {
        for (Object e : entries) {
            if (!(e instanceof PaintStreamRecorder r) || r.hasContent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the {@code ops:[...]} array element of this recorder, dropping
     * empty groups.
     */
    public NElement opsElement() {
        NArrayElementBuilder a = NElement.ofArrayBuilder();
        for (Object e : entries) {
            if (e instanceof PaintStreamRecorder r) {
                if (r.hasContent()) {
                    NObjectElementBuilder g = PaintStreamFormat.op("group");
                    g.add("ops", r.opsElement());
                    a.add(g.build());
                }
            } else {
                a.add((NElement) e);
            }
        }
        return a.build();
    }

    /**
     * Returns the root list of recorded entries (elements and child groups).
     */
    public List<Object> getEntries() {
        return entries;
    }
}
