package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.logging.Level;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

public class JTextAreaNTxMessageList extends JPanel implements NTxLogger {
    private JTextArea view;
    private static int MAX_LINES=1024*1024;
    public JTextAreaNTxMessageList() {
        super(new BorderLayout());
        view = new JTextArea();
        view.setEditable(false);
        add(new JScrollPane(view));
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
        Instant time = Instant.now();
        NMsg nmsg = msg.message();
        Level type = nmsg.getLevel();
        NTxSource source = msg.source();
        if (type == null) {
            type = Level.INFO;
        }

        NMsg mm = NMsg.ofC("[%s] [%s] [%s] %s",
                time,
                type,
                source == null ? null : source.shortName(),
                nmsg
        ).withThrowable(nmsg.getThrowable());
        final String formattedMessage = mm.toString() + "\n";
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Document doc = view.getDocument();
                view.append(formattedMessage);

                // Handle the line capping
                Element root = doc.getDefaultRootElement();
                if (root.getElementCount() > MAX_LINES) {
                    try {
                        // Calculate how many lines to remove
                        int linesToRemove = root.getElementCount() - MAX_LINES;
                        // Get the offset of the end of the last line to be removed
                        int endOffset = root.getElement(linesToRemove - 1).getEndOffset();

                        // Remove the old text from the beginning
                        doc.remove(0, endOffset);
                    } catch (BadLocationException e) {
                        // This shouldn't happen with correct index math
                        e.printStackTrace();
                    }
                }

                // Optional: Auto-scroll to bottom
                view.setCaretPosition(doc.getLength());
            }
        });
    }

    public void updateContent() {

    }
}
