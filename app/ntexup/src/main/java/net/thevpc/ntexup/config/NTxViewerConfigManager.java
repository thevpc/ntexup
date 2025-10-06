package net.thevpc.ntexup.config;

import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementParser;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.elem.NElements;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.platform.NStoreType;
import net.thevpc.nuts.util.NBlankable;

import java.io.*;
import java.util.*;

public class NTxViewerConfigManager {

    private NPath viewerConfigFile;

    public NTxViewerConfigManager() {
        this(null);
    }

    public NTxViewerConfigManager(NPath viewerConfigFile) {
        String configName = "ntexup-config.tson";
        NPath appCacheFolder = NApp.of().getConfFolder();
        if (viewerConfigFile == null) {
            if (appCacheFolder == null) {
                viewerConfigFile = NPath.ofIdStore(NId.of("net.thevpc.ntexup:ntexup"), NStoreType.CACHE).resolve(configName);
            } else {
                viewerConfigFile = appCacheFolder.resolve(configName);
            }
        } else if (viewerConfigFile.isDirectory()) {
            viewerConfigFile = viewerConfigFile.resolve(configName);
        } else if (!viewerConfigFile.exists() && !viewerConfigFile.getName().endsWith(".tson")) {
            viewerConfigFile = viewerConfigFile.resolve(configName);
        }
        this.viewerConfigFile = viewerConfigFile;
    }

    public void markAccessed(NPath path) {
        NTxViewerConfig config = loadViewerConfig();
        List<NTxProject> old = new ArrayList<>();
        NTxProject found = null;
        if (config.getRecentProjects() != null) {
            for (NTxProject a : config.getRecentProjects()) {
                if (a != null && !NBlankable.isBlank(a.getPath())) {
                    if (NPath.of(a.getPath()).equals(path)) {
                        found = a;
                    }
                    old.add(a);
                }
            }
        }
        Date now = new Date();
        if (found != null) {
            found.setLastAccess(now);
        } else {
            NTxProject p = new NTxProject();
            p.setLastAccess(now);
            p.setPath(path.toString());
            old.add(p);
        }
        config.setRecentProjects(old.toArray(new NTxProject[0]));
        saveViewerConfig(config);
    }

    public void markSaved(NPath path) {
        NTxViewerConfig config = loadViewerConfig();
        List<NTxProject> old = new ArrayList<>();
        NTxProject found = null;
        if (config.getRecentProjects() != null) {
            for (NTxProject a : config.getRecentProjects()) {
                if (a != null && !NBlankable.isBlank(a.getPath())) {
                    if (NPath.of(a.getPath()).equals(path)) {
                        found = a;
                    }
                    old.add(a);
                }
            }
        }
        Date now = new Date();
        if (found != null) {
            found.setLastAccess(now);
            found.setLastSaved(now);
        } else {
            NTxProject p = new NTxProject();
            p.setLastAccess(now);
            p.setLastSaved(now);
            p.setPath(path.toString());
            old.add(p);
        }
        config.setRecentProjects(old.toArray(new NTxProject[0]));
        saveViewerConfig(config);
    }

    public void saveViewerConfig(NTxViewerConfig config) {
        config = validate(config);
        NElement elem = NElements.of().toElement(config);
        viewerConfigFile.mkParentDirs();
        try (OutputStream os = viewerConfigFile.getOutputStream()) {
            NElementWriter.ofTson().write(elem, os);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public NPath[] getLastAccessedPaths() {
        NTxViewerConfig config = loadViewerConfig();
        return Arrays.stream(config.getRecentProjects()).map(x -> NPath.of(x.getPath())).toArray(NPath[]::new);
    }

    public NTxProject[] getRecentProjects() {
        NTxViewerConfig config = loadViewerConfig();
        return config.getRecentProjects();
    }

    public NPath getLastAccessedPath() {
        NTxViewerConfig config = loadViewerConfig();
        NTxProject[] recentProjects = config.getRecentProjects();
        return recentProjects.length == 0 ? null : NPath.of(recentProjects[0].getPath());
    }

    public NTxViewerConfig loadViewerConfig() {
        NTxViewerConfig config = null;
        if (viewerConfigFile.isRegularFile()) {
            try (InputStream is = viewerConfigFile.getInputStream()) {
                NElement d = NElementParser.ofTson().parse(is);
                config = NElements.of().fromElement(d, NTxViewerConfig.class);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        config = validate(config);
        return config;
    }

    private int compare(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null && b != null) {
            return -1;
        }
        if (a != null && b == null) {
            return 1;
        }
        if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        }
        throw new IllegalArgumentException("invalid comparision");
    }

    private NTxViewerConfig validate(NTxViewerConfig config) {
        if (config == null) {
            config = new NTxViewerConfig();
        }
        if (config.getRecentProjects() == null) {
            config.setRecentProjects(new NTxProject[0]);
        }
        config.setRecentProjects(Arrays.stream(config.getRecentProjects())
                .filter(x -> x != null && !NBlankable.isBlank(x.getPath()))
                .peek(x -> {
                    if (x.getLastAccess() == null) {
                        x.setLastAccess(new Date());
                    }
                })
                .sorted((a, b) -> {
                    int p;
                    p = compare(b.getLastAccess(), a.getLastAccess());
                    if (p != 0) {
                        return p;
                    }
                    return compare(a.getPath(), b.getPath());
                }).toArray(NTxProject[]::new)
        );
        return config;
    }
}
