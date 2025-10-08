package net.thevpc.ntexup.engine.log;

import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

import java.util.logging.Level;

public class SilentNTxLogger implements NTxLogger {
    private int errorCount;

    public SilentNTxLogger() {

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
    public void log(NTxMsg message) {
        NMsg msg = message.message();
        Level level = msg.getLevel();
        if (level == null) {
            level = Level.INFO;
        }
        if (level.intValue() >= Level.SEVERE.intValue()) {
            errorCount++;
        }
    }

    public boolean isSuccessful() {
        return errorCount == 0;
    }

    public int getErrorCount() {
        return errorCount;
    }
}
