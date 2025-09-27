package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.NSession;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsg;

public class NTexupOptionsParser {
    public void parse(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            while (!cmdLine.isEmpty()) {
                cmdLine.matcher()
                        .with("--view-log").matchTrueFlag(a -> {
                            options.setGuiMode(true);
                            options.showLogs = true;
                        })
                        .with("--view").matchTrueFlag(a -> {
                            options.setGuiMode(true);
                        })
                        .with("--gui").matchFlag(a -> {
                            options.setGuiMode(a.booleanValue());
                            NSession.of().setGui(a.booleanValue());
                        })
                        .with("--reopen").matchTrueFlag(a -> {
                            options.reopen = true;
                            continueParsingReopen(cmdLine, options);
                        })
                        .with("--build-repo").matchTrueFlag(a -> {
                            options.setTerminalMode(true);
                            continueParsingBuildRepository(cmdLine, options);
                        })
                        .with("--list-templates").matchFlag(a -> {
                            options.setTerminalMode(true);
                            continueParsingListTemplates(cmdLine, options);
                        })
                        .with("--dump").matchFlag(a -> {
                            options.setTerminalMode(true);
                            options.dump = true;
                        })
                        .with("--documentation").matchFlag(a -> options.documentation = true)
                        .with("--output").matchEntry(a -> {
                            options.setTerminalMode(true);
                            options.output = NPath.of(a.stringValue());
                        })
                        .with("--open").matchEntry(a -> {
                            if (a.getStringValue().isPresent()) {
                                options.paths.add(NPath.of(a.stringValue()));
                            } else {
                                options.paths.add(NPath.ofUserDirectory());
                            }
                            continueParsingOpen(cmdLine, options);
                        })
                        .with("--pdf").matchFlag(a -> {
                            options.setTerminalMode(true);
                            continueParsingPdf(cmdLine, options);
                        })
                        .with("--new").matchTrueFlag(a -> {
                            continueParsingNew(cmdLine, options);
                        })
                        .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                        .requireDefaults();
            }
        }
        if (options.documentation) {
            options.paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
        }
        if (!options.guiMode && !options.terminalMode) {
            options.guiMode = NSession.of().isGui();
            options.terminalMode = !options.guiMode;
        } else if (options.guiMode && options.terminalMode) {
            options.guiMode = NSession.of().isGui();
            options.terminalMode = !options.guiMode;
        }
    }

    private void continueParsingNew(NCmdLine cmdLine, Options options) {
        options.action = Action.NEW;
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.dump = true)
                    .with("--template").matchEntry(a -> options.templateUrl = a.stringValue())
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingReopen(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> {
                        options.dump = true;
                        options.setTerminalMode(true);
                    })
                    .with("--documentation").matchFlag(a -> options.documentation = true)
                    .with("--output").matchEntry(a -> options.output = NPath.of(a.stringValue()))
                    .with("--open").matchEntry(a -> {
                        options.paths.add(NPath.of(a.stringValue()));
                        continueParsingOpen(cmdLine, options);
                    })
                    .with("--pdf").matchEntry(a -> {
                        options.setTerminalMode(true);
                        continueParsingPdf(cmdLine, options);
                    })
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingBuildRepository(NCmdLine cmdLine, Options options) {
        options.action = Action.BUILD_REPO;
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchTrueFlag(a -> {
                        options.dump = true;
                        options.setTerminalMode(true);
                    })
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingListTemplates(NCmdLine cmdLine, Options options) {
        options.action = net.thevpc.ntexup.cmdline.Action.LIST_TEMPLATES;
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> {
                        options.dump = true;
                        options.setTerminalMode(true);
                    })
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingOpen(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> {
                        options.dump = true;
                        options.setTerminalMode(true);
                    })
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingPdf(NCmdLine cmdLine, Options options) {
        options.outputFormat = OutputFormat.PDF;
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--output").matchEntry(a -> {
                        options.output = NPath.of(a.stringValue());
                        options.setTerminalMode(true);
                    })
                    .with("--dump").matchFlag(a -> {
                        options.dump = true;
                        options.setTerminalMode(true);
                    })
                    .withCondition(c -> {
                        NArg a = c.peek().get();
                        return a.isOption() && a.key().startsWith("--var-");
                    }).matchEntry(a -> {
                        options.vars.put(a.key().substring("--var-".length()), a.stringValue());
                    })
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

}
