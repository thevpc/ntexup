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
                        .with("--view-log").matchTrueFlag(a -> {
                            options.getOrCreate(ViewFrameActionOptions.class).viewLog=true;
                        })
                        .with("--view").matchTrueFlag(a -> {
                            options.getOrCreate(ViewFrameActionOptions.class);
                        })
                        .with("view").matchTrueFlag(a -> {
                            options.getOrCreate(ViewFrameActionOptions.class).ifNoProjectViewCurrentDirectory=true;
                            continueParsingView(cmdLine, options);
                        })
                        .with("view-doc").matchTrueFlag(a -> {
                            options.getOrCreate(ViewFrameActionOptions.class);
                            options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
                            continueShowDoc(cmdLine,options);
                        })
                        .with("generate-doc-pdf").matchTrueFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class).outputFormat=OutputFormat.PDF;
                            options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
                            continueParsingGeneratePdfDoc(cmdLine, options);
                        })
                        .with("--gui").matchFlag(a -> {
                            options.getOrCreate(ViewFrameActionOptions.class);
                            NSession.of().setGui(a.booleanValue());
                        })
                        .with("reopen").matchTrueFlag(a -> {
                            NTxViewerConfigManager c = new NTxViewerConfigManager();
                            NPath p = c.getLatestProjectPath();
                            if (p != null) {
                                options.getOrCreate(OpenActionOptions.class).paths.add(p);
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
                        .with("open").matchEntry(a -> {
                            if (a.getStringValue().isPresent()) {
                                options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of(a.stringValue()));
                            } else {
                                options.getOrCreate(OpenActionOptions.class).paths.add(NPath.ofUserDirectory());
                            }
                            continueParsingOpen(cmdLine, options);
                        })
                        .with("generate-pdf").matchFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class).outputFormat=OutputFormat.PDF;
                            continueParsingGeneratePdf(cmdLine, options);
                        })
                        .with("new").matchTrueFlag(a -> {
                            options.getOrCreate(NewActionOptions.class);
                            continueParsingNew(cmdLine, options);
                        })
                        .withNonOption().matchAny(a -> {
                            options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of(a.image()));
                        })
                        .requireDefaults();
            }
        }
//        if (options.documentation) {
//            options.paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
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
                        options.getOrCreate(ViewFrameActionOptions.class);
                        options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
                    })
                    .with("--generate-pdf").matchFlag(a -> {
                        options.getOrCreate(NewActionOptions.class).generatePdf = true;
                        if(a.getStringValue().isPresent()) {
                            options.getOrCreate(NewActionOptions.class).generatePdfOutput=NPath.of(a.stringValue());
                        }
                    })
                    .with("--generate-doc-pdf").matchFlag(a -> {
                        options.getOrCreate(GenerateActionOptions.class).outputFormat=OutputFormat.PDF;
                        options.getOrCreate(GenerateActionOptions.class).paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
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
                        options.getOrCreate(ViewFrameActionOptions.class);
                        options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
                    })
                    .withNonOption().matchAny(a -> options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of(a.image())))
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

    private void continueParsingOpen(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .withNonOption().matchAny(a ->  options.getOrCreate(OpenActionOptions.class).paths.add(NPath.of(a.image())))
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

    private void continueParsingView(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .withCondition(c -> {
                        NArg a = c.peek().get();
                        return a.isOption() && a.key().startsWith("--var-");
                    }).matchEntry(a -> {
                        options.vars.put(a.key().substring("--var-".length()), a.stringValue());
                    })
                    .withNonOption().matchAny(a ->  options.getOrCreate(ViewFrameActionOptions.class).paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

}
