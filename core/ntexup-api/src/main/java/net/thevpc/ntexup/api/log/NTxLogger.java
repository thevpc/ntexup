package net.thevpc.ntexup.api.log;


import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.log.NLogger;
import net.thevpc.nuts.text.NMsg;

public interface NTxLogger extends NLogger {

    void log(NTxMsg message);
    void log(NMsg message, NTxSource source);
}
