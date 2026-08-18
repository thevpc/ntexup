package net.thevpc.ntexup.engine.security;

import net.thevpc.ntexup.api.document.security.NTxManifest;
import net.thevpc.ntexup.api.document.security.NTxManifestOptions;
import net.thevpc.ntexup.api.document.security.NTxManifestPublishInfo;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.auth.AuthorshipSigner;
import net.thevpc.ntexup.engine.document.DefaultNTxDocument;
import net.thevpc.ntexup.engine.impl.NTxCompiledDocumentImpl;
import net.thevpc.ntexup.api.document.security.NTxManifestResource;
import net.thevpc.ntexup.api.document.security.NTxManifestDependency;
import net.thevpc.nuts.artifact.NDefinition;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NDigest;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;

public class NTxManifestElementMetaDataBuilder {
    NTxCompiledDocumentImpl compiledDocument;
    NTxEngine engine;

    public NTxManifestElementMetaDataBuilder(NTxCompiledDocumentImpl compiledDocument, NTxEngine engine) {
        this.compiledDocument = compiledDocument;
        this.engine = engine;
    }

    public NTxManifest computeManifestElement(NTxManifestOptions options) {
        NTxManifestElementMetaData memd = new NTxManifestElementMetaData();
        memd.authorName = engine.getAuthorName();
        memd.authorEmail = engine.getAuthorEmail();
        memd.authorKey = engine.getAuthorKey();
        memd.authorUrl = engine.getAuthorUrl();
        memd.authorOrcId = engine.getAuthorOrcid();
        memd.digestAlgo = AuthorshipSigner.DEFAULT_DIGEST;
        memd.signatureAlgo = AuthorshipSigner.DEFAULT_SIG;
        memd.sign = options.isSign() || options.isPublish();
        memd.publish = options.isPublish();
        return computeManifestElement(memd);
    }

