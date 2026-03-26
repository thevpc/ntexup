package net.thevpc.ntexup.api.log;

import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

public class NTxMsg {
    private NMsg message;
    private NTxSource source;

    public static NTxMsg of(NMsg message, NTxSource source) {
        return new NTxMsg(message, source);
    }

    public static NTxMsg of(NMsg message) {
        return new NTxMsg(message, null);
    }

    public NTxMsg(NMsg message, NTxSource source) {
        this.message = message;
        this.source = source;
    }

    public NMsg message() {
        return message;
    }

    public NTxSource source() {
        return source;
    }
}
