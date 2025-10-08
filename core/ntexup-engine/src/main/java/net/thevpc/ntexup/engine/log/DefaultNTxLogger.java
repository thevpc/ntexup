package net.thevpc.ntexup.engine.log;

import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NStringUtils;

import java.time.Instant;
import java.util.logging.Level;

public class DefaultNTxLogger implements NTxLogger {
    private NTxSource defaultSource;

    public DefaultNTxLogger() {

    }
    public DefaultNTxLogger(NTxSource defaultSource) {
        this.defaultSource = defaultSource;
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
        NMsg nmsg=msg.message();
        Level type=nmsg.getLevel();
        Throwable error=msg.error();
        NTxSource source=msg.source();
        if (type == null) {
            type = Level.INFO;
        }
        if (source == null) {
            source = defaultSource;
        }
        NOut.resetLine().println(NMsg.ofC("[%s] [%s] [%s] %s", time, type,
                source == null ? null : source.shortName(),
                nmsg
        ));
        if (error != null) {
            for (String s : NStringUtils.stacktraceArray(error)) {
                NOut.resetLine().println(NMsg.ofC("\t%s", s));
            }
        }
    }
}
