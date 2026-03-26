package net.thevpc.ntexup.api.document.security;

import net.thevpc.nuts.util.NCopiable;

import java.io.Serializable;

public class NTxManifestOptions implements Serializable, NCopiable, Cloneable {
    private boolean sign;
    private boolean publish;
    
    public NTxManifestOptions() {
    }

    public boolean isSign() {
        return sign;
    }

    public NTxManifestOptions setSign(boolean sign) {
        this.sign = sign;
        return this;
    }

    public boolean isPublish() {
        return publish;
    }

    public NTxManifestOptions setPublish(boolean publish) {
        this.publish = publish;
        return this;
    }

    @Override
    public NTxManifestOptions copy() {
        return clone();
    }

    @Override
    protected NTxManifestOptions clone() {
        try {
            return (NTxManifestOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
