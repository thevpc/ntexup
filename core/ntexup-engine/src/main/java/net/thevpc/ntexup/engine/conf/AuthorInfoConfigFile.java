package net.thevpc.ntexup.engine.conf;

import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.auth.AuthorshipSignUtils;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.core.NStoreKey;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathPermission;
import net.thevpc.nuts.log.NLogger;
import net.thevpc.nuts.platform.NStoreType;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NBlankable;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AuthorInfoConfigFile {
    private final NPath confDir;
    private final NLogger logger;

    public AuthorInfoConfigFile(NLogger logger) {
        this.logger = logger;
        confDir = NPath.of(
                NStoreKey.ofUser(NStoreType.CONF)
                        .sharedId(NId.of("net.thevpc.ntexup:ntexup"))
        );

    }

    public AuthorInfo saveAuthorInfo(AuthorInfo value) {
        NPath conf = confDir.resolve("author.tson");
        confDir.mkdirs();

        // save author.tson — never write key material here, only metadata + optional path overrides
        NObjectElementBuilder ob = NElement.ofObjectBuilder();
        ob.add("author-name", value.authorName);
        ob.add("author-email", value.authorEmail);
        ob.add("author-url", value.authorUrl);
        ob.add("author-orcid", value.authorUrl);
        ob.add("private-key", value.privateKeyFile);
        ob.add("public-key", value.publicKeyFile);
        logger.log(NMsg.ofC("store author config to %s : %s", conf, value));
        NElementWriter.ofTson().setFormatter(NElementFormatterStyle.PRETTY).write(ob.build(), conf);

        // save key pair if present
        if (!value.effPublicKeyFile.isFile() || !value.effPrivateKeyFile.isFile()) {
            if (value.keyPair != null) {
                // private key — PKCS8
                String privateB64 = Base64.getEncoder().encodeToString(
                        value.keyPair.getPrivate().getEncoded()
                );
                logger.log(NMsg.ofC("store author PrivateKey to %s", value.effPrivateKeyFile));
                value.effPrivateKeyFile.writeString(privateB64);
                // set permissions to owner-read-only if possible
                value.effPrivateKeyFile.setPermissions(NPathPermission.OWNER_READ);

                // public key — X509
                String publicB64 = Base64.getEncoder().encodeToString(
                        value.keyPair.getPublic().getEncoded()
                );

                logger.log(NMsg.ofC("store author PublicKey to %s", value.effPublicKeyFile));
                value.effPublicKeyFile.writeString(publicB64);
            }
        }
        return loadAuthorInfo(); // reload to resolve effective paths consistently
    }

    public AuthorInfo ensureGeneratedKey() {
        AuthorInfo a = loadAuthorInfo();
        if (a.keyPair == null) {
            a.keyPair = AuthorshipSignUtils.generateKeyPair();
            a.effPrivateKeyFile.writeString(Base64.getEncoder().encodeToString(a.keyPair.getPrivate().getEncoded()));
            a.effPublicKeyFile.writeString(Base64.getEncoder().encodeToString(a.keyPair.getPublic().getEncoded()));
        }
        return a;
    }

    public AuthorInfo loadAuthorInfo() {
        AuthorInfo aa = new AuthorInfo();
        NPath conf = confDir.mkdirs().resolve("author.tson");
        if (conf.isRegularFile()) {
            NElement c = NElementReader.ofTson().read(conf);
            if (c.isAnyObject()) {
                for (NPairElement child : c.asObject().get().namedPairs()) {
                    String k = NTxUtils.uid(child.key().asStringValue().orElse(""));
                    switch (k) {
                        case "author-name": {
                            aa.authorName = (child.value().asStringValue().orElse(""));
                            break;
                        }
                        case "author-url": {
                            aa.authorUrl = (child.value().asStringValue().orElse(""));
                            break;
                        }
                        case "author-orcid": {
                            aa.authorOrcid = (child.value().asStringValue().orElse(""));
                            break;
                        }
                        case "author-email": {
                            aa.authorEmail = (child.value().asStringValue().orElse(""));
                            break;
                        }
                        case "private-key": {
                            aa.privateKeyFile = child.value().asStringValue().orNull();
                            break;
                        }
                        case "public-key": {
                            aa.publicKeyFile = child.value().asStringValue().orNull();
                            break;
                        }
                    }
                }
            }
        }

        NPath effPrivateKeyFile = confDir.resolve("author-key.priv");
        if (!NBlankable.isBlank(aa.privateKeyFile)) {
            effPrivateKeyFile = NPath.of(aa.privateKeyFile);
            if (!effPrivateKeyFile.isAbsolute()) {
                effPrivateKeyFile = effPrivateKeyFile.toAbsolute(confDir);
            }
        }
        NPath effPublicKeyFile = confDir.resolve("author-key.pub");
        if (!NBlankable.isBlank(aa.publicKeyFile)) {
            effPublicKeyFile = NPath.of(aa.publicKeyFile);
            if (!effPublicKeyFile.isAbsolute()) {
                effPublicKeyFile = effPublicKeyFile.toAbsolute(confDir);
            }
        }
        aa.effPrivateKeyFile = effPrivateKeyFile;
        aa.effPublicKeyFile = effPublicKeyFile;
        // now how to load key pair?

        KeyPair keyPair = null;
        if (aa.effPrivateKeyFile.isRegularFile() && aa.effPublicKeyFile.isRegularFile()) {
            try {
                byte[] privateBytes = Base64.getDecoder().decode(
                        aa.effPrivateKeyFile.readString().trim()
                );
                byte[] publicBytes = Base64.getDecoder().decode(
                        aa.effPublicKeyFile.readString().trim()
                );
                // algo is embedded in the X509/PKCS8 encoding, no need to store it separately
                KeyFactory kf = KeyFactory.getInstance("EC");
                PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
                PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicBytes));
                keyPair = new KeyPair(publicKey, privateKey);
            } catch (Exception e) {
                // log but don't crash — author just won't be able to sign
                logger.log(NMsg.ofC("failed to load author key pair: %s", e.getMessage()).asWarning());
            }
        } else if (aa.effPrivateKeyFile.isRegularFile() != aa.effPublicKeyFile.isRegularFile()) {
            // one exists but not the other — warn, likely corrupted setup
            logger.log(NMsg.ofC("incomplete key pair — both private-key.key and public-key.key must exist").asWarning());
        }
        aa.keyPair = keyPair;
        return aa;
    }

}
