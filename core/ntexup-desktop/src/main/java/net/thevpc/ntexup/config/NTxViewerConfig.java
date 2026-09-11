package net.thevpc.ntexup.config;

public class NTxViewerConfig {
    private NTxProject[] recentProjects;

    public NTxProject[] getRecentProjects() {
        return recentProjects;
    }

    public NTxViewerConfig setRecentProjects(NTxProject[] recentProjects) {
        this.recentProjects = recentProjects;
        return this;
    }
}
