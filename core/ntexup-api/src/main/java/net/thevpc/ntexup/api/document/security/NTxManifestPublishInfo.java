package net.thevpc.ntexup.api.document.security;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;
import net.thevpc.nuts.elem.NToElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NCopiable;
import net.thevpc.nuts.util.NNameFormat;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.io.Serializable;
import java.util.Objects;

public class NTxManifestPublishInfo implements Cloneable, Serializable, NToElement, NCopiable {
    private String type;
    private String authority;
    private String token;

    public static NOptional<NTxManifestPublishInfo> parse(NElement element) {
        if (element == null) {
            return NOptional.ofNamedEmpty("resource");
        }
        if (element.isNull()) {
            return NOptional.ofNamedEmpty("resource");
        }
        if (element.isAnyObject()) {
            NObjectElement o = element.asObject().get();
            String n = NStringUtils.stripToNull(o.name().get());
            if(n!=null){
                n= NNameFormat.LOWER_KEBAB_CASE.format(n);
            }
            NTxManifestPublishInfo r = new NTxManifestPublishInfo();
            try {
                r.setType(n);
            } catch (Exception ex) {
                return NOptional.ofError(NMsg.ofC("invalid publish-info type %s", n));
            }
            r.setToken(o.get("value").flatMap(x -> x.asStringValue()).orNull());
            r.setAuthority(o.get("source").flatMap(x -> x.asStringValue()).orNull());
            return NOptional.of(r);
        }
        return NOptional.ofNamedError("publish-info");
    }


    public String getType() {
        return type;
    }

    public NTxManifestPublishInfo setType(String type) {
        this.type = type;
        return this;
    }

    public String getAuthority() {
        return authority;
    }

    public NTxManifestPublishInfo setAuthority(String authority) {
        this.authority = authority;
        return this;
    }

    public String getToken() {
        return token;
    }

    public NTxManifestPublishInfo setToken(String token) {
        this.token = token;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NTxManifestPublishInfo that = (NTxManifestPublishInfo) o;
        return Objects.equals(type, that.type) && Objects.equals(authority, that.authority) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, authority, token);
    }

    @Override
    public String toString() {
        return "NTxManifestPublishInfo{" +
                "type='" + type + '\'' +
                ", source='" + authority + '\'' +
                ", value='" + token + '\'' +
                '}';
    }

    @Override
    protected NTxManifestPublishInfo clone() {
        try {
            return (NTxManifestPublishInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NTxManifestPublishInfo copy() {
        return clone();
    }

    @Override
    public NElement toElement() {
        return NElement.ofObject(type,
                NElement.ofPair("authority", authority),
                NElement.ofPair("token", token)
        );
    }
}
