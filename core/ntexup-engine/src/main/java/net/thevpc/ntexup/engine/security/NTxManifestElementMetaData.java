package net.thevpc.ntexup.engine.security;

import java.security.KeyPair;

public class NTxManifestElementMetaData {
    public boolean sign;
    public boolean publish;
    public String authorName;
    public String authorOrcId;
    public String authorEmail;
    public KeyPair authorKey;
    public String authorUrl;
    public String signatureAlgo;
    public String digestAlgo;

}
