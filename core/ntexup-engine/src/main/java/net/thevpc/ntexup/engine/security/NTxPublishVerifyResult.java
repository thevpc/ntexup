package net.thevpc.ntexup.engine.security;

import java.time.Instant;

public class NTxPublishVerifyResult {
    private final NTxManifestVerificationResult manifest;
    private final Instant timestamp; // from TSA — trusted time

    public NTxPublishVerifyResult(NTxManifestVerificationResult manifest, Instant timestamp) {
        this.manifest = manifest;
        this.timestamp = timestamp;
    }
    public NTxManifestVerificationResult getManifest() { return manifest; }
    public Instant getTimestamp() { return timestamp; }
}
