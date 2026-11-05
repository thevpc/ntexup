package net.thevpc.ntexup.engine.renderer.htmlstatic;

import net.thevpc.nuts.elem.NArrayElementBuilder;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.elem.NPairElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compresses the baked frames of one page into what gets shipped.
 * <p>
 * Two effects dominate the payload of an animated page. The same primitives
 * recur almost unchanged from frame to frame — a flow of dots repaints three
 * operations out of forty — and the expensive part of an operation, a glyph
 * outline or a gradient, is a long string repeated verbatim over and over. So:
 * <ul>
 *     <li>every composite value is interned bottom-up into one per-page
 *     {@code defs} array and replaced by {@code {"$":id}}. Interning the leaf
 *     strings first is what lets operations that merely reuse the same shape
 *     collapse to the same id too;</li>
 *     <li>every frame after the first ships as the operation slots that
 *     changed, and falls back to a full frame once the patch stops paying for
 *     itself.</li>
 * </ul>
 * The shipped form is plain JSON with a single twist: TSON object headers, which
 * JSON has no way to spell, become a {@code k} field. Nothing else about the
 * recorder's field names changes, so the browser decoder is a direct
 * transcription of {@link #encodeValue}, and {@link #decode(NElement)} performs
 * the same walk in java to prove the round trip is lossless.
 */
class HtmlStaticCodec {

    static final int FORMAT_VERSION = 1;
    /**
     * Values that encode to less than this stay inline: interning a two digit
     * number costs more than it saves.
     */
    private static final int INLINE_MAX_LENGTH = 24;
    /**
     * Beyond this share of the full frame, a patch ships the frame instead.
     */
    private static final double FULL_FRAME_RATIO = 0.55;

    private final boolean interning;
    private final Map<String, Integer> defIds = new LinkedHashMap<>();
    private final List<NElement> defValues = new ArrayList<>();

    HtmlStaticCodec() {
        this(true);
    }

    HtmlStaticCodec(boolean interning) {
        this.interning = interning;
    }

    /**
     * Builds {@code {defs:[...],frames:[{t,ops}|{t,patch,len},...]}}.
     */
    NElement encodePage(List<HtmlStaticBaker.Frame> frames) {
        List<List<NElement>> encoded = new ArrayList<>();
        for (HtmlStaticBaker.Frame frame : frames) {
            List<NElement> slots = new ArrayList<>();
            for (NElement op : frame.ops()) {
                slots.add(encodeValue(op));
            }
            encoded.add(slots);
        }
        NArrayElementBuilder out = NElement.ofArrayBuilder();
        List<NElement> previous = null;
        for (int i = 0; i < frames.size(); i++) {
            List<NElement> slots = encoded.get(i);
            long millis = frames.get(i).millis();
            if (previous == null) {
                out.add(fullFrame(millis, slots));
            } else {
                NArrayElementBuilder patch = NElement.ofArrayBuilder();
                long patchBytes = 0;
                int changes = 0;
                for (int k = 0; k < slots.size(); k++) {
                    NElement before = k < previous.size() ? previous.get(k) : null;
                    NElement now = slots.get(k);
                    if (before == null || !before.toString().equals(now.toString())) {
                        NArrayElementBuilder entry = NElement.ofArrayBuilder();
                        entry.add(NElement.of(k));
                        entry.add(now);
                        patch.add(entry.build());
                        patchBytes += now.toString().length() + 8;
                        changes++;
                    }
                }
                long fullBytes = 0;
                for (NElement op : slots) {
                    fullBytes += op.toString().length() + 2;
                }
                boolean cheap = changes == 0 || patchBytes <= (long) (fullBytes * FULL_FRAME_RATIO);
                if (cheap) {
                    NObjectElementBuilder ob = NElement.ofObjectBuilder();
                    ob.add("t", NElement.of(millis));
                    ob.add("patch", patch.build());
                    if (slots.size() != previous.size()) {
                        ob.add("len", NElement.of(slots.size()));
                    }
                    out.add(ob.build());
                } else {
                    out.add(fullFrame(millis, slots));
                }
            }
            previous = slots;
        }
        NArrayElementBuilder defs = NElement.ofArrayBuilder();
        for (NElement d : defValues) {
            defs.add(d);
        }
        NObjectElementBuilder page = NElement.ofObjectBuilder();
        page.add("defs", defs.build());
        page.add("frames", out.build());
        return page.build();
    }

    private NElement fullFrame(long millis, List<NElement> slots) {
        NArrayElementBuilder ops = NElement.ofArrayBuilder();
        for (NElement op : slots) {
            ops.add(op);
        }
        NObjectElementBuilder ob = NElement.ofObjectBuilder();
        ob.add("t", NElement.of(millis));
        ob.add("ops", ops.build());
        return ob.build();
    }

    /**
     * Composites and long strings move to the defs table, scalars stay where
     * they are so a short operation costs nothing extra.
     */
    private NElement encodeValue(NElement value) {
        if (value == null || value.isNull()) {
            return NElement.ofNull();
        }
        boolean composite = value.asObject().isPresent() || value.asArray().isPresent();
        if (!composite) {
            String text = value.toString();
            if (!interning || text.length() <= INLINE_MAX_LENGTH) {
                return plain(value);
            }
            return intern(text, plain(value));
        }
        NElement encoded = encodeComposite(value);
        if (!interning || encoded.toString().length() <= INLINE_MAX_LENGTH) {
            return encoded;
        }
        return intern(encoded.toString(), encoded);
    }

    private NElement encodeComposite(NElement value) {
        if (value.asObject().isPresent()) {
            NObjectElementBuilder ob = NElement.ofObjectBuilder();
            String header = headerOf(value);
            if (header != null && !header.isEmpty()) {
                ob.add("k", NElement.of(header));
            }
            for (NPairElement pair : value.asObject().get().pairs()) {
                ob.add(keyOf(pair.key()), encodeValue(pair.value()));
            }
            return ob.build();
        }
        NArrayElementBuilder a = NElement.ofArrayBuilder();
        for (NElement item : value.asArray().get()) {
            a.add(encodeValue(item));
        }
        return a.build();
    }

    /**
     * Keeps a scalar usable as JSON: names and single quoted literals must
     * become plain strings.
     */
    private static NElement plain(NElement value) {
        if (value.isString()) {
            return NElement.of(value.asStringValue().orElse(""));
        }
        if (value.isNumber() || value.isBoolean() || value.isNamed()) {
            return value;
        }
        return NElement.of(value.toString());
    }

    private NElement intern(String canonical, NElement encoded) {
        Integer id = defIds.get(canonical);
        if (id == null) {
            id = defValues.size();
            defIds.put(canonical, id);
            defValues.add(encoded);
        }
        NObjectElementBuilder ob = NElement.ofObjectBuilder();
        ob.add("$", NElement.of(id));
        return ob.build();
    }

    private NElement lookup(int id) {
        return defValues.get(id);
    }

    /**
     * Rebuilds the operation list of every frame from a shipped page, resolving
     * defs references and applying patches: the walk the player performs.
     *
     * @param page       the shipped page element, read back from disk
     * @param definitions the defs of that page, already parsed
     */
    static List<List<NElement>> decode(NElement page) {
        List<NElement> defs = new ArrayList<>();
        NElement defsElement = field(page, "defs");
        if (defsElement != null && defsElement.asArray().isPresent()) {
            for (NElement d : defsElement.asArray().get()) {
                defs.add(d);
            }
        }
        HtmlStaticCodec codec = new HtmlStaticCodec();
        codec.defValues.addAll(defs);
        List<List<NElement>> frames = new ArrayList<>();
        NElement framesElement = field(page, "frames");
        if (framesElement == null || !framesElement.asArray().isPresent()) {
            return frames;
        }
        List<NElement> current = new ArrayList<>();
        for (NElement frame : framesElement.asArray().get()) {
            NElement ops = field(frame, "ops");
            if (ops != null && ops.asArray().isPresent()) {
                current = new ArrayList<>();
                for (NElement op : ops.asArray().get()) {
                    current.add(codec.resolve(op));
                }
            } else {
                NElement patch = field(frame, "patch");
                if (patch != null && patch.asArray().isPresent()) {
                    for (NElement entry : patch.asArray().get()) {
                        List<NElement> items = new ArrayList<>();
                        if (entry.asArray().isPresent()) {
                            for (NElement item : entry.asArray().get()) {
                                items.add(item);
                            }
                        }
                        if (items.size() < 2) {
                            continue;
                        }
                        int index = items.get(0).asIntValue().orElse(-1);
                        if (index < 0) {
                            continue;
                        }
                        while (current.size() < index) {
                            current.add(NElement.ofNull());
                        }
                        NElement value = codec.resolve(items.get(1));
                        if (index == current.size()) {
                            current.add(value);
                        } else {
                            current.set(index, value);
                        }
                    }
                }
                NElement len = field(frame, "len");
                if (len != null && len.asIntValue().isPresent()) {
                    int size = len.asIntValue().get();
                    while (current.size() > size) {
                        current.remove(current.size() - 1);
                    }
                }
            }
            frames.add(new ArrayList<>(current));
        }
        return frames;
    }

    /**
     * Replaces {@code {"$":id}} by the defs entry it names, all the way down.
     */
    private NElement resolve(NElement value) {
        if (value.asObject().isPresent()) {
            NElement ref = field(value, "$");
            if (ref != null && ref.asIntValue().isPresent()) {
                return resolve(lookup(ref.asIntValue().get()));
            }
            NObjectElementBuilder ob = NElement.ofObjectBuilder();
            for (NPairElement pair : value.asObject().get().pairs()) {
                ob.add(keyOf(pair.key()), resolve(pair.value()));
            }
            return ob.build();
        }
        if (value.asArray().isPresent()) {
            NArrayElementBuilder a = NElement.ofArrayBuilder();
            for (NElement item : value.asArray().get()) {
                a.add(resolve(item));
            }
            return a.build();
        }
        return value;
    }

    /**
     * Inverts the {@code k} header encoding so a decoded operation can be
     * compared, element for element, with what the recorder produced.
     */
    static NElement toRecorderForm(NElement jsonValue) {
        if (jsonValue.asObject().isPresent()) {
            String header = null;
            List<NPairElement> pairs = new ArrayList<>(jsonValue.asObject().get().pairs());
            List<NPairElement> rest = new ArrayList<>();
            for (NPairElement pair : pairs) {
                String key = keyOf(pair.key());
                NElement child = toRecorderForm(pair.value());
                if ("k".equals(key) && child.asStringValue().isPresent()) {
                    header = child.asStringValue().get();
                } else {
                    rest.add(NElement.ofPair(key, child));
                }
            }
            if (header == null) {
                NObjectElementBuilder ob = NElement.ofObjectBuilder();
                for (NPairElement pair : rest) {
                    ob.add(pair);
                }
                return ob.build();
            }
            NObjectElementBuilder ob = NElement.ofObjectBuilder(header);
            for (NPairElement pair : rest) {
                ob.add(pair);
            }
            return ob.build();
        }
        if (jsonValue.asArray().isPresent()) {
            NArrayElementBuilder a = NElement.ofArrayBuilder();
            for (NElement item : jsonValue.asArray().get()) {
                a.add(toRecorderForm(item));
            }
            return a.build();
        }
        return jsonValue;
    }

    static NElement field(NElement container, String name) {
        return container.asListContainer().flatMap(c -> c.get(name)).orNull();
    }

    static String headerOf(NElement e) {
        return e.asNamed().flatMap(n -> n.name()).orElse(null);
    }

    static String keyOf(NElement key) {
        String s = key.toString();
        if (s.length() > 1 && s.charAt(0) == '"') {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }
}
