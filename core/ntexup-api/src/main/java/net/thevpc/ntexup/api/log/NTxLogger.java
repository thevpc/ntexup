package net.thevpc.ntexup.api.log;


import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

public interface NTxLogger {

    void log(NTxMsg message);
    void log(NMsg message);
    void log(NMsg message, NTxSource source);
}
