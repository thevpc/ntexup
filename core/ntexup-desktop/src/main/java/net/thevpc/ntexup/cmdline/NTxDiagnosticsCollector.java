package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Collects the warnings and errors logged while a command line action runs, so that
 * the command can end with a summary, a machine readable report and a meaningful
 * exit code (the engine itself keeps going after most errors and still writes output).
 */
public class NTxDiagnosticsCollector implements NTxLogger {

    public static class Diagnostic {
        public final String level;
        public final String source;
        public final String message;

        Diagnostic(String level, String source, String message) {
            this.level = level;
            this.source = source;
            this.message = message;
        }

        String key() {
            return level + "|" + source + "|" + message;
        }
    }

    private final List<Diagnostic> errors = new ArrayList<>();
    private final List<Diagnostic> warnings = new ArrayList<>();
    private final Set<String> seen = new LinkedHashSet<>();
    private final List<String> outputs = new ArrayList<>();

    @Override
    public void log(NMsg message) {
        log(NTxMsg.of(message));
    }

    @Override
    public void log(NMsg message, NTxSource source) {
        log(NTxMsg.of(message, source));
    }

    @Override
    public synchronized void log(NTxMsg msg) {
        NMsg m = msg.message();
        if (m == null) {
            return;
        }
        Level level = m.level();
        if (level == null || level.intValue() < Level.WARNING.intValue()) {
            return;
        }
        boolean error = level.intValue() >= Level.SEVERE.intValue();
        NTxSource src = msg.source();
        String text = String.valueOf(m);
        if (m.throwable() != null && !text.contains(String.valueOf(m.throwable()))) {
            text = text + " : " + m.throwable();
        }
        Diagnostic d = new Diagnostic(error ? "error" : "warning", src == null ? "engine" : src.shortName(), text);
        if (seen.add(d.key())) {
            (error ? errors : warnings).add(d);
        }
    }

    public synchronized void addOutput(NPath output) {
        outputs.add(output.normalize().toAbsolute().toString());
    }

    public synchronized List<Diagnostic> errors() {
        return new ArrayList<>(errors);
    }

    public synchronized List<Diagnostic> warnings() {
        return new ArrayList<>(warnings);
    }

    public synchronized String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"status\": \"").append(errors.isEmpty() ? (warnings.isEmpty() ? "ok" : "warnings") : "errors").append("\",\n");
        sb.append("  \"errorCount\": ").append(errors.size()).append(",\n");
        sb.append("  \"warningCount\": ").append(warnings.size()).append(",\n");
        sb.append("  \"outputs\": [");
        for (int i = 0; i < outputs.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(quote(outputs.get(i)));
        }
        sb.append("],\n  \"diagnostics\": [");
        List<Diagnostic> all = new ArrayList<>(errors);
        all.addAll(warnings);
        for (int i = 0; i < all.size(); i++) {
            Diagnostic d = all.get(i);
            sb.append(i == 0 ? "\n" : ",\n")
                    .append("    {\"level\": ").append(quote(d.level))
                    .append(", \"source\": ").append(quote(d.source))
                    .append(", \"message\": ").append(quote(d.message)).append("}");
        }
        sb.append(all.isEmpty() ? "]\n}\n" : "\n  ]\n}\n");
        return sb.toString();
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : String.valueOf(s).toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
