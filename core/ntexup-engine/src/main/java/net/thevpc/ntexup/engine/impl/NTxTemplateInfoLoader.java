package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.engine.eval.NTxGitHelper;
import net.thevpc.nuts.NIllegalArgumentException;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementAnnotation;
import net.thevpc.nuts.elem.NElementParser;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NTxTemplateInfoLoader {

    public List<NTxTemplateInfo> loadTemplateInfo(String name, NPath path, NTxLogger log) {
        List<NTxTemplateInfo> allTemplates = new ArrayList<>();
        try {
            if (NTxGitHelper.isGithubFolder(path.toString())) {
                NPath nPath1 = NTxGitHelper.resolveGithubPath(path.toString(), log);
                if (nPath1.resolve("ntexup-repository.tson").isRegularFile()) {
                    try {
                        loadTemplateInfo(NElementParser.ofTson().parse(nPath1.resolve("ntexup-repository.tson")), name, path, allTemplates);
                    } catch (Exception e) {
                        log.log(NMsg.ofC("unable to parse repository templates '%s' at '%s' : %s", name, path, e).asError());
                    }
                } else {
                    log.log(NMsg.ofC("repository template not found '%s' at '%s'", name, path).asWarning());
                }
            } else if (path.isLocal()) {
                if (path.resolve("ntexup-repository.tson").isRegularFile()) {
                    try {
                        loadTemplateInfo(NElementParser.ofTson().parse(path.resolve("ntexup-repository.tson")), name, path, allTemplates);
                    } catch (Exception e) {
                        log.log(NMsg.ofC("unable to parse repository templates '%s' at '%s' : %s", name, path, e).asError());
                    }
                } else {
                    log.log(NMsg.ofC("repository template not found '%s' at '%s'", name, path).asWarning());
                }
            } else {
                log.log(NMsg.ofC("unable to parse repository templates '%s' at '%s'", name, path).asWarning());
            }
        } catch (Exception e) {
            log.log(NMsg.ofC("unable to load repository '%s' at '%s' : %s", name, path, e).asError());
        }
        return allTemplates;
    }

    private void loadTemplateInfo(NElement elem, String repoName, NPath repoPath, List<NTxTemplateInfo> allTemplates) {
        if (elem.isObject()) {
            for (NElement child : elem.asObject().get().children()) {
                loadTemplateInfo(child, repoName, repoPath, allTemplates);
            }
        }else if (elem.isArray()) {
            for (NElement child : elem.asArray().get().children()) {
                loadTemplateInfo(child, repoName, repoPath, allTemplates);
            }
        }else if (elem.isNamedUplet("template")) {
            String name = null;
            List<String> binaries = new ArrayList<>();
            String layout = null;
            String version = null;
            boolean recommended = false;
            for (NElement o : elem.asUplet().get().children()) {
                if (o.isNamedPair()) {
                    NPairElement p = o.asNamedPair().get();
                    switch (p.key().asStringValue().get()) {
                        case "recommended": {
                            recommended = p.value().asBooleanValue().get();
                            break;
                        }
                        case "name": {
                            name = p.value().asStringValue().get();
                            break;
                        }
                        case "binaries": {
                            if (p.value().isArray()) {
                                for (NElement child : p.value().asArray().get().children()) {
                                    binaries.add(child.asStringValue().get());
                                }
                            } else {
                                binaries.add(p.value().asStringValue().get());
                            }
                            break;
                        }
                        case "version": {
                            version = p.value().asStringValue().get();
                            break;
                        }
                        case "layout": {
                            layout = p.value().asStringValue().get();
                            break;
                        }
                        default: {
                            throw new NIllegalArgumentException(NMsg.ofC("unsupported %s", p));
                        }
                    }
                } else if (o.isName("recommended")) {
                    recommended = true;
                } else {
                    throw new NIllegalArgumentException(NMsg.ofC("unsupported %s", o));
                }
            }
            allTemplates.add(
                    new NTxTemplateInfoImpl(
                            NStringUtils.trim(name),
                            layout, version, repoPath.resolveChild(NMsg.ofC("$s/v$s/templates/$s", name, version, layout).toString()).toString(),
                            recommended,
                            repoName, repoPath.toString(),
                            NStringUtils.trim(NMsg.ofC("/$s/v$s/templates/$s", name, version, layout).toString()),
                            binaries.toArray(new String[0])
                    )
            );
        } else {
            throw new NIllegalArgumentException(NMsg.ofC("unsupported %s", elem));
        }
    }

    public List<NTxTemplateInfo> loadTemplateInfoOwn(String name, NPath path, NTxLogger log) {
        List<NTxTemplateInfo> allTemplates = new ArrayList<>();
        try {
            if (NTxGitHelper.isGithubFolder(path.toString())) {
                NPath nPath1 = NTxGitHelper.resolveGithubPath(path.toString(), log);
                if (nPath1.resolve("ntexup-repository.tson").isRegularFile()) {
                    String layout = path.getParent().getName();
                    String version = path.getParent().getParent().getName();
                    try {
                        loadTemplateInfoOwn(NElementParser.ofTson().parse(nPath1.resolve("ntexup-repository.tson")), name, path, allTemplates,version, layout);
                    } catch (Exception e) {
                        log.log(NMsg.ofC("unable to parse repository templates '%s' at '%s' : %s", name, path, e).asError());
                    }
                } else {
                    log.log(NMsg.ofC("repository template not found '%s' at '%s'", name, path).asWarning());
                }
            } else if (path.isLocal()) {
                if (path.resolve("ntexup-repository.tson").isRegularFile()) {
                    try {
                        String layout = path.getParent().getName();
                        String version = path.getParent().getParent().getName();
                        loadTemplateInfoOwn(NElementParser.ofTson().parse(path.resolve("ntexup-repository.tson")), name, path, allTemplates, layout, version);
                    } catch (Exception e) {
                        log.log(NMsg.ofC("unable to parse repository templates '%s' at '%s' : %s", name, path, e).asError());
                    }
                } else {
                    log.log(NMsg.ofC("repository template not found '%s' at '%s'", name, path).asWarning());
                }
            } else {
                log.log(NMsg.ofC("unable to parse repository templates '%s' at '%s'", name, path).asWarning());
            }
        } catch (Exception e) {
            log.log(NMsg.ofC("unable to load repository '%s' at '%s' : %s", name, path, e).asError());
        }
        return allTemplates;
    }

    private void loadTemplateInfoOwn(NElement elem, String repoName, NPath repoPath, List<NTxTemplateInfo> allTemplates,String version,String layout) {
        if (elem.isNamedUplet("template")) {
            String name = null;
            List<String> binaries = new ArrayList<>();
            boolean recommended = false;
            for (NElement o : elem.asObject().get().children()) {
                if (o.isNamedPair()) {
                    NPairElement p = o.asNamedPair().get();
                    switch (p.key().asStringValue().get()) {
                        case "recommended": {
                            recommended = p.value().asBooleanValue().get();
                            break;
                        }
                        case "name": {
                            name = p.value().asStringValue().get();
                            break;
                        }
                        case "binaries": {
                            if (p.value().isArray()) {
                                for (NElement child : p.value().asArray().get().children()) {
                                    binaries.add(child.asStringValue().get());
                                }
                            } else {
                                binaries.add(p.value().asStringValue().get());
                            }
                            break;
                        }
                        case "version": {
                            version = p.value().asStringValue().get();
                            break;
                        }
                        case "layout": {
                            layout = p.value().asStringValue().get();
                            break;
                        }
                        default: {
                            throw new NIllegalArgumentException(NMsg.ofC("unsupported %s", p));
                        }
                    }
                } else if (o.isName("recommended")) {
                    recommended = true;
                } else {
                    throw new NIllegalArgumentException(NMsg.ofC("unsupported %s", o));
                }
            }
            allTemplates.add(
                    new NTxTemplateInfoImpl(
                            NStringUtils.trim(name),
                            layout, version, repoPath.resolveChild(NMsg.ofC("$s/v$s/templates/$s", name, version, layout).toString()).toString(),
                            recommended,
                            repoName, repoPath.toString(),
                            NStringUtils.trim(NMsg.ofC("/$s/v$s/templates/$s", name, version, layout).toString()),
                            binaries.toArray(new String[0])
                    )
            );
        } else {
            throw new NIllegalArgumentException(NMsg.ofC("unsupported %s", elem));
        }
    }
}