    public NOptional<NTxManifestVerificationResult> verifyManifest(NTxManifest manifestLoaded) {
        if (manifestLoaded == null) return NOptional.ofError(NMsg.ofC("No manifest found"));

        // 1. Header Validation
        String digestAlgo = manifestLoaded.getDigestAlgo();
        String signatureAlgo = manifestLoaded.getSignatureAlgo();
        String sigValue = manifestLoaded.getSignature();
        String keyValue = manifestLoaded.getAuthorPublicKey();

        if (NBlankable.isBlank(digestAlgo) || NBlankable.isBlank(signatureAlgo) ||
                NBlankable.isBlank(sigValue) || NBlankable.isBlank(keyValue)) {
            return NOptional.ofError(NMsg.ofC("Missing critical cryptographic headers"));
        }

        try {
            // 2. Resource & Dependency Integrity (The "Why" check)
            // Check Resources
            NTxManifestResource[] localRes = resources(digestAlgo);
            NTxManifestResource[] loadedRes = manifestLoaded.getResources();
            if (localRes.length != loadedRes.length) {
                return NOptional.ofError(NMsg.ofC("Resource count mismatch: local %d vs manifest %d", localRes.length, loadedRes.length));
            }
            for (int i = 0; i < localRes.length; i++) {
                if (!Objects.equals(localRes[i].getFingerprint(), loadedRes[i].getFingerprint())) {
                    return NOptional.ofError(NMsg.ofC("Resource fingerprint mismatch at index %d", i));
                }
            }

            // Check Dependencies
            NTxManifestDependency[] localDeps = dependencies(digestAlgo);
            NTxManifestDependency[] loadedDeps = manifestLoaded.getDependencies();
            if (localDeps.length != loadedDeps.length) {
                return NOptional.ofError(NMsg.ofC("Dependency count mismatch"));
            }
            for (int i = 0; i < localDeps.length; i++) {
                if (!Objects.equals(localDeps[i].getValue(), loadedDeps[i].getValue())) {
                    return NOptional.ofError(NMsg.ofC("Dependency version/value mismatch: %s", localDeps[i].getValue()));
                }
            }

            // 3. TSA Verification (Proof of Time)
            NTxManifestPublishInfo pi = manifestLoaded.getPublishInfo();
            if (pi != null && "tsa".equals(pi.getType())) {
                NTxManifest toVerifyTSA = manifestLoaded.copy();
                toVerifyTSA.setPayload(NElement.ofObject());
                toVerifyTSA.setPublishInfo(null);
                byte[] manifestHash = MessageDigest.getInstance(digestAlgo)
                        .digest(toVerifyTSA.toElement().toCompactString().getBytes(StandardCharsets.UTF_8));
                NOptional<Instant> d = Rfc3161Helper.verifyRfc3161Token(manifestHash, pi.getToken());
                if (!d.isPresent()) {
                    return NOptional.ofError(NMsg.ofC("TSA timestamp verification failed"));
                }
            }

            // 4. Public Key & Signature (Proof of Author)
            byte[] keyBytes = Base64.getDecoder().decode(keyValue);
            KeyFactory kf = KeyFactory.getInstance(signatureAlgo.contains("RSA") ? "RSA" : "EC");
            PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(keyBytes));

            NTxManifest toVerifySig = manifestLoaded.copy();
            toVerifySig.setSignature(null);
            toVerifySig.setPublishInfo(null);

            byte[] headerBytes = toVerifySig.toElement().toCompactString().getBytes(StandardCharsets.UTF_8);

            Signature sig = Signature.getInstance(signatureAlgo);
            sig.initVerify(publicKey);
            sig.update(headerBytes);

            if (!sig.verify(Base64.getDecoder().decode(sigValue))) {
                return NOptional.ofError(NMsg.ofC("Signature invalid: Metadata has been altered"));
            }

            // 5. Final Content Fingerprint Check (Proof of Body Integrity)
            NTxManifest localCheck = manifestLoaded.copy();

            // This ensures the 500 pages match the fingerprint
            NTxManifest recomputed = computeManifestElement_step_fingerprint(localCheck, digestAlgo);

            if (!Objects.equals(recomputed.getContentFingerprint(), manifestLoaded.getContentFingerprint())) {
                return NOptional.ofError(NMsg.ofC("Content-fingerprint mismatch: Page data has changed"));
            }

            return NOptional.of(new NTxManifestVerificationResult(
                    manifestLoaded.getAuthorName(), manifestLoaded.getAuthorEmail(),
                    manifestLoaded.getAuthorUrl(), manifestLoaded.getAuthorOrcId(),
                    signatureAlgo, keyValue
            ));

        } catch (Exception e) {
            return NOptional.ofError(NMsg.ofC("Verification system error: %s", e.getMessage()));
        }
    }

    private NTxManifest computeManifestElement_step_fingerprint(NTxManifest info, String digestAlgo) {
        NTxCompiledDocumentImpl.FingerprintBuilder bb = compiledDocument.getFingerPrintBuilder();
        AuthorshipSigner contentFingerPrintSigner = new AuthorshipSigner(null, digestAlgo, null);
        contentFingerPrintSigner.updateCompact(NElement.ofObject(
                NElement.ofPair("dependencies", NElement.ofArray(Arrays.stream(info.getDependencies()).map(NToElement::toElement).toArray(NElement[]::new))),
                NElement.ofPair("resources", NElement.ofArray(Arrays.stream(info.getResources()).map(NToElement::toElement).toArray(NElement[]::new)))
        ));
        for (DefaultNTxDocument.NamedPart v : bb.getContentFiles().values()) {
            contentFingerPrintSigner.update(v.name.getBytes(StandardCharsets.UTF_8));
            contentFingerPrintSigner.update((byte[]) v.value);
        }
        NTxManifest hashedInfo = info.copy();
        hashedInfo.setContentFingerprint(contentFingerPrintSigner.signString());
        return hashedInfo;
    }

    private NTxManifest computeManifestElement_step_sign(NTxManifest hashedInfo, NTxManifestElementMetaData memd) {
        NTxManifest signedInfo = hashedInfo.copy();
        NAssert.requireNamedNonBlank(hashedInfo.getContentFingerprint(), "fingerprint");
        KeyPair authorKey = memd.authorKey;
        PrivateKey privateKey = null;
        NAssert.requireNonNull(authorKey, () -> NMsg.ofC("unable to sign document : missing author key"));
        NAssert.requireNonBlank(hashedInfo.getAuthorName(), () -> NMsg.ofC("unable to sign document : missing author name"));
        NAssert.requireNonBlank(hashedInfo.getAuthorEmail(), () -> NMsg.ofC("unable to sign document : missing author email"));
        signedInfo.setAuthorPublicKey(Base64.getEncoder().encodeToString(authorKey.getPublic().getEncoded()));
        privateKey = authorKey.getPrivate();
        signedInfo.setSignature(
                new AuthorshipSigner(privateKey, memd.digestAlgo, memd.signatureAlgo)
                        .updateCompact(
                                hashedInfo.toElement()
                        ).signString()
        );
        return signedInfo;
    }

    private NTxManifest computeManifestElement_step_publish(NTxManifest signedInfo, NTxManifestElementMetaData memd) {
        NTxManifest publishedInfo = signedInfo.copy();
        try {
            NTxManifest toPublish = signedInfo.copy();
            toPublish.setPayload(NElement.ofObject());// just ignore the payload, and hash only the headers
            byte[] manifestHash = MessageDigest.getInstance(memd.digestAlgo)
                    .digest(toPublish.toElement().toCompactString().getBytes(StandardCharsets.UTF_8));
            String tsaUrl = NLiteral.of(engine.getEnv("tsa-url").orNull()).asString().orElse("https://freetsa.org/tsr");
            Rfc3161Helper.RfcTokenResult tsaToken = Rfc3161Helper.requestRfc3161Token(manifestHash, memd.digestAlgo, tsaUrl);
            publishedInfo.setPublishInfo(
                    new NTxManifestPublishInfo()
                            .setType("tsa")
                            .setAuthority(tsaToken.tsaUrl)
                            .setToken(Base64.getEncoder().encodeToString(tsaToken.token))
            );
            return publishedInfo;
        } catch (Exception e) {
            throw new NIllegalArgumentException(NMsg.ofC("TSA timestamping failed : %s",
                    NException.ofUncheckedException(e)).asWarning(), NException.ofUncheckedException(e));
        }
    }

    private NTxManifest computeManifestElement(NTxManifestElementMetaData memd) {
        NTxManifest info = new NTxManifest();
        info.setTimestamp(Instant.now());
        info.setVersion(NTxEngine.CURRENT_VERSION);
        info.setDigestAlgo(memd.digestAlgo);
        info.setSignatureAlgo(memd.signatureAlgo);
        info.setPayload(compiledDocument.toElement(true));
        info.setAuthorName(NStringUtils.stripToNull(memd.authorName));
        info.setAuthorEmail(NStringUtils.stripToNull(memd.authorEmail));
        info.setAuthorOrcId(NStringUtils.stripToNull(memd.authorOrcId));
        info.setAuthorUrl(NStringUtils.stripToNull(memd.authorUrl));
        info.setDependencies(dependencies(memd.digestAlgo));
        info.setResources(resources(memd.digestAlgo));

        NTxManifest hashedInfo = computeManifestElement_step_fingerprint(info, memd.digestAlgo);
        boolean sign = memd.sign || memd.publish;
        if (sign) {
            NTxManifest signedInfo = computeManifestElement_step_sign(hashedInfo, memd);
            if (memd.publish) {
                return computeManifestElement_step_publish(signedInfo, memd);
            }
            return signedInfo;
        } else {
            return hashedInfo;
        }
    }


    private NTxManifestResource[] resources(String digestAlgo) {
        Map<String, NTxManifestResource> ed = compiledDocument.getFingerPrintBuilder().getEffectiveResource();
        List<NTxManifestResource> deps = new ArrayList<>();
        for (String s : new TreeSet<>(ed.keySet())) {
            NTxManifestResource d = ed.get(s);
            deps.add(d);
        }
        return deps.toArray(new NTxManifestResource[0]);

    }

    private NTxManifestDependency[] dependencies(String digestAlgo) {
        Map<String, NDefinition> ed = compiledDocument.getFingerPrintBuilder().getEffectiveDependencies();
        List<NTxManifestDependency> deps = new ArrayList<>();
        for (String s : new TreeSet<>(ed.keySet())) {
            NDefinition d = ed.get(s);
            deps.add(new NTxManifestDependency()
                            .setValue(d.id().longName())
                    .setFingerprint(NDigest.of()
                            .algorithm(digestAlgo)
                            .source(d.content().get()).computeString())
            );
        }
        return deps.toArray(new NTxManifestDependency[0]);
    }


}
