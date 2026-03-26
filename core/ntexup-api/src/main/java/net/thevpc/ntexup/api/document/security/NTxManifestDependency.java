package net.thevpc.ntexup.api.document.security;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.util.NCopiable;
import net.thevpc.nuts.util.NOptional;

public class NTxManifestDependency implements NToElement, NCopiable {
    private String value;
    private String fingerprint;

    public String getFingerprint() {
        return fingerprint;
    }

    public NTxManifestDependency setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
        return this;
    }

    public static NOptional<NTxManifestDependency> parse(NElement element) {
        if (element == null) {
            return NOptional.ofNamedEmpty("dependency");
        }
        if (element.isNull()) {
            return NOptional.ofNamedEmpty("dependency");
        }
        if (element.isObject()) {
            NObjectElement o = element.asObject().get();
            return NOptional.of(new NTxManifestDependency()
                    .setFingerprint(o.get("fingerprint")
                            .flatMap(x->x.asStringValue())
                            .orNull())
                    .setValue(o.get("value")
                            .flatMap(x->x.asStringValue())
                            .orNull())

            );
        }
        return NOptional.ofNamedError("dependency");
    }

    public NTxManifestDependency setValue(String value) {
        this.value = value;
        return this;
    }

    public String getValue() {
        return value;
    }

    @Override
    protected NTxManifestDependency clone() {
        try {
            return (NTxManifestDependency) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NElement toElement() {
        return NElement.ofString(value);
    }

    @Override
    public NTxManifestDependency copy() {
        return clone();
    }
}

