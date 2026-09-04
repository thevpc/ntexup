package net.thevpc.ntexup.engine.security;

import net.thevpc.nuts.net.NHttpClient;
import net.thevpc.nuts.net.NHttpResponse;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NException;
import net.thevpc.nuts.util.NOptional;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.*;
import org.bouncycastle.util.Store;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.*;
import java.time.Instant;
import java.util.*;

public class Rfc3161Helper {
    private static final List<String> DEFAULT_TSA_URLS = Arrays.asList(
            "https://timestamp.digicert.com",
            "https://tsa.esign.gov",
            "https://timestamp.sectigo.com",
            "https://freetsa.org/tsr"
    );

    private static ASN1ObjectIdentifier toTspAlgo(String digestAlgo) {
        switch (digestAlgo.toUpperCase().replace("-", "")) {
            case "SHA256": return TSPAlgorithms.SHA256;
            case "SHA384": return TSPAlgorithms.SHA384;
            case "SHA512": return TSPAlgorithms.SHA512;
            default: throw new IllegalArgumentException("unsupported digest algo for TSA: " + digestAlgo);
        }
    }

    // --- RFC 3161 token request with fallback ---

    public static RfcTokenResult requestRfc3161Token(byte[] hash, String digestAlgo, String preferredTsaUrl) {
        List<String> urls = new ArrayList<>();
        if (!NBlankable.isBlank(preferredTsaUrl)) {
            urls.add(preferredTsaUrl);
        }
        DEFAULT_TSA_URLS.stream()
                .filter(u -> !u.equals(preferredTsaUrl))
                .forEach(urls::add);

        Exception lastException = null;
        for (String tsaUrl : urls) {
            try {
                byte[] token = doRequestRfc3161Token(hash, digestAlgo, tsaUrl);
                return new RfcTokenResult(token, tsaUrl);
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw NException.ofUncheckedException(new IOException("all TSA servers failed", lastException));
    }

    private static byte[] doRequestRfc3161Token(byte[] hash, String digestAlgo, String tsaUrl) throws Exception {
        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        reqGen.setCertReq(true);
        TimeStampRequest request = reqGen.generate(
                toTspAlgo(digestAlgo),
                hash,
                BigInteger.valueOf(System.currentTimeMillis())
        );
        byte[] requestBytes = request.getEncoded();

        NHttpResponse response = NHttpClient.of()
                .POST(tsaUrl)
                .addHeader("Content-Type", "application/timestamp-query")
                .addHeader("Accept", "application/timestamp-reply")
                .requestBody(requestBytes)
//                .setConnectTimeout(NDuration.ofSeconds(10))  // short so fallback is fast
                .run();
        if (!response.isOk()) {
            throw new IOException("HTTP " + response.statusCode());
        }

        TimeStampResponse tsResponse = new TimeStampResponse(response.contentAsBytes());
        tsResponse.validate(request);
        if (tsResponse.getStatus() != 0) {
            throw new IOException("TSA rejected: " + tsResponse.getStatusString());
        }
        return tsResponse.getTimeStampToken().getEncoded();
    }

    public static class RfcTokenResult {
        public final byte[] token;
        public final String tsaUrl; // which TSA actually succeeded
        public RfcTokenResult(byte[] token, String tsaUrl) {
            this.token = token;
            this.tsaUrl = tsaUrl;
        }
    }

    // --- RFC 3161 token verification ---

    public static NOptional<Instant> verifyRfc3161Token(byte[] manifestHash, String tsaTokenB64) {
        if (NBlankable.isBlank(tsaTokenB64)) {
            return NOptional.ofError(NMsg.ofC("no tsa-token in manifest — document was not published"));
        }

        try {
            byte[] tokenBytes = Base64.getDecoder().decode(tsaTokenB64);
            TimeStampToken token = new TimeStampToken(new CMSSignedData(tokenBytes));
            TimeStampTokenInfo info = token.getTimeStampInfo();

            // 1. verify hash matches what was timestamped
            if (!Arrays.equals(manifestHash, info.getMessageImprintDigest())) {
                return NOptional.ofError(NMsg.ofC(
                        "tsa-token hash mismatch — manifest may have been altered after timestamping"));
            }

            // 2. verify token signature against embedded TSA certificate
            Store<X509CertificateHolder> certs = token.getCertificates();
            SignerInformationStore signers = token.toCMSSignedData().getSignerInfos();
            for (SignerInformation signer : signers.getSigners()) {
                Collection<X509CertificateHolder> matches = certs.getMatches(signer.getSID());
                for (X509CertificateHolder certHolder : matches) {

                    // 2a. verify TSA cert was valid AT the time of timestamping
                    if (!certHolder.isValidOn(info.getGenTime())) {
                        return NOptional.ofError(NMsg.ofC(
                                "TSA certificate was not valid at timestamp time"));
                    }

                    // 2b. verify token signature
                    if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certHolder))) {
                        return NOptional.ofError(NMsg.ofC("tsa-token signature invalid"));
                    }

                    // 2c. verify TSA cert chains to a trusted root in the system trust store
                    X509Certificate javaCert = new JcaX509CertificateConverter().getCertificate(certHolder);
                    try {
                        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        CertPath certPath = cf.generateCertPath(Collections.singletonList(javaCert));
                        PKIXParameters params = new PKIXParameters(loadSystemTrustStore());
                        params.setRevocationEnabled(false); // CRL checks require network, skip for now
                        params.setDate(info.getGenTime()); // validate at timestamp time, not now
                        validator.validate(certPath, params);
                    } catch (CertPathValidatorException e) {
                        // warn but don't hard-fail — TSA certs may not be in all system trust stores
                        // a reviewer can manually verify the TSA cert if disputed
                        // TODO: bundle known TSA root certs as a fallback
                        return NOptional.ofError(NMsg.ofC(
                                "TSA certificate chain not trusted by system trust store: %s", e.getMessage()));
                    }
                }
            }

            return NOptional.of(info.getGenTime().toInstant());

        } catch (Exception e) {
            return NOptional.ofError(NMsg.ofC("tsa-token verification failed: %s", e.getMessage()));
        }
    }

    private static KeyStore loadSystemTrustStore() throws Exception {
        // loads the JVM's default trust store (cacerts)
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                X509Certificate[] accepted = ((X509TrustManager) tm).getAcceptedIssuers();
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);
                for (X509Certificate cert : accepted) {
                    ks.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
                }
                return ks;
            }
        }
        throw new IllegalStateException("no X509TrustManager found in system trust store");
    }
}
