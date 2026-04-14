package net.thevpc.ntexup.api.document.security;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NCopiable;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NTxManifest implements NToElement, NCopiable, Cloneable {
    private String version;
    private NElement payload;
    private String digestAlgo;
    private String signatureAlgo;
    private String contentFingerprint;
    private Instant timestamp;
    private NTxManifestDependency[] dependencies;
    private NTxManifestResource[] resources;
    private String authorName;
    private String authorEmail;
    private String authorOrcId;
    private String authorUrl;
    private String authorPublicKey;
    private String signature;
    private NTxManifestPublishInfo publishInfo;

    public static NOptional<NTxManifest> parse(NPath path) {
        if (path == null) {
            return NOptional.ofNamedEmpty(NMsg.ofC("unable to parse %s : null path", path).asError());
        }
        if (path.exists()) {
            NElement p = null;
            try {
                p = NElementReader.ofTson().read(path);
            } catch (Exception ex) {
                return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : %s", path, ex).asError(ex));
            }
            if (p == null) {
                return NOptional.ofNamedEmpty(NMsg.ofC("unable to parse %s : empty content", path).asError());
            }
            return parse(p);
        }
        return NOptional.ofNamedEmpty(NMsg.ofC("unable to parse %s : file not found", path).asError());
    }

    public static NOptional<NTxManifest> parse(NElement element) {
        return parse(element, null);
    }

    private static NOptional<NTxManifest> parse(NElement element, Object source) {
        if (source == null) {
            source = NElement.ofString("manifest");
        }
        try {
            if (element == null) {
                return NOptional.ofNamedEmpty(NMsg.ofC("unable to parse %s : null element", source).asError());
            }
            List<NElementAnnotation> found = element.findAnnotations("ntexup-manifest");
            if (found.isEmpty()) {
                return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : invalid format, missing @ntexup-manifest", source));
            }
            if (found.size() > 1) {
                return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : invalid format, too many @ntexup-manifest instances", source));
            }
            NElementAnnotation a = found.get(0);

            NTxManifest f = new NTxManifest();
            f.setPayload(element);
            f.setVersion(a.get("version").flatMap(x -> x.asStringValue()).orNull());
            f.setDigestAlgo(a.get("digest-algo").flatMap(x -> x.asStringValue()).orNull());
            f.setSignatureAlgo(a.get("signature-algo").flatMap(x -> x.asStringValue()).orNull());
            NOptional<NElement> dependencies1 = a.get("dependencies");
            if (dependencies1.isPresent()) {
                NElement arr = dependencies1.get();
                List<NTxManifestDependency> deps = new ArrayList<>();
                if (arr.isListContainer()) {
                    for (NElement child : arr.asListContainer().get().children()) {
                        NOptional<NTxManifestDependency> d = NTxManifestDependency.parse(child);
                        if (!d.isPresent()) {
                            return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : dependencies format", source));
                        }
                        deps.add(d.get());
                    }
                    f.setDependencies(deps.toArray(new NTxManifestDependency[0]));
                } else {
                    return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : dependencies format", source));
                }
            } else {
                f.setDependencies(new NTxManifestDependency[0]);
            }

            NOptional<NElement> resources1 = a.get("resources");
            if (resources1.isPresent()) {
                NElement arr = resources1.get();
                List<NTxManifestResource> rsrs = new ArrayList<>();
                if (arr.isListContainer()) {
                    for (NElement child : arr.asListContainer().get().children()) {
                        NOptional<NTxManifestResource> d = NTxManifestResource.parse(child);
                        if (!d.isPresent()) {
                            return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : resources format", source));
                        }
                        rsrs.add(d.get());
                    }
                    f.setResources(rsrs.toArray(new NTxManifestResource[0]));
                } else {
                    return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : resources format", source));
                }
            } else {
                f.setResources(new NTxManifestResource[0]);
            }
            f.setSignatureAlgo(a.get("content-fingerprint").flatMap(x -> x.asStringValue()).orNull());
            f.setTimestamp(a.get("timestamp").flatMap(x -> x.asInstantValue()).orNull());
            f.setAuthorName(a.get("author-name").flatMap(x -> x.asStringValue()).orNull());
            f.setAuthorEmail(a.get("author-email").flatMap(x -> x.asStringValue()).orNull());
            f.setAuthorUrl(a.get("author-url").flatMap(x -> x.asStringValue()).orNull());
            f.setAuthorOrcId(a.get("author-orc-id").flatMap(x -> x.asStringValue()).orNull());
            f.setAuthorPublicKey(a.get("author-public-key").flatMap(x -> x.asStringValue()).orNull());
            f.setAuthorPublicKey(a.get("author-public-key").flatMap(x -> x.asStringValue()).orNull());
            f.setSignature(a.get("manifest-signature").flatMap(x -> x.asStringValue()).orNull());
            NElement publishElement = a.get("publish").orNull();
            if (publishElement != null && !publishElement.isNull()) {
                NOptional<NTxManifestPublishInfo> pi = NTxManifestPublishInfo.parse(publishElement);
                if (pi.isPresent()) {
                    f.setPublishInfo(pi.get());
                } else {
                    return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : invalid publish info", source));
                }
            }
            return NOptional.of(f);
        } catch (Exception ex) {
            return NOptional.ofNamedError(NMsg.ofC("unable to parse %s : %s", source, ex).asError(ex));
        }
    }

    public String getAuthorOrcId() {
        return authorOrcId;
    }

    public NTxManifest setAuthorOrcId(String authorOrcId) {
        this.authorOrcId = authorOrcId;
        return this;
    }

    public NTxManifestPublishInfo getPublishInfo() {
        return publishInfo;
    }

    public NTxManifest setPublishInfo(NTxManifestPublishInfo publishInfo) {
        this.publishInfo = publishInfo;
        return this;
    }

    public String getSignature() {
        return signature;
    }

    public NTxManifest setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public NTxManifest setVersion(String nextVersion) {
        this.version = nextVersion;
        return this;
    }

    public NElement getPayload() {
        return payload;
    }

    public NTxManifest setPayload(NElement payload) {
        this.payload = payload;
        return this;
    }

    public String getDigestAlgo() {
        return digestAlgo;
    }

    public NTxManifest setDigestAlgo(String digestAlgo) {
        this.digestAlgo = digestAlgo;
        return this;
    }

    public String getSignatureAlgo() {
        return signatureAlgo;
    }

    public NTxManifest setSignatureAlgo(String signatureAlgo) {
        this.signatureAlgo = signatureAlgo;
        return this;
    }

    public String getContentFingerprint() {
        return contentFingerprint;
    }

    public NTxManifest setContentFingerprint(String contentFingerprint) {
        this.contentFingerprint = contentFingerprint;
        return this;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public NTxManifest setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public NTxManifestDependency[] getDependencies() {
        return dependencies;
    }

    public NTxManifest setDependencies(NTxManifestDependency[] dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public NTxManifestResource[] getResources() {
        return resources;
    }

    public NTxManifest setResources(NTxManifestResource[] resources) {
        this.resources = resources;
        return this;
    }

    public String getAuthorName() {
        return authorName;
    }

    public NTxManifest setAuthorName(String authorName) {
        this.authorName = authorName;
        return this;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public NTxManifest setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
        return this;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public NTxManifest setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
        return this;
    }

    public String getAuthorPublicKey() {
        return authorPublicKey;
    }

    public NTxManifest setAuthorPublicKey(String authorPublicKey) {
        this.authorPublicKey = authorPublicKey;
        return this;
    }

    @Override
    public NTxManifest copy() {
        return null;
    }

    @Override
    protected NTxManifest clone() {
        try {
            NTxManifest u = (NTxManifest) super.clone();
            if (u.dependencies != null) {
                u.dependencies = Arrays.stream(u.dependencies).map(NTxManifestDependency::copy).toArray(NTxManifestDependency[]::new);
            }
            if (u.resources != null) {
                u.resources = Arrays.stream(u.resources).map(NTxManifestResource::copy).toArray(NTxManifestResource[]::new);
            }
            if (u.publishInfo != null) {
                u.publishInfo = u.publishInfo.copy();
            }
            return u;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public NElement toElement() {
        List<NElement> elements = new ArrayList<>();
        elements.add(NElement.ofPair("version", NStringUtils.firstNonBlank(version, NTxEngine.CURRENT_VERSION)));
        elements.add(NElement.ofPair("digest-algo", digestAlgo));
        elements.add(NElement.ofPair("signature-algo", signatureAlgo));
        elements.add(NElement.ofPair("dependencies", NElement.ofArray(Arrays.stream(dependencies == null ? new NTxManifestDependency[0] : dependencies).map(NToElement::toElement).toArray(NElement[]::new))));
        elements.add(NElement.ofPair("resources", NElement.ofArray(Arrays.stream(resources == null ? new NTxManifestResource[0] : resources).map(NToElement::toElement).toArray(NElement[]::new))));
        elements.add(NElement.ofPair("content-fingerprint", contentFingerprint));
        elements.add(NElement.ofPair("timestamp", timestamp != null ? timestamp : Instant.now()));
        elements.add(((NBlankable.isBlank(authorName)) ? null : NElement.ofPair("author-name", NElement.ofString(NStringUtils.trimToNull(authorName)))));
        elements.add(((NBlankable.isBlank(authorEmail)) ? null : NElement.ofPair("author-email", NElement.ofString(NStringUtils.trimToNull(authorEmail)))));
        elements.add(((NBlankable.isBlank(authorUrl)) ? null : NElement.ofPair("author-url", NElement.ofString(NStringUtils.trimToNull(authorUrl)))));
        elements.add(((NBlankable.isBlank(authorOrcId)) ? null : NElement.ofPair("author-orc-id", NElement.ofString(NStringUtils.trimToNull(authorOrcId)))));
        elements.add(((authorPublicKey == null) ? null : NElement.ofPair("author-public-key", authorPublicKey)));
        if (!NBlankable.isBlank(signature)) {
            elements.add(NElement.ofPair("manifest-signature", signature));
        }

        if (publishInfo != null) {
            elements.add(NElement.ofPair("publish", publishInfo.toElement()));
        }

        NElementAnnotation metadata = NElementAnnotation.of(
                "ntexup-manifest",
                elements.toArray(new NElement[0])
        );
        NElementBuilder e = payload.builder().addAffixAt(0, metadata, NAffixAnchor.START);
        return e.build();
    }
}
