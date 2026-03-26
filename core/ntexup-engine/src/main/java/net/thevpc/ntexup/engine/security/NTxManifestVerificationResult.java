package net.thevpc.ntexup.engine.security;

import java.util.Objects;

public class NTxManifestVerificationResult {
    private final String authorName;
    private final String authorEmail;
    private final String authorUrl;
    private final String authorOrcId;
    private final String publicKeyAlgo;
    private final String publicKey;

    public NTxManifestVerificationResult(String authorName, String authorEmail, String authorUrl, String authorOrcId, String publicKeyAlgo, String publicKey) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.authorUrl = authorUrl;
        this.publicKeyAlgo = publicKeyAlgo;
        this.authorOrcId = authorOrcId;
        this.publicKey = publicKey;
    }

    public String getAuthorOrcId() {
        return authorOrcId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public String getPublicKeyAlgo() {
        return publicKeyAlgo;
    }

    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NTxManifestVerificationResult that = (NTxManifestVerificationResult) o;
        return Objects.equals(authorName, that.authorName) && Objects.equals(authorEmail, that.authorEmail) && Objects.equals(authorUrl, that.authorUrl) && Objects.equals(authorOrcId, that.authorOrcId) && Objects.equals(publicKeyAlgo, that.publicKeyAlgo) && Objects.equals(publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorName, authorEmail, authorUrl, authorOrcId, publicKeyAlgo, publicKey);
    }
}
