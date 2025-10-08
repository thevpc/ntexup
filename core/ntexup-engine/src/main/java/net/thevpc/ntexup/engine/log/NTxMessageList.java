package net.thevpc.ntexup.engine.log;

import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

import java.util.ArrayList;
import java.util.List;

public class NTxMessageList implements NTxLogger {
    private List<NTxLogger> registeredMessages = new ArrayList<>();

    @Override
    public void log(NTxMsg msg) {
        for (NTxLogger r : registeredMessages) {
            r.log(msg);
        }
    }

    @Override
    public void log(NMsg message) {
        for (NTxLogger r : registeredMessages) {
            r.log(message);
        }
    }

    @Override
    public void log(NMsg message, NTxSource source) {
        for (NTxLogger r : registeredMessages) {
            r.log(message,source);
        }
    }

    public void add(NTxLogger a) {
        if (a != null) {
            registeredMessages.add(a);
        }
    }

    public void remove(NTxLogger a) {
        if (a != null) {
            registeredMessages.remove(a);
        }
    }
}
