package net.thevpc.ntexup.engine.renderer.dump;

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
public class DumpRecorder {

    private final List<Object> entries;
    private final DumpRecorder parent;

    public DumpRecorder() {
        this(null);
    }

    private DumpRecorder(DumpRecorder parent) {
        this.parent = parent;
        this.entries = new ArrayList<>();
        if (parent != null) {
            parent.entries.add(this);
        }
    }

    /**
     * Opens a nested group bound to a cloned graphics and returns its recorder.
     */
    public DumpRecorder branch() {
        return new DumpRecorder(this);
    }

    public void record(NElement op) {
        entries.add(op);
    }

    public boolean hasContent() {
        for (Object e : entries) {
            if (!(e instanceof DumpRecorder r) || r.hasContent()) {
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
            if (e instanceof DumpRecorder r) {
                if (r.hasContent()) {
                    NObjectElementBuilder g = DumpFormat.op("group");
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
