package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.cmdline.options.*;
import net.thevpc.ntexup.config.NTxViewerConfigManager;
import net.thevpc.nuts.platform.NSysEditorFamily;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.util.Arrays;
import java.util.Locale;

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
                        .when("image").asFlag(a -> {
                            options.getOrCreate(GenerateActionOptions.class).outputFormat = OutputFormat.IMAGE;
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
                    .when("--output", "-o").asEntry(a -> {
                        GenerateActionOptions g = options.getOrCreate(GenerateActionOptions.class);
                        String v = a.stringValue();
                        g.output = NPath.of(v);
                        g.outputDirectory = v.endsWith("/") || v.endsWith("\\");
                    })
                    .when("--pages", "-p").asEntry(a -> {
                        parsePages(a.stringValue(), options.getOrCreate(GenerateActionOptions.class));
                    })
                    .when("--dpi").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).dpi = parseIntEntry(a.stringValue(), "--dpi");
                    })
                    .when("--type", "--format").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).imageFormat = a.stringValue();
                    })
                    .when("--size").asEntry(a -> {
                        String s = a.stringValue();
                        String[] w = s.split("[xX]");
                        if (w.length == 2) {
                            options.getOrCreate(GenerateActionOptions.class).pageWidth = parseIntEntry(w[0], "--size");
                            options.getOrCreate(GenerateActionOptions.class).pageHeight = parseIntEntry(w[1], "--size");
                        }
                    })
                    .when("--page-width").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).pageWidth = parseIntEntry(a.stringValue(), "--page-width");
                    })
                    .when("--page-height").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).pageHeight = parseIntEntry(a.stringValue(), "--page-height");
                    })
                    .when("--page-size").asEntry(a -> {
                        int[] s = resolvePageSize(a.stringValue());
                        options.getOrCreate(GenerateActionOptions.class).pageWidth = s[0];
                        options.getOrCreate(GenerateActionOptions.class).pageHeight = s[1];
                    })
                    .when("--grid").asEntry(a -> {
                        String s = a.stringValue();
                        String[] w = s.split("[xX]");
                        if (w.length == 2) {
                            options.getOrCreate(GenerateActionOptions.class).gridX = parseIntEntry(w[0], "--grid");
                            options.getOrCreate(GenerateActionOptions.class).gridY = parseIntEntry(w[1], "--grid");
                        }
                    })
                    .when("--grid-x").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).gridX = parseIntEntry(a.stringValue(), "--grid-x");
                    })
                    .when("--grid-y").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).gridY = parseIntEntry(a.stringValue(), "--grid-y");
                    })
                    .when("--margin").asEntry(a -> {
                        float f = parseFloatEntry(a.stringValue(), "--margin");
                        GenerateActionOptions g = options.getOrCreate(GenerateActionOptions.class);
                        g.marginTop = f;
                        g.marginBottom = f;
                        g.marginLeft = f;
                        g.marginRight = f;
                    })
                    .when("--margin-top").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).marginTop = parseFloatEntry(a.stringValue(), "--margin-top");
                    })
                    .when("--margin-bottom").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).marginBottom = parseFloatEntry(a.stringValue(), "--margin-bottom");
                    })
                    .when("--margin-left").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).marginLeft = parseFloatEntry(a.stringValue(), "--margin-left");
                    })
                    .when("--margin-right").asEntry(a -> {
                        options.getOrCreate(GenerateActionOptions.class).marginRight = parseFloatEntry(a.stringValue(), "--margin-right");
                    })
                    .when("--landscape").asFlag(a -> {
                        options.getOrCreate(GenerateActionOptions.class).orientation = net.thevpc.ntexup.api.renderer.NTxPageOrientation.LANDSCAPE;
                    })
                    .when("--portrait").asFlag(a -> {
                        options.getOrCreate(GenerateActionOptions.class).orientation = net.thevpc.ntexup.api.renderer.NTxPageOrientation.PORTRAIT;
                    })
                    .when("--show-page-number").asTrueFlag(a -> {
                        options.getOrCreate(GenerateActionOptions.class).showPageNumber = true;
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

    private int[] resolvePageSize(String value) {
        String n = NStringUtils.strip(value).toLowerCase(Locale.ROOT);
        switch (n) {
            case "a0": return new int[]{2384, 3370};
            case "a1": return new int[]{1684, 2384};
            case "a2": return new int[]{1191, 1684};
            case "a3": return new int[]{842, 1191};
            case "a4": return new int[]{595, 842};
            case "a5": return new int[]{420, 595};
            case "a6": return new int[]{298, 420};
            case "b0": return new int[]{2835, 4008};
            case "b1": return new int[]{2004, 2835};
            case "b2": return new int[]{1417, 2004};
            case "b3": return new int[]{1001, 1417};
            case "b4": return new int[]{709, 1001};
            case "b5": return new int[]{499, 709};
            case "b6": return new int[]{354, 499};
            case "letter": return new int[]{612, 792};
            case "legal": return new int[]{612, 1008};
            case "ledger":
            case "tabloid": return new int[]{792, 1224};
            case "executive": return new int[]{522, 756};
            case "statement":
            case "halfletter": return new int[]{396, 612};
            case "folio": return new int[]{612, 936};
            default: {
                String[] parts = n.split("[xX]");
                if (parts.length == 2) {
                    NOptional<Integer> w = NLiteral.of(parts[0]).asInt();
                    NOptional<Integer> h = NLiteral.of(parts[1]).asInt();
                    if (w.isPresent() && h.isPresent()) {
                        return new int[]{w.get(), h.get()};
                    }
                }
                throw new IllegalArgumentException("unknown page size: " + value + " (supported: A0-A6, B0-B6, Letter, Legal, Ledger/Tabloid, Executive, Statement, Folio or WxH in points)");
            }
        }
    }

    private void parsePages(String value, GenerateActionOptions g) {
        for (String part : value.split(",")) {
            part = NStringUtils.strip(part);
            if (!part.isEmpty()) {
                int dash = part.indexOf('-');
                if (dash > 0) {
                    int a = parseIntEntry(part.substring(0, dash), "--pages");
                    int b = parseIntEntry(part.substring(dash + 1), "--pages");
                    for (int i = a; i <= b; i++) {
                        g.pages.add(i);
                    }
                } else {
                    g.pages.add(parseIntEntry(part, "--pages"));
                }
            }
        }
    }

    private int parseIntEntry(String value, String option) {
        return NLiteral.of(NStringUtils.strip(value)).asInt()
                .orElseThrow(() -> new IllegalArgumentException("invalid integer value for " + option + ": " + value));
    }

    private float parseFloatEntry(String value, String option) {
        return NLiteral.of(NStringUtils.strip(value)).asFloat()
                .orElseThrow(() -> new IllegalArgumentException("invalid number value for " + option + ": " + value));
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
