package net.thevpc.ntexup.engine.auth;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.util.NStringUtils;

import java.io.IOException;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class AuthorshipSignUtils {

    public static final String DEFAULT_DIGEST = "SHA256";
    public static final String DEFAULT_SIG = "SHA256withECDSA";

    // --- Key generation (do once, store private key securely) ---
    public static KeyPair generateKeyPair() {
        KeyPairGenerator gen = null;
        try {
            gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new NIllegalArgumentException(NMsg.ofC("error generating key pair : %s", e));
        }
    }


    // --- Sign ---
    public static NElement sign(byte[] dataToSign, PrivateKey privateKey, String digestAlgo, String sigAlgo) {
        try {
            if (privateKey == null) {
                digestAlgo = NStringUtils.firstNonBlankTrimmed(digestAlgo, DEFAULT_DIGEST);
                MessageDigest md;
                md = MessageDigest.getInstance(digestAlgo);
                md.update(dataToSign, 0, dataToSign.length);
                return NElement.ofObject(
                        NElement.ofPair("algo", digestAlgo),
                        NElement.ofPair("value", NElement.ofString(Base64.getEncoder().encodeToString(md.digest())))
                );
            }
            sigAlgo = NStringUtils.firstNonBlankTrimmed(sigAlgo, DEFAULT_SIG);
            Signature sig = Signature.getInstance(sigAlgo);
            sig.initSign(privateKey);
            sig.update(dataToSign);
            return NElement.ofObject(
                    NElement.ofPair("algo", sigAlgo),
                    NElement.ofPair("value", NElement.ofString(Base64.getEncoder().encodeToString(sig.sign())))
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new NIOException(new IOException(e));
        }
    }

    // --- Verify ---
    public static boolean verify(byte[] originalData, String signatureB64, PublicKey publicKey,String sigAlgo)
            throws Exception {
        Signature sig = Signature.getInstance(NStringUtils.firstNonBlankTrimmed(sigAlgo, DEFAULT_SIG));
        sig.initVerify(publicKey);
        sig.update(originalData);
        return sig.verify(Base64.getDecoder().decode(signatureB64));
    }
}