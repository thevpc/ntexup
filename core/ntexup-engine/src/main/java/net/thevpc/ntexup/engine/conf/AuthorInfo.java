package net.thevpc.ntexup.engine.conf;

import net.thevpc.nuts.io.NPath;

import java.security.KeyPair;

public class AuthorInfo {
    public String privateKeyFile = null;
    public String publicKeyFile = null;
    public String authorName;
    public String authorUrl;
    public String authorEmail;
    public String authorOrcid;
    public KeyPair keyPair;
    public NPath effPrivateKeyFile;
    public NPath effPublicKeyFile;
}
