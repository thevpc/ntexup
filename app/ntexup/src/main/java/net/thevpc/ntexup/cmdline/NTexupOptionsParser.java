package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.cmdline.options.*;
import net.thevpc.ntexup.config.NTxViewerConfigManager;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NStringUtils;

public class NTexupOptionsParser {
    public void parse(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            while (!cmdLine.isEmpty()) {
                cmdLine.matcher()
                        .with("show", "open").matchAny(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class).ifNoProjectViewCurrentDirectory = true;
                            if (a.getStringValue().isPresent()) {
                                options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.stringValue()));
                            }
                            continueParsingShow(cmdLine, options);
                        })
                        .with("show-doc").matchTrueFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class);
                            options.getOrCreate(ShowActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                            continueShowDoc(cmdLine, options);
                        })
                        .with("generate-doc").matchTrueFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                            continueParsingGeneratePdfDoc(cmdLine, options);
                        })
                        .with("reopen").matchTrueFlag(a -> {
                            NTxViewerConfigManager c = new NTxViewerConfigManager();
                            NPath p = c.getLatestProjectPath();
                            if (p != null) {
                                options.getOrCreate(ShowActionOptions.class).addPath(p);
                            }
                            continueParsingReopen(cmdLine, options);
                        })
                        .with("build-repo").matchAny(a -> {
                            options.getOrCreate(BuildRepoActionOptions.class);
                            if (a.getStringValue().isPresent()) {
                                options.getOrCreate(BuildRepoActionOptions.class).addPath(NPath.of(a.stringValue()));
                            }
                            continueParsingBuildRepository(cmdLine, options);
                        })
                        .with("list-templates").matchFlag(a -> {
                            options.getOrCreate(ListTemplatesActionOptions.class);
                            continueParsingListTemplates(cmdLine, options);
                        })
                        .with("generate", "pdf").matchFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class);
                            continueParsingGeneratePdf(cmdLine, options);
                        })
                        .with("new").matchTrueFlag(a -> {
                            options.getOrCreate(NewActionOptions.class);
                            continueParsingNew(cmdLine, options);
                        })
                        .with("--gui").matchFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class);
                            NSession.of().setGui(a.booleanValue());
                        })
                        .with("--install-syntax").matchEntry(a -> {
                            EditorActionOptions w = options.getOrCreate(EditorActionOptions.class);
                            w.getSyntaxInfo().addEditor(NStringUtils.firstNonBlank(a.getStringValue().orNull(), "all"));
                            while (!cmdLine.isEmpty()) {
                                cmdLine.matcher()
                                        .with("--force").matchFlag(aa -> {
                                            w.getSyntaxInfo().setForce(aa.booleanValue());
                                        })
                                        .withNonOption().matchAny(aa -> w.getSyntaxInfo().addEditor(aa.asString().orNull()))
                                        .requireDefaults();
                            }
                        })
                        .with("--dump").matchFlag(a -> {
                            options.getOrCreate(DumpDocumentOptions.class);
                        })
                        .withCondition(c -> {
                            NArg u = c.peek().get();
                            if (!u.isOption() && u.isNonOption()) {
                                String m = u.image();
                                if (m.equals(".") || m.equals("..") || m.contains("/") || m.contains("\\")) {
                                    return true;
                                }
                            }
                            return false;
                        }).matchAny(a -> {
                            options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.image()));
                        })
                        .requireDefaults();
            }
        }
//        if (options.documentation) {
//            options.paths.add(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
//        }
//        if (!options.guiMode && !options.terminalMode) {
//            options.guiMode = NSession.of().isGui();
//            options.terminalMode = !options.guiMode;
//        } else if (options.guiMode && options.terminalMode) {
//            options.guiMode = NSession.of().isGui();
//            options.terminalMode = !options.guiMode;
//        }
    }

    private void continueShowDoc(NCmdLine cmdLine, Options options) {
        cmdLine.skipAll();
    }

    private void continueParsingNew(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .with("--show").matchFlag(a -> {
                        options.getOrCreate(NewActionOptions.class).openViewer = true;
                    })
                    .with("--show-doc").matchFlag(a -> {
                        options.getOrCreate(ShowFrameActionOptions.class);
                        options.getOrCreate(ShowActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                    })
                    .with("--generate-pdf").matchFlag(a -> {
                        options.getOrCreate(NewActionOptions.class).generatePdf = true;
                        if (a.getStringValue().isPresent()) {
                            options.getOrCreate(NewActionOptions.class).generatePdfOutput = NPath.of(a.stringValue());
                        }
                    })
                    .with("--generate-doc-pdf").matchFlag(a -> {
                        options.getOrCreate(GenerateActionOptions.class).outputFormat = OutputFormat.PDF;
                        options.getOrCreate(GenerateActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                        if (a.getStringValue().isPresent()) {
                            options.getOrCreate(GenerateActionOptions.class).output = NPath.of(a.stringValue());
                        }
                    })
                    .with("--template", "-t").matchEntry(a -> options.getOrCreate(NewActionOptions.class).templateUrl = a.stringValue())
                    .withNonOption().matchAny(a -> options.getOrCreate(NewActionOptions.class).addPath(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingReopen(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .with("--show-doc").matchFlag(a -> {
                        options.getOrCreate(ShowFrameActionOptions.class);
                        options.getOrCreate(ShowActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                    })
                    .withNonOption().matchAny(a -> options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingBuildRepository(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .withNonOption().matchAny(a -> options.getOrCreate(BuildRepoActionOptions.class).addPath(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingListTemplates(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .requireDefaults();
        }
    }


    private void continueParsingGeneratePdfDoc(NCmdLine cmdLine, Options options) {
        continueParsingGeneratePdf(cmdLine, options);
    }

    private void continueParsingGeneratePdf(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--output").matchEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).output = NPath.of(a.stringValue());
                    })
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .withCondition(c -> {
                        NArg a = c.peek().get();
                        return a.isOption() && a.key().startsWith("--var-");
                    }).matchEntry(a -> {
                        options.vars.put(a.key().substring("--var-".length()), a.stringValue());
                    })
                    .withNonOption().matchAny(a -> options.getOrCreate(GenerateActionOptions.class).addPath(NPath.of(a.image())))
                    .requireDefaults();
        }
        if (options.getOrCreate(GenerateActionOptions.class).paths.isEmpty()) {
            options.getOrCreate(GenerateActionOptions.class).addPath(NPath.ofUserDirectory());
        }
    }

    private void continueParsingShow(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
//                    .withCondition(c -> {
//                        NArg a = c.peek().get();
//                        return a.isOption() && a.key().startsWith("--var-");
//                    }).matchEntry(a -> {
//                        options.vars.put(a.key().substring("--var-".length()), a.stringValue());
//                    })
                    .withNonOption().matchAny(a -> options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.image())))
                    .requireDefaults();
        }
        if (options.getOrCreate(ShowActionOptions.class).paths.isEmpty()) {
            options.getOrCreate(ShowActionOptions.class).addPath(NPath.ofUserDirectory());
        }
    }


}
