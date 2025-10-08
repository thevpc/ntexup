package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.logging.Level;

public class JTextAreaNTxMessageList extends JPanel implements NTxLogger {
    private JTextArea view;

    public JTextAreaNTxMessageList() {
        super(new BorderLayout());
        view = new JTextArea();
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
        Throwable error = msg.error();
        NTxSource source = msg.source();
        if (type == null) {
            type = Level.INFO;
        }

        NMsg mm = NMsg.ofC("[%s] [%s] [%s] %s",
                time,
                type,
                source == null ? null : source.shortName(),
                nmsg
        );
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.append(mm.toString());
                view.append("\n");
            }
        });
    }

    public void updateContent() {

    }
}
