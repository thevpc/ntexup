package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.cmdline.options.*;
import net.thevpc.ntexup.config.NTxViewerConfigManager;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;

public class NTexupOptionsParser {
    public void parse(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            while (!cmdLine.isEmpty()) {
                cmdLine.matcher()
                        .with("--show-log").matchTrueFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class).viewLog=true;
                        })
                        .with("--show").matchTrueFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class);
                        })
                        .with("show","open").matchTrueFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class).ifNoProjectViewCurrentDirectory=true;
                            if (a.getStringValue().isPresent()) {
                                options.getOrCreate(ShowActionOptions.class).paths.add(NPath.of(a.stringValue()));
                            } else {
                                options.getOrCreate(ShowActionOptions.class).paths.add(NPath.ofUserDirectory());
                            }
                            continueParsingShow(cmdLine, options);
                        })
                        .with("show-doc").matchTrueFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class);
                            options.getOrCreate(ShowActionOptions.class).paths.add(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                            continueShowDoc(cmdLine,options);
                        })
                        .with("generate-doc").matchTrueFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class).paths.add(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                            continueParsingGeneratePdfDoc(cmdLine, options);
                        })
                        .with("--gui").matchFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class);
                            NSession.of().setGui(a.booleanValue());
                        })
                        .with("reopen").matchTrueFlag(a -> {
                            NTxViewerConfigManager c = new NTxViewerConfigManager();
                            NPath p = c.getLatestProjectPath();
                            if (p != null) {
                                options.getOrCreate(ShowActionOptions.class).paths.add(p);
                            }
                            continueParsingReopen(cmdLine, options);
                        })
                        .with("build-repo").matchTrueFlag(a -> {
                            options.getOrCreate(BuildRepoActionOptions.class);
                            continueParsingBuildRepository(cmdLine, options);
                        })
                        .with("list-templates").matchFlag(a -> {
                            options.getOrCreate(ListTemplatesActionOptions.class);
                            continueParsingListTemplates(cmdLine, options);
                        })
                        .with("--dump").matchFlag(a -> {
                            options.getOrCreate(DumpDocumentOptions.class);
                        })
                        .with("generate").matchFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class);
                            continueParsingGeneratePdf(cmdLine, options);
                        })
                        .with("new").matchTrueFlag(a -> {
                            options.getOrCreate(NewActionOptions.class);
                            continueParsingNew(cmdLine, options);
                        })
                        .withNonOption().matchAny(a -> {
                            options.getOrCreate(ShowActionOptions.class).paths.add(NPath.of(a.image()));
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
                    .with("--view").matchFlag(a -> {
                        options.getOrCreate(NewActionOptions.class).openViewer=true;
                    })
                    .with("--view-doc").matchFlag(a -> {
                        options.getOrCreate(ShowFrameActionOptions.class);
                        options.getOrCreate(ShowActionOptions.class).paths.add(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                    })
                    .with("--generate-pdf").matchFlag(a -> {
                        options.getOrCreate(NewActionOptions.class).generatePdf = true;
                        if(a.getStringValue().isPresent()) {
                            options.getOrCreate(NewActionOptions.class).generatePdfOutput=NPath.of(a.stringValue());
                        }
                    })
                    .with("--generate-doc-pdf").matchFlag(a -> {
                        options.getOrCreate(GenerateActionOptions.class).outputFormat=OutputFormat.PDF;
                        options.getOrCreate(GenerateActionOptions.class).paths.add(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                        if(a.getStringValue().isPresent()) {
                            options.getOrCreate(GenerateActionOptions.class).output=NPath.of(a.stringValue());
                        }
                    })
                    .with("--template","-t").matchEntry(a -> options.getOrCreate(NewActionOptions.class).templateUrl = a.stringValue())
                    .withNonOption().matchAny(a -> options.getOrCreate(NewActionOptions.class).paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingReopen(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .with("--view-doc").matchFlag(a -> {
                        options.getOrCreate(ShowFrameActionOptions.class);
                        options.getOrCreate(ShowActionOptions.class).paths.add(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                    })
                    .withNonOption().matchAny(a -> options.getOrCreate(ShowActionOptions.class).paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingBuildRepository(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .withNonOption().matchAny(a -> options.getOrCreate(BuildRepoActionOptions.class).paths.add(NPath.of(a.image())))
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
                    .withNonOption().matchAny(a -> options.getOrCreate(GenerateActionOptions.class).paths.add(NPath.of(a.image())))
                    .requireDefaults();
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
                    .withNonOption().matchAny(a ->  options.getOrCreate(ShowActionOptions.class).paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }


}
