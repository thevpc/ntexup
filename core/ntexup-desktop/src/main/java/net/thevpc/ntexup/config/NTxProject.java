package net.thevpc.ntexup.config;

import java.util.Date;

public class NTxProject {
    private String path;
    private Date lastAccess;
    private Date lastSaved;

    public String getPath() {
        return path;
    }

    public NTxProject setPath(String path) {
        this.path = path;
        return this;
    }

    public Date getLastAccess() {
        return lastAccess;
    }

    public NTxProject setLastAccess(Date lastAccess) {
        this.lastAccess = lastAccess;
        return this;
    }

    public Date getLastSaved() {
        return lastSaved;
    }

    public NTxProject setLastSaved(Date lastSaved) {
        this.lastSaved = lastSaved;
        return this;
    }

    @Override
    public String toString() {
        return String.valueOf(path);
    }
}
