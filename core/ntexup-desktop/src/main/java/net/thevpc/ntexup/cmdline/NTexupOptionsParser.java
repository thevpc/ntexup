package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.cmdline.options.*;
import net.thevpc.ntexup.config.NTxViewerConfigManager;
import net.thevpc.nuts.platform.NSysEditorFamily;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NStringUtils;

import java.util.Arrays;

public class NTexupOptionsParser {
    public void parse(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            while (!cmdLine.isEmpty()) {
                cmdLine.matcher()
                        .when("show", "open").asArg(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class).ifNoProjectViewCurrentDirectory = true;
                            if (a.getStringValue().isPresent()) {
                                options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.stringValue()));
                            }
                            continueParsingShow(cmdLine, options);
                        })
                        .when("show-html").asArg(a -> {
                            options.getOrCreate(ShowHtmlActionOptions.class).html = true;
                            if (a.getStringValue().isPresent()) {
                                options.getOrCreate(ShowHtmlActionOptions.class).path=NPath.of(a.stringValue());
                            }
                            continueParsingShow(cmdLine, options);
                        })
                        .when("show-doc").asTrueFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class);
                            options.getOrCreate(ShowActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                            continueShowDoc(cmdLine, options);
                        })
                        .when("generate-doc").asTrueFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                            continueParsingGeneratePdfDoc(cmdLine, options);
                        })
                        .when("reopen").asTrueFlag(a -> {
                            NTxViewerConfigManager c = new NTxViewerConfigManager();
                            NPath p = c.getLatestProjectPath();
                            if (p != null) {
                                options.getOrCreate(ShowActionOptions.class).addPath(p);
                            }
                            continueParsingReopen(cmdLine, options);
                        })
                        .when("build-repo").asArg(a -> {
                            options.getOrCreate(BuildRepoActionOptions.class);
                            if (a.getStringValue().isPresent()) {
                                options.getOrCreate(BuildRepoActionOptions.class).addPath(NPath.of(a.stringValue()));
                            }
                            continueParsingBuildRepository(cmdLine, options);
                        })
                        .when("list-templates").asFlag(a -> {
                            options.getOrCreate(ListTemplatesActionOptions.class);
                            continueParsingListTemplates(cmdLine, options);
                        })
                        .when("generate", "pdf").asFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class);
                            continueParsingGeneratePdf(cmdLine, options);
                        })
                        .when("new").asTrueFlag(a -> {
                            options.getOrCreate(NewActionOptions.class);
                            continueParsingNew(cmdLine, options);
                        })
                        .when("--gui").asFlag(a -> {
                            options.getOrCreate(ShowFrameActionOptions.class);
                            NSession.of().gui(a.booleanValue());
                        })
                        .when("install-editor-syntax").asEntry(a -> {
                            EditorActionOptions w = options.getOrCreate(EditorActionOptions.class);
                            String s = NStringUtils.firstNonBlank(a.getStringValue().orNull(), "all");
                            if (NStringUtils.strip(s).equalsIgnoreCase("all")) {
                                w.getSyntaxInfo().addAll(Arrays.asList(NSysEditorFamily.values()));
                            } else {
                                w.getSyntaxInfo().addAll(NSysEditorFamily.parseSet(s).get());
                            }
                            while (!cmdLine.isEmpty()) {
                                cmdLine.matcher()
                                        .when("--force","-f").asFlag(aa -> {
                                            w.setForce(aa.booleanValue());
                                        })
                                        .whenNonOption().asArg(aa -> {
                                            String ss = NStringUtils.strip(aa.asString().orNull());
                                            if (NStringUtils.strip(ss).equalsIgnoreCase("all")) {
                                                w.getSyntaxInfo().addAll(Arrays.asList(NSysEditorFamily.values()));
                                            } else {
                                                w.getSyntaxInfo().addAll(NSysEditorFamily.parseSet(ss).get());
                                            }
                                        })
                                        .withDefaults()
                                        .require()
                                ;
                            }
                        })
                        .when("dump").asFlag(a -> {
                            options.getOrCreate(DumpDocumentOptions.class);
                        })
                        .whenArg(u -> {
                            if (!u.isOption() && u.isNonOption()) {
                                String m = u.image();
                                if (m.equals(".") || m.equals("..") || m.contains("/") || m.contains("\\")) {
                                    return true;
                                }
                            }
                            return false;
                        }).asArg(a -> {
                            options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.image()));
                        })
                        .withDefaults()
                        .require()
                ;
            }
        }
        if(options.isEmpty()){
            options.getOrCreate(ShowFrameActionOptions.class).ifNoProjectViewCurrentDirectory = true;
            options.getOrCreate(ShowActionOptions.class).addPath(NPath.ofUserDirectory());
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
                    .when("--dump").asFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .when("--show").asFlag(a -> {
                        options.getOrCreate(NewActionOptions.class).openViewer = true;
                    })
                    .when("--show-doc").asFlag(a -> {
                        options.getOrCreate(ShowFrameActionOptions.class);
                        options.getOrCreate(ShowActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                    })
                    .when("--generate-pdf").asFlag(a -> {
                        options.getOrCreate(NewActionOptions.class).generatePdf = true;
                        if (a.getStringValue().isPresent()) {
                            options.getOrCreate(NewActionOptions.class).generatePdfOutput = NPath.of(a.stringValue());
                        }
                    })
                    .when("--generate-doc-pdf").asFlag(a -> {
                        options.getOrCreate(GenerateActionOptions.class).outputFormat = OutputFormat.PDF;
                        options.getOrCreate(GenerateActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                        if (a.getStringValue().isPresent()) {
                            options.getOrCreate(GenerateActionOptions.class).output = NPath.of(a.stringValue());
                        }
                    })
                    .when("--template", "-t").asEntry(a -> options.getOrCreate(NewActionOptions.class).templateUrl = a.stringValue())
                    .whenNonOption().asArg(a -> options.getOrCreate(NewActionOptions.class).addPath(NPath.of(a.image())))
                    .withDefaults()
                    .require()
            ;
        }
    }

    private void continueParsingReopen(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .when("--dump").asFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .when("--show-doc").asFlag(a -> {
                        options.getOrCreate(ShowFrameActionOptions.class);
                        options.getOrCreate(ShowActionOptions.class).addPath(NPath.of("https://github.com/thevpc/ntexup-doc-slides.git"));
                    })
                    .whenNonOption().asArg(a -> options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.image())))
                    .withDefaults()
                    .require()
            ;
        }
    }

    private void continueParsingBuildRepository(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .when("--dump").asFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .whenNonOption().asArg(a -> options.getOrCreate(BuildRepoActionOptions.class).addPath(NPath.of(a.image())))
                    .withDefaults()
                    .require()
            ;
        }
    }

    private void continueParsingListTemplates(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .when("--dump").asFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .withDefaults()
                    .require()
            ;
        }
    }


    private void continueParsingGeneratePdfDoc(NCmdLine cmdLine, Options options) {
        continueParsingGeneratePdf(cmdLine, options);
    }

    private void continueParsingGeneratePdf(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .when("--output").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).output = NPath.of(a.stringValue());
                    })
                    .when("--dump").asFlag(a -> options.getOrCreate(DumpDocumentOptions.class))
                    .whenArg(a -> a.key().startsWith("--var-")).asEntry(a -> {
                        options.vars.put(a.key().substring("--var-".length()), a.stringValue());
                    })
                    .whenNonOption().asArg(a -> options.getOrCreate(GenerateActionOptions.class).addPath(NPath.of(a.image())))
                    .withDefaults()
                    .require()
            ;
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
                    .whenNonOption().asArg(a -> options.getOrCreate(ShowActionOptions.class).addPath(NPath.of(a.image())))
                    .withDefaults()
                    .require()
            ;
        }
        if (options.getOrCreate(ShowActionOptions.class).paths.isEmpty()) {
            options.getOrCreate(ShowActionOptions.class).addPath(NPath.ofUserDirectory());
        }
    }


}
