package net.thevpc.ntexup.engine.repo;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.engine.impl.NTxTemplateInfoImpl;
import net.thevpc.ntexup.engine.impl.NTxTemplateInfoLoader;
import net.thevpc.nuts.artifact.NVersion;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RepoBuilderTool {
    private NTxLogger log;

    public RepoBuilderTool(NTxLogger log) {
        this.log = log;
    }

    public boolean buildRepository(NPath path) {
        if (!isRepositoryFolder(path)) {
            log.log(NMsg.ofC("not a ntexup repository '%s'", path).asError());
            return false;
        }
        List<NTxTemplateInfo> all = new ArrayList<>();
        Set<NVersion> versions = new HashSet<>();
        for (NPath nPath : path.list()) {
            if (isThemeFolder(nPath)) {
                for (NTxTemplateInfo a : buildRepoTheme(nPath)) {
                    all.add(a);
                    for (String s : a.binaryVersions()) {
                        NVersion v = NVersion.of(s);
                        if (v.isSingleValue()) {
                            versions.add(v);
                        }
                    }
                }
            }
        }
        try (PrintStream out = path.resolve("ndoc-repository.tson").getPrintStream()) {
            for (NTxTemplateInfo nTxTemplateInfo : all) {
                out.println(nTxTemplateInfo.toElement().toString(true));
            }
        }
        for (NVersion version : versions) {
            try (PrintStream out = path.resolve("ndoc-repository-" + version + ".tson").getPrintStream()) {
                for (NTxTemplateInfo nTxTemplateInfo : all) {
                    List<String> bv = nTxTemplateInfo.binaryVersions();
                    for (String v : bv) {
                        if (NVersion.of(v).filter().acceptVersion(version)) {
                            out.println(nTxTemplateInfo.toElement().toString(true));
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isRepositoryFolder(NPath folder) {
        if (!folder.isDirectory()) {
            return false;
        }
        for (NPath nPath : folder.list()) {
            if (isThemeFolder(nPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isThemeFolder(NPath folder) {
        if (!folder.isDirectory()) {
            return false;
        }
        List<NPath> versions = folder.list().stream().filter(x -> isThemeVersionFolder(x)).collect(Collectors.toList());
        return !versions.isEmpty();
    }

    private boolean isThemeVersionFolder(NPath folder) {
        if (!folder.isDirectory()) {
            return false;
        }
        if (!folder.resolve("templates").isDirectory()) {
            return false;
        }
        if (!folder.resolve("theme").isDirectory()) {
            return false;
        }
        return true;
    }

    private List<NTxTemplateInfo> buildRepoTheme(NPath folder) {
        List<NPath> versions = folder.list().stream().filter(x -> isThemeVersionFolder(x)).collect(Collectors.toList());
        List<NTxTemplateInfo> ok = new ArrayList<>();
        for (NPath version : versions) {
            for (NTxTemplateInfo a : buildRepoVersionTheme(version, folder.getName())) {
                ok.add(
                        new NTxTemplateInfoImpl(
                                a.name(),
                                a.layout(), a.version(), null,
                                a.recommended(),
                                null,
                                null,
                                "/" + folder.getName() + "/" + version.getName() + "/templates/" + a.localPath(),
                                a.binaryVersions().toArray(new String[0])
                        )
                );
            }
        }
        return ok;
    }

    private List<NTxTemplateInfo> buildRepoVersionTheme(NPath versionFolder, String themeName) {
        String versionName = versionFolder.getName();
        List<NPath> templateFolders = versionFolder.resolve("templates").list().stream().filter(x -> x.isDirectory()).collect(Collectors.toList());
        NPath themeInfo = versionFolder.resolve("ndoc-theme.tson");
        List<NTxTemplateInfo> existing = null;
        List<NTxTemplateInfo> newTemplates = new ArrayList<>();
        if (themeInfo.isRegularFile()) {
            NTxTemplateInfoLoader loader = new NTxTemplateInfoLoader();
            existing = loader.loadTemplateInfo(themeName, themeInfo, log);
            newTemplates.addAll(existing);
            newTemplates.removeIf(x -> !versionFolder.resolve("templates").resolve(x.localPath()).isDirectory());
            for (NPath templateFolder : templateFolders) {
                NTxTemplateInfo old = newTemplates.stream().filter(x -> x.localPath().equals(templateFolder.getName())).findFirst().orElse(null);
                if (old == null) {
                    newTemplates.add(new NTxTemplateInfoImpl(themeName, templateFolder.getName(), versionName, null, false, null, null, templateFolder.getName(), new String[]{NTxEngine.CURRENT_VERSION}));
                } else {
                    if (old.binaryVersions().isEmpty()) {
                        newTemplates.remove(old);
                        newTemplates.add(old.withBinaryVersions(new String[]{NTxEngine.CURRENT_VERSION}));
                    }
                }
            }
        } else {
            for (NPath templateFolder : templateFolders) {
                newTemplates.add(new NTxTemplateInfoImpl(themeName, templateFolder.getName(), versionName, null, false, null, null, templateFolder.getName(), new String[]{NTxEngine.CURRENT_VERSION}));
            }
        }
        if (!newTemplates.isEmpty()) {
            if (newTemplates.stream().noneMatch(x -> x.recommended())) {
                NTxTemplateInfo v = newTemplates.stream().filter(x -> x.localPath().equals("medium")).findFirst().orElse(null);
                if (v != null) {
                    newTemplates.remove(v);
                    newTemplates.add(v.withRecommended(true));
                } else {
                    v = newTemplates.remove(0);
                    newTemplates.add(v.withRecommended(true));
                }
            }
        }
        if (!newTemplates.equals(existing)) {
            try (PrintStream out = themeInfo.getPrintStream()) {
                for (NTxTemplateInfo nTxTemplateInfo : newTemplates) {
                    out.println(nTxTemplateInfo.toElement().toString(true));
                }
            }
        }
        return newTemplates;
    }
}
