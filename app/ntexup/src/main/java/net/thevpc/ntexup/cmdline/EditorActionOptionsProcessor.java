package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.cmdline.options.EditorActionOptions;
import net.thevpc.nuts.artifact.NVersion;
import net.thevpc.nuts.core.NWorkspace;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

public class EditorActionOptionsProcessor {
    EditorActionOptions v;

    public EditorActionOptionsProcessor(EditorActionOptions v) {
        this.v = v;
    }

    public void process() {
        if (v.idea) {
            runActionInstallIdea();
        }
        if (v.kate) {
            runActionInstallKate();
        }
        if (v.vim) {
            runActionInstallVim();
        }
        if (v.gedit) {
            runActionInstallGedit();
        }
        if (v.jedit) {
            runActionInstallJEdit();
        }
        if (v.nodepadpp) {
            runActionInstallNodepadPlusPlus();
        }
        if (v.vscode) {
            runActionInstallVscode();
        }
    }

    private void runActionInstallIdea() {
        NMsg ntexup = NMsg.ofStyledKeyword("ntexup");
        NMsg app = NMsg.ofStyledDate("IntelliJIdea");
        NPath local = null;
        // ".config/JetBrains/IntelliJIdea2024.1/filetypes/
        NPath jb = NPath.ofUserHome().resolve(".config/JetBrains/");
        boolean doForce = false;
        if (jb.isDirectory()) {
            local = jb.list().stream().filter(x -> {
                if (!x.isDirectory()) {
                    return false;
                }
                if (!x.resolve("idea64.vmoptions").isRegularFile()) {
                    return false;
                }
                return true;
            }).sorted((a, b) -> {
                return -NVersion.of(resolveJetbrainsVersion(a.getName()))
                        .compareTo(resolveJetbrainsVersion(b.getName()));
            }).findFirst().orElse(null);
        }
        if (local == null) {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported. Idea does not seem to be installed", ntexup, app, NWorkspace.of().getOsFamily()));
            return;
        } else if (!local.isRegularFile()) {
        } else {
            if (v.force) {
                doForce = true;
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting already to %s", ntexup, app, local));
                return;
            }
        }
        NPath remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/intellij/ntexup.xml");
        remote.mkdirs().copyTo(local);
        if (doForce) {
            NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s", ntexup, app, local));
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", ntexup, app, local));
        }
    }

    private String resolveJetbrainsVersion(String name) {
        if (name.startsWith("IdeaIC")) {
            return name.substring("IdeaIC".length());
        }
        if (name.startsWith("IntelliJIdea")) {
            return name.substring("IntelliJIdea".length());
        }
        return "";
    }

