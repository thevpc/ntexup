package net.thevpc.ntexup.engine.log;

import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.log.NLog;
import net.thevpc.nuts.log.NMsgIntent;
import net.thevpc.nuts.util.NMsg;

import java.time.Instant;
import java.util.logging.Level;

public class NTxLoggerDelegateImpl implements NTxLogger {
    private NTxSource defaultSource;
    private NTxLogger other;
    private int errorCount;

    public NTxLoggerDelegateImpl(NTxSource defaultSource, NTxLogger other) {
        this.defaultSource = defaultSource;
        this.other = other;
    }

    public boolean isSelfSuccessful() {
        return errorCount == 0;
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
        Instant time = Instant.now();
        NMsg msg = message.message();
        Level level = msg.getLevel();
        if (level == null) {
            level = Level.INFO;
        }
        Throwable error = message.error();
        msg = msg.withLevel(level);
        NTxSource source = message.source();
        if (source == null) {
            source = defaultSource;
        }
//        session.out().println(NMsg.ofC("[%s] [%s] [%s] %s", time, type,
//                source == null ? null : source.shortName(),
//                message.getMessage()
//        ));
//        if (error != null) {
//            for (String s : NReservedLangUtils.stacktraceToArray(error)) {
//                session.out().println(NMsg.ofC("\t%s", s));
//            }
//        }
        if (level.intValue() >= Level.SEVERE.intValue()) {
            errorCount++;
        }
        if (other != null) {
            other.log(NTxMsg.of(msg, error, source));
        }
        NLog.of(getClass()).log(NMsg.ofC("%s %s",source, msg).asInfo().withThrowable(error));
    }
}
