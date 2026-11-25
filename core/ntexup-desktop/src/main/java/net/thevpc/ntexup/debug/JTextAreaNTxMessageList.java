package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * Bounded, lazily-populated log view.
 * <p>
 * The engine logs every page compilation and render pass. Keeping that stream
 * in a Swing document is very expensive: each appended line becomes a
 * {@code GlyphView}/{@code TextLayout}, and the old one-million-line cap could
 * pin several gigabytes for a large deck and make navigation crawl.
 * <p>
 * This view therefore:
 * <ul>
 *     <li>keeps at most {@link #MAX_LINES} lines,</li>
 *     <li>only touches the Swing document while the debug frame is showing;
 *     when hidden it stores the last lines in a small ring buffer,</li>
 *     <li>flushes that buffer when the frame becomes visible.</li>
 * </ul>
 */
public class JTextAreaNTxMessageList extends JPanel implements NTxLogger {
    private static final int MAX_LINES = 2000;
    private static final int TRIM_LINES = 200;

    private final JTextArea view;
    private final Deque<String> pending = new ArrayDeque<>();
    private volatile boolean shown;

    public JTextAreaNTxMessageList() {
        super(new BorderLayout());
        view = new JTextArea();
        view.setEditable(false);
        add(new JScrollPane(view));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        shown = true;
        flushPending();
    }

    @Override
    public void removeNotify() {
        shown = false;
        super.removeNotify();
    }

    @Override
    public void log(NMsg message) {
        log(NTxMsg.of(message));
    }

    @Override
    public void log(NMsg message, NTxSource source) {
        log(NTxMsg.of(message, source));
    }

    @Override
    public void log(NTxMsg msg) {
        String formatted = format(msg);
        if (!shown) {
            synchronized (pending) {
                pending.addLast(formatted);
                while (pending.size() > MAX_LINES) {
                    pending.removeFirst();
                }
            }
            return;
        }
        SwingUtilities.invokeLater(() -> appendLine(formatted));
    }

    private static String format(NTxMsg msg) {
        Instant time = Instant.now();
        NMsg nmsg = msg.message();
        Level type = nmsg.level();
        NTxSource source = msg.source();
        if (type == null) {
            type = Level.INFO;
        }
        return NMsg.ofC("[%-30s] [%s] [%s] %s",
                time,
                type,
                source == null ? null : source.shortName(),
                nmsg
        ).withThrowable(nmsg.throwable()).toString() + "\n";
    }

    private void flushPending() {
        List<String> copy;
        synchronized (pending) {
            if (pending.isEmpty()) {
                return;
            }
            copy = new ArrayList<>(pending);
            pending.clear();
        }
        SwingUtilities.invokeLater(() -> {
            for (String line : copy) {
                appendLine(line);
            }
        });
    }

    private void appendLine(String line) {
        if (!shown) {
            // Frame was hidden again before the EDT ran.
            synchronized (pending) {
                pending.addLast(line);
                while (pending.size() > MAX_LINES) {
                    pending.removeFirst();
                }
            }
            return;
        }
        Document doc = view.getDocument();
        view.append(line);

        Element root = doc.getDefaultRootElement();
        int count = root.getElementCount();
        if (count > MAX_LINES) {
            int remove = count - MAX_LINES + TRIM_LINES;
            try {
                doc.remove(0, root.getElement(remove - 1).getEndOffset());
            } catch (BadLocationException e) {
                // Should not happen with correct index math.
            }
        }
    }

    public void updateContent() {
    }
}
