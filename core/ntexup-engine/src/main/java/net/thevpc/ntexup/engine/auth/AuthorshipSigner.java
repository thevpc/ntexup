package net.thevpc.ntexup.engine.auth;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.util.NStringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class AuthorshipSigner {

    public static final String DEFAULT_DIGEST = "SHA256";
    public static final String DEFAULT_SIG = "SHA256withECDSA";


    private final PrivateKey privateKey;
    String digestAlgo;
    String sigAlgo;
    HashUpdater updater;

    public AuthorshipSigner(PrivateKey privateKey) {
        this(privateKey, null, null);
    }

    public AuthorshipSigner(PrivateKey privateKey, String digestAlgo0, String sigAlgo0) {
        this.privateKey = privateKey;
        this.digestAlgo = NStringUtils.firstNonBlankStripped(digestAlgo0, DEFAULT_DIGEST);
        this.sigAlgo = NStringUtils.firstNonBlankStripped(sigAlgo0, DEFAULT_SIG);
        if (privateKey == null) {
            updater = new HashUpdater() {
                final MessageDigest md;

                {
                    try {
                        md = MessageDigest.getInstance(digestAlgo);
                    } catch (NoSuchAlgorithmException e) {
                        throw new NIOException(new IOException(e));
                    }
                }

                @Override
                public void update(byte[] dataToSign) {
                    md.update(dataToSign, 0, dataToSign.length);
                }

                @Override
                public NElement build() {
                    return NElement.ofObject(
                            NElement.ofPair("algo", digestAlgo),
                            NElement.ofPair("value", NElement.ofString(Base64.getEncoder().encodeToString(md.digest())))
                    );
                }

                @Override
                public String buildString() {
                    return Base64.getEncoder().encodeToString(md.digest());
                }
            };
        } else {
            updater = new HashUpdater() {
                final Signature sig;

                {
                    try {
                        sig = Signature.getInstance(sigAlgo);
                        sig.initSign(privateKey);
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        throw new NIOException(new IOException(e));
                    }
                }

                @Override
                public void update(byte[] dataToSign) {
                    try {
                        sig.update(dataToSign);
                    } catch (SignatureException e) {
                        throw new NIOException(new IOException(e));
                    }
                }

                @Override
                public NElement build() {
                    try {
                        return NElement.ofObject(
                                NElement.ofPair("algo", sigAlgo),
                                NElement.ofPair("value", NElement.ofString(Base64.getEncoder().encodeToString(sig.sign())))
                        );
                    } catch (SignatureException e) {
                        throw new NIOException(new IOException(e));
                    }
                }

                @Override
                public String buildString() {
                    try {
                        return Base64.getEncoder().encodeToString(sig.sign());
                    } catch (SignatureException e) {
                        throw new NIOException(new IOException(e));
                    }
                }
            };
        }
    }

    interface HashUpdater {
        void update(byte[] dataToSign);

        NElement build();

        String buildString();
    }

    public AuthorshipSigner update(byte[] dataToSign) {
        updater.update(dataToSign);
        return this;
    }

    public AuthorshipSigner updateCompact(NElement element) {
        updater.update(element.toCompactString().getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public NElement sign() {
        return updater.build();
    }

    public String signString() {
        return updater.buildString();
    }

}