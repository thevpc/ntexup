package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.repo.RepoBuilderTool;
import net.thevpc.nuts.NOut;
import net.thevpc.nuts.NSession;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathRenameOptions;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NMsg;

public class NTxTerminalProcessor {

    public void parse(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--reopen").matchTrueFlag(a -> {
                        options.reopen = true;
                        continueParsingReopen(cmdLine, options);
                    })
                    .with("--build-repo").matchTrueFlag(a -> {
                        continueParsingBuildRepository(cmdLine, options);
                    })
                    .with("--list-templates").matchFlag(a -> {
                        continueParsingListTemplates(cmdLine, options);
                    })
                    .with("--dump").matchFlag(a -> options.dump = true)
                    .with("--documentation").matchFlag(a -> options.documentation = true)
                    .with("--output").matchEntry(a -> options.output = NPath.of(a.stringValue()))
                    .with("--open").matchEntry(a -> {
                        options.paths.add(NPath.of(a.stringValue()));
                        continueParsingOpen(cmdLine, options);
                    })
                    .with("--pdf").matchFlag(a -> {
                        continueParsingPdf(cmdLine, options);
                    })
                    .with("--new").matchTrueFlag(a -> {
                        continueParsingNew(cmdLine, options);
                    })
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
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
                    .with("--dump").matchFlag(a -> options.dump = true)
                    .with("--documentation").matchFlag(a -> options.documentation = true)
                    .with("--output").matchEntry(a -> options.output = NPath.of(a.stringValue()))
                    .with("--open").matchEntry(a -> {
                        options.paths.add(NPath.of(a.stringValue()));
                        continueParsingOpen(cmdLine, options);
                    })
                    .with("--pdf").matchEntry(a -> {
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
                    .with("--dump").matchFlag(a -> options.dump = true)
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingListTemplates(NCmdLine cmdLine, Options options) {
        options.action = net.thevpc.ntexup.cmdline.Action.LIST_TEMPLATES;
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.dump = true)
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingOpen(NCmdLine cmdLine, Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--dump").matchFlag(a -> options.dump = true)
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
    }

    private void continueParsingPdf(NCmdLine cmdLine, Options options) {
        options.outputFormat = OutputFormat.PDF;
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--output").matchEntry(a -> options.output = NPath.of(a.stringValue()))
                    .with("--dump").matchFlag(a -> options.dump = true)
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

    public void runTerminal(NCmdLine cmdLine, Options options) {
        parse(cmdLine, options);
        if (!options.viewer && !options.console) {
            options.viewer = NSession.of().isGui();
            options.console = !options.viewer;
        } else if (options.viewer && options.console) {
            options.viewer = NSession.of().isGui();
            options.console = !options.viewer;
        }

        switch (options.action) {
            case NEW: {
                NTxEngine engine = new DefaultNTxEngine();
                if (options.dump) {
                    engine.dump();
                }
                if (NBlankable.isBlank(options.templateUrl)) {
                    cmdLine.throwMissingArgument("--template");
                }
                if (NBlankable.isBlank(options.paths.size() > 1)) {
                    cmdLine.throwError(NMsg.ofC("too many paths"));
                }
                if (options.paths.isEmpty()) {
                    options.paths.add(NPath.of(".").resolve(NPath.of(options.templateUrl).getName()));
                }
                NPath f = options.paths.get(0);
                engine.createProject(f, NPath.of(options.templateUrl), x -> options.vars.get(x));
                break;
            }
            case LIST_TEMPLATES: {
                NTxEngine engine = new DefaultNTxEngine();
                if (options.dump) {
                    engine.dump();
                }
                if (NSession.of().isPlainOut()) {
                    for (NTxTemplateInfo template : engine.getTemplates()) {
                        NOut.println(NMsg.ofC("%s (%s @ %s) %s",
                                NMsg.ofStyledPath(template.url()),
                                NMsg.ofStyledPrimary1(template.name()),
                                NMsg.ofStyledPrimary2(template.repoName()),
                                template.recommended() ? NMsg.ofStyledError(" (*)") : ""
                        ));
                    }
                } else {
                    NOut.println(engine.getTemplates());
                }
                break;
            }
            case BUILD_REPO: {
                NTxEngine engine = new DefaultNTxEngine();
                if (options.dump) {
                    engine.dump();
                }
                RepoBuilderTool tool = new RepoBuilderTool(engine.log());
                if (options.paths.isEmpty()) {
                    options.paths.add(NPath.of("."));
                }
                for (NPath path : options.paths) {
                    if (tool.buildRepository(path)) {
                        engine.log().log(NMsg.ofC("repository built successfully at %s", path));
                    }
                }
                break;
            }
            case OPEN: {
                boolean someDump = false;
                if (options.paths.isEmpty()) {
                    options.paths.add(NPath.of("."));
                }
                NPath expecteOutput = options.output;
                if (expecteOutput == null) {
                    expecteOutput = NPath.of(".");
                }
                for (NPath path : options.paths) {
                    NTxEngine engine = new DefaultNTxEngine();
                    if (options.dump && !someDump) {
                        engine.dump();
                        someDump = true;
                    }
//                    engine.importDefaultDependencies();
                    NTxCompiledDocument doc = engine.loadCompiledDocument(path);
                    NTxDocumentStreamRendererConfig renderConfig = new NTxDocumentStreamRendererConfig();
                    NTxDocumentStreamRenderer renderer = engine.newPdfRenderer().get();
                    renderer.setStreamRendererConfig(renderConfig);
                    NPath output = null;
                    if (options.paths.size() == 1) {
                        if (expecteOutput.isDirectory()) {
                            output = expecteOutput.resolve(path.getName()).resolveSibling(NPathRenameOptions.ofExtension("pdf"));
                        } else {
                            output = expecteOutput.resolveSibling(NPathRenameOptions.ofExtension("pdf"));
                        }
                    } else {
                        output = expecteOutput.resolve(path.getName()).resolveSibling(NPathRenameOptions.ofExtension("pdf"));
                    }
                    renderer.setOutput(output);
                    renderer.render(doc);
                }
                break;
            }
        }
    }

}