    private void runActionInstallKate() {
        NMsg ntexup = NMsg.ofStyledKeyword("ntexup");
        NMsg app = NMsg.ofStyledDate("kate");
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".local/share/org.kde.syntax-highlighting/syntax/");
            boolean doForce = false;
            if (!local.isRegularFile()) {
            } else {
                if (v.force) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("%s %s syntax highlighting already to %s", ntexup, app, local));
                    return;
                }
            }
            NPath remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/kate/ntexup.xml");
            remote.mkdirs().copyTo(local);
            NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", ntexup, app, local));
            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s", ntexup, app, local));
            }
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", ntexup, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallVim() {
        NMsg ntexup = NMsg.ofStyledKeyword("ntexup");
        NMsg app = NMsg.ofStyledDate("vim");
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".vim/");
            boolean doForce = false;
            if (
                    !local.resolve("syntax/ntexup.vim").isRegularFile()
                            && !local.resolve("ftdetect/ntexup.vim").isRegularFile()
            ) {
                //
            } else {
                if (v.force) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("%s %s syntax highlighting already to %s", ntexup, app, local));
                    return;
                }
            }

            NPath remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/vim/syntax/ntexup.vim");
            remote.mkdirs().copyTo(local.resolve("syntax/ntexup.vim"));
            remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/vim/ftdetect/ntexup.vim");
            remote.mkdirs().copyTo(local.resolve("ftdetect/ntexup.vim"));

            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s", ntexup, app, local));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", ntexup, app, local.resolve("syntax/ntexup.vim")));
            }
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", ntexup, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallVscode() {
        NMsg ntexup = NMsg.ofStyledKeyword("ntexup");
        NMsg app = NMsg.ofStyledDate("Visual Studio Code");
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".vscode/extensions/ntexup-syntax");
            boolean doForce = false;
            if (
                    !local.isDirectory()
                            || !local.resolve("package.json").isRegularFile()
            ) {
                //
            } else {
                if (v.force) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("%s %s syntax highlighting already to %s", ntexup, app, local));
                    return;
                }
            }

            NPath remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/vscode/ntexup-syntax/package.json");
            remote.mkdirs().copyTo(local.resolve("package.json"));
            remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/vscode/ntexup-syntax/syntaxes/ntexup.tmLanguage.json");
            remote.mkdirs().copyTo(local.resolve("syntaxes/ntexup.tmLanguage.json"));

            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s", ntexup, app, local));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", ntexup, app, local));
            }
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", ntexup, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallGedit() {
        NMsg ntexup = NMsg.ofStyledKeyword("ntexup");
        NMsg app = NMsg.ofStyledDate("gedit");
        if (NWorkspace.of().getOsFamily().isPosix()) {
            NPath local = NPath.ofUserHome().resolve(".local/share/gtksourceview-4/language-specs/ntexup.lang");
            boolean doForce = false;
            if (!local.isRegularFile()) {
                //
            } else {
                if (v.force) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("%s %s syntax highlighting already to %s", ntexup, app, local));
                    return;
                }
            }

            NPath remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/gedit/ntexup.lang");
            remote.mkdirs().copyTo(local);
            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s", ntexup, app, local));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", ntexup, app, local));
            }
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", ntexup, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallNodepadPlusPlus() {
        NMsg ntexup = NMsg.ofStyledKeyword("ntexup");
        NMsg app = NMsg.ofStyledDate("Nodepad++");
        if (NWorkspace.of().getOsFamily().isWindow()) {
            NPath local = NPath.ofUserHome().resolve(NWorkspace.of().getSysEnv("APPDATA") + "/Notepad++/userDefineLangs/ntexup.xml");
            boolean doForce = false;
            if (!local.isRegularFile()) {
                //
            } else {
                if (v.force) {
                    doForce = true;
                } else {
                    NOut.println(NMsg.ofC("%s %s syntax highlighting already to %s", ntexup, app, local));
                    return;
                }
            }

            NPath remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/nodepadpp/ntexup.xml");
            remote.mkdirs().copyTo(local);
            if (doForce) {
                NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s", ntexup, app, local));
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", ntexup, app, local));
            }
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting for %s is not supported", ntexup, app, NWorkspace.of().getOsFamily()));
        }
    }

    private void runActionInstallJEdit() {
        NMsg ntexup = NMsg.ofStyledKeyword("ntexup");
        NMsg app = NMsg.ofStyledDate("jEdit");
        NPath local = NPath.ofUserHome().resolve(".jedit/modes/ntexup.xml");
        boolean doForce = false;
        if (!local.isRegularFile()) {
            //
        } else {
            if (v.force) {
                doForce = true;
            } else {
                NOut.println(NMsg.ofC("%s %s syntax highlighting already to %s", ntexup, app, local));
                return;
            }
        }

        NPath remote = NPath.of("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration/jedit/ntexup.xml");
        remote.mkdirs().copyTo(local);
        if (doForce) {
            NOut.println(NMsg.ofC("%s %s syntax highlighting re-installed successfully to %s", ntexup, app, local));
        } else {
            NOut.println(NMsg.ofC("%s %s syntax highlighting installed successfully to %s", ntexup, app, local));
        }
    }

}
