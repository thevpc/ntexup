package net.thevpc.ntexup.api.document.security;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NCopiable;
import net.thevpc.nuts.util.NOptional;

import java.time.Instant;
import java.util.Objects;

public class NTxManifestResource implements NToElement, NCopiable {
    private String value;
    private Instant lastVisited;
    private NTxManifestResourceType type;
    private String fingerPrint;

    public static NOptional<NTxManifestResource> parse(NElement element) {
        if (element == null) {
            return NOptional.ofNamedEmpty("resource");
        }
        if (element.isNull()) {
            return NOptional.ofNamedEmpty("resource");
        }
        if (element.isNamedObject()) {
            NObjectElement o = element.asObject().get();
            String n = o.name().get();
            NTxManifestResource r = new NTxManifestResource();
            try {
                r.setType(NTxManifestResourceType.valueOf(n.toUpperCase()));
            } catch (Exception ex) {
                return NOptional.ofError(NMsg.ofC("invalid resource type %s", n));
            }
            r.setValue(o.get("value").flatMap(x -> x.asStringValue()).orNull());
            r.setFingerprint(o.get("fingerprint").flatMap(x -> x.asStringValue()).orNull());
            r.setLastVisited(o.get("last-visited").flatMap(x -> x.asInstantValue()).orNull());
            return NOptional.of(r);
        }
        return NOptional.ofNamedError("resource");
    }

    public String getValue() {
        return value;
    }

    public NTxManifestResource setValue(String value) {
        this.value = value;
        return this;
    }

    public Instant getLastVisited() {
        return lastVisited;
    }

    public NTxManifestResource setLastVisited(Instant lastVisited) {
        this.lastVisited = lastVisited;
        return this;
    }

    public NTxManifestResourceType getType() {
        return type;
    }

    public NTxManifestResource setType(NTxManifestResourceType type) {
        this.type = type;
        return this;
    }

    public String getFingerprint() {
        return fingerPrint;
    }

    public NTxManifestResource setFingerprint(String fingerPrint) {
        this.fingerPrint = fingerPrint;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NTxManifestResource that = (NTxManifestResource) o;
        return type == that.type && Objects.equals(value, that.value) && Objects.equals(lastVisited, that.lastVisited) && Objects.equals(fingerPrint, that.fingerPrint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, lastVisited, type, fingerPrint);
    }

    @Override
    protected NTxManifestResource clone() {
        try {
            return (NTxManifestResource) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NTxManifestResource copy() {
        return clone();
    }

    @Override
    public NElement toElement() {
        return NElement.ofObject(
                type == null ? null : type.name().toLowerCase(),
                NElement.ofPair("value", value),
                NElement.ofPair("lastVisited", lastVisited),
                NElement.ofPair("fingerprint", fingerPrint)
        );
    }
}
