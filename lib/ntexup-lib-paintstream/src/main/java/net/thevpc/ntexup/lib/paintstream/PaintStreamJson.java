package net.thevpc.ntexup.lib.paintstream;

import net.thevpc.nuts.elem.NArrayElement;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.text.NMsg;

import java.nio.charset.StandardCharsets;

/**
 * Writes a shipped paint stream as strict JSON.
 * <p>
 * The nuts element writer always indents, and the payload is an export artifact
 * downloaded by a browser, so the format owns its own serialization: one line
 * per value when compact, plain {@code JSON.parse} input either way. The element
 * tree it receives is the one {@link PaintStreamCodec} produced, which means a
 * TSON object header has already become a {@code k} field — anything still named
 * here would be unspellable in JSON and fails the write instead of shipping a
 * file the browser cannot read.
 * <p>
 * Public because every artifact of the format is written with it: the page
 * payloads, the manifest and the container descriptor all have to be readable by
 * the same plain parser, on either side of the wire.
 */
public class PaintStreamJson {

    public static byte[] write(NElement element, boolean pretty) {
        StringBuilder sb = new StringBuilder(1024);
        value(element, sb, pretty, 0);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void value(NElement e, StringBuilder sb, boolean pretty, int depth) {
        if (e == null || e.isNull()) {
            sb.append("null");
            return;
        }
        NObjectElement object = e.asObject().orNull();
        if (object != null) {
            String header = PaintStreamCodec.headerOf(object);
            if (header != null && !header.isEmpty()) {
                throw new IllegalStateException(NMsg.ofC("paint-stream: cannot write json, %s is a named object",
                        e).toString());
            }
            object(object, sb, pretty, depth);
            return;
        }
        NArrayElement array = e.asArray().orNull();
        if (array != null) {
            array(array, sb, pretty, depth);
            return;
        }
        if (e.isNumber()) {
            sb.append(number(e));
            return;
        }
        if (e.isBoolean()) {
            sb.append(e.asBooleanValue().get());
            return;
        }
        String text = e.asStringValue().orNull();
        string(text == null ? e.toString() : text, sb);
    }

    private static String number(NElement e) {
        Number n = e.asNumberValue().orNull();
        if (n == null) {
            throw new IllegalStateException(NMsg.ofC("paint-stream: cannot write json, %s is not a plain number",
                    e).toString());
        }
        if (e.isFloatingNumber()) {
            return Double.toString(n.doubleValue());
        }
        return Long.toString(n.longValue());
    }

    private static void object(NObjectElement object, StringBuilder sb, boolean pretty, int depth) {
        if (object.size() == 0) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        boolean first = true;
        for (NPairElement pair : object.pairs()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            newline(sb, pretty, depth + 1);
            string(PaintStreamCodec.keyOf(pair.key()), sb);
            sb.append(':');
            if (pretty) {
                sb.append(' ');
            }
            value(pair.value(), sb, pretty, depth + 1);
        }
        newline(sb, pretty, depth);
        sb.append('}');
    }

    private static void array(NArrayElement array, StringBuilder sb, boolean pretty, int depth) {
        if (array.size() == 0) {
            sb.append("[]");
            return;
        }
        sb.append('[');
        boolean first = true;
        for (NElement item : array) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            newline(sb, pretty, depth + 1);
            value(item, sb, pretty, depth + 1);
        }
        newline(sb, pretty, depth);
        sb.append(']');
    }

    private static void newline(StringBuilder sb, boolean pretty, int depth) {
        if (!pretty) {
            return;
        }
        sb.append('\n');
        sb.append("  ".repeat(depth));
    }

    private static void string(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
