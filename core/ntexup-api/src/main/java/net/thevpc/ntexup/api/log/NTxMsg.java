package net.thevpc.ntexup.api.log;

import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

public class NTxMsg {
    private NMsg message;
    private Throwable error;
    private NTxSource source;

    public static NTxMsg of(NMsg message, Throwable error, NTxSource source) {
        return new NTxMsg(message, error, source);
    }
    public static NTxMsg of(NMsg message, NTxSource source) {
        return new NTxMsg(message, null, source);
    }

    public static NTxMsg of(NMsg message) {
        return new NTxMsg(message, null, null);
    }

    public NTxMsg(NMsg message, Throwable error, NTxSource source) {
        this.message = message;
        this.error = error;
        this.source = source;
    }

    public NMsg message() {
        return message;
    }

    public Throwable error() {
        return error;
    }

    public NTxSource source() {
        return source;
    }
}
