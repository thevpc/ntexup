package net.thevpc.ntexup.cmdline;

import com.formdev.flatlaf.FlatLightLaf;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxTemplateFilter;
import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.cmdline.options.*;
import net.thevpc.ntexup.engine.repo.RepoBuilderTool;
import net.thevpc.ntexup.main.MainFrame;
import net.thevpc.ntexup.util.NTexupUtils;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.io.NAsk;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.swing.NSwingUtils;
import net.thevpc.nuts.text.*;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.util.NValidationException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathRenameOptions;
import net.thevpc.nuts.util.*;

import java.util.*;
import java.util.stream.Collectors;

public class NTexupOptionsProcessor {

    private static class Info {
        Options options;
        Set<Action> visitedOptions;
        NTxEngine engine;
        MainFrame mainFrame;
    }

    public void process(Options options, NTxEngine engine) {

        Info info = new Info();
        info.options = options;
        info.visitedOptions = new HashSet<>();
        info.engine = engine;
        LinkedHashMap<Action, ActionOptions> optionsMap = options.actionOptions.stream().collect(Collectors.toMap(x -> x.action, x -> x, (v1, v2) -> v2, LinkedHashMap::new));

        if (optionsMap.remove(Action.DUMP) != null) {
            if (info.visitedOptions.add(Action.DUMP)) {
                engine.dump();
            }
        }
        if (optionsMap.remove(Action.LIST_TEMPLATES) != null) {
            if (info.visitedOptions.add(Action.LIST_TEMPLATES)) {
                runActionListTemplates(options, engine);
            }
        }
        if (optionsMap.remove(Action.BUILD_REPO) != null) {
            if (info.visitedOptions.add(Action.BUILD_REPO)) {
                runActionBuildRepo(options, engine);
            }
        }
        for (Map.Entry<Action, ActionOptions> a : new LinkedHashMap<>(optionsMap).entrySet()) {
            switch (a.getKey()) {
                case DUMP:
                case LIST_TEMPLATES:
                case BUILD_REPO: {
                    //wil be handled later!
                    break;
                }
                case SHOW_FRAME: {
                    optionsMap.remove(a.getKey());
                    runActionShowFrame(info);
                    break;
                }
                case NEW: {
                    optionsMap.remove(a.getKey());
                    runActionNew(info);
                    break;
                }
                case SHOW_DOCUMENT: {
                    optionsMap.remove(a.getKey());
                    runActionOpenInViewer(info);
                    break;
                }
                case GENERATE: {
                    optionsMap.remove(a.getKey());
                    runActionGenerate(info);
                    break;
                }
                case EDITOR: {
                    EditorActionOptions v = (EditorActionOptions) a.getValue();
                    new NEditorSyntaxInstaller(
                            v.getSyntaxInfo()
                                    .copy()
                                    .setRepoFolder("https://github.com/thevpc/ntexup/raw/refs/heads/main/documentation/integration")
                                    .setLangId("ntexup")
                    ).run();
                    break;
                }
            }
        }
    }

    private void runActionShowFrame(Info info) {
        if (info.mainFrame == null) {
            NSwingUtils.setSharedWorkspaceInstance();
            FlatLightLaf.setup(new com.formdev.flatlaf.FlatDarculaLaf());
            info.mainFrame = new MainFrame(info.engine);
            NTexupUtils.runUiAsync(() -> {
                info.mainFrame.setVisible(true);
            });
        }
        ShowFrameActionOptions showFrameActionOptions = info.options.getOrCreate(ShowFrameActionOptions.class);
        if (showFrameActionOptions != null && showFrameActionOptions.viewLog) {
            info.mainFrame.getService().showDebug();
        }
    }

    private void runActionListTemplates(Options options, NTxEngine engine) {
        NTxTemplateInfo[] templates = engine.getTemplates();
        if (NSession.of().isPlainOut()) {
            int idLayoutWidth = Math.max(Arrays.stream(templates).mapToInt(x -> x.id().length()).max().orElse(0), 3);
            int nameLayoutWidth = Math.max(Arrays.stream(templates).mapToInt(x -> NStringUtils.trim(x.name()).length()).max().orElse(0), 3);
            for (NTxTemplateInfo template : templates) {
                NId id = NId.get(template.id()).orNull();
                if (id != null) {
                    NOut.println(NMsg.ofC("%-" + idLayoutWidth + "s %-" + nameLayoutWidth + "s %-3s %s",
                            id,
                            NMsg.ofStyledPrimary3(NStringUtils.trim(template.name())),
                            template.recommended() ? NMsg.ofStyledError(" (*)") : "",
                            NMsg.ofStyledPath(template.url())
                    ));
                } else {
                    NOut.println(NMsg.ofC("%-" + idLayoutWidth + "s %-" + nameLayoutWidth + "s %-3s %s",
                            NMsg.ofStyledPrimary1(template.id()),
                            NMsg.ofStyledPrimary3(NStringUtils.trim(template.name())),
                            template.recommended() ? NMsg.ofStyledError(" (*)") : "",
                            NMsg.ofStyledPath(template.url())
                    ));
                }
            }
        } else {
            NOut.println(templates);
        }
    }

    private void runActionBuildRepo(Options options, NTxEngine engine) {
        RepoBuilderTool tool = new RepoBuilderTool(engine.log());
        List<NPath> paths = new ArrayList<>(options.get(BuildRepoActionOptions.class).paths);
        if (paths.isEmpty()) {
            paths.add(NPath.of("."));
        }
        for (NPath path : paths) {
            if (tool.buildRepository(path)) {
                engine.log().log(NMsg.ofC("repository built successfully at %s", path));
            }
        }
    }

    private void runActionGenerate(List<NPath> paths, NPath expecteOutput, Info info) {
        if (expecteOutput == null) {
            expecteOutput = NPath.of(".");
        }
        for (NPath path : paths) {
            NChronometer ch = NChronometer.startNow();
            NTxCompiledDocument doc = info.engine.loadCompiledDocument(path);
            if (doc.pages().isEmpty()) {
                info.engine.log().log(NMsg.ofC("no pages to render : %s", path.normalize().toAbsolute()).asError());
                return;
            }
            NTxDocumentStreamRendererConfig renderConfig = new NTxDocumentStreamRendererConfig();
            NTxDocumentStreamRenderer renderer = info.engine.newPdfRenderer().get();
            renderer.setStreamRendererConfig(renderConfig);
            NPath output = null;
            if (paths.size() == 1) {
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
            ch.stop();
            info.engine.log().log(NMsg.ofC("generated : %s (%s) in %s", output.normalize().toAbsolute(), NMemorySizeFormat.DEFAULT.format(NMemorySize.ofBytes(output.getContentLength()).normalize()), ch).asInfo().withDurationMillis(ch.getDurationMs()));

        }
    }

    private void runActionGenerate(Info info) {
        List<NPath> paths = new ArrayList<>();

        NewActionOptions newActionOptions = info.options.get(NewActionOptions.class);
        if (newActionOptions != null) {
            paths.addAll(newActionOptions.paths);
        }
        if (info.options.get(ShowActionOptions.class) != null) {
            paths.addAll(info.options.get(ShowActionOptions.class).paths);
        }
        paths.addAll(info.options.get(GenerateActionOptions.class).paths);

        if (paths.isEmpty()) {
            paths.add(NPath.ofUserDirectory());
        }
        NPath expecteOutput = info.options.get(GenerateActionOptions.class).output;
        if (expecteOutput == null) {
            expecteOutput = NPath.of(".");
        }
        runActionGenerate(paths, expecteOutput, info);
    }

    private void runActionNew(Info info) {
        if (info.mainFrame == null) {
            String templateUrl = info.options.get(NewActionOptions.class).templateUrl;
            if (info.options.get(NewActionOptions.class).paths.isEmpty()) {
                info.options.get(NewActionOptions.class).paths.add(NPath.of("."));
            }
            for (NPath f : info.options.get(NewActionOptions.class).paths) {
                if (info.engine.isNtxProject(f)) {
                    throw new NIllegalArgumentException(NMsg.ofC("unable to create ntexup project. Project file or folder already exists at %s", f.normalize().toAbsolute()));
                }
            }
            if (NBlankable.isBlank(templateUrl)) {
                NTxTemplateInfo[] templates = info.engine.getTemplates();
                NTextBuilder sb = NTextBuilder.of();
                sb.append("Enter template url. You can choose from the following :").newLine();
                for (int i = 0; i < templates.length; i++) {
                    NTxTemplateInfo template = templates[i];
                    sb.append(NMsg.ofC("[%-3s] %s %-25s : %s", NText.ofStyled("#" + (i + 1), NTextStyle.number()),
                                    template.recommended() ? NMsg.ofStyledError("*") : " ",
                                    NMsg.ofStyledPrimary1(template.id()), NMsg.ofStyledPath(template.url())))
                            .newLine();
                }
                String value = NAsk.of().forString(NMsg.ofC("%s", sb))
                        .setValidator((sval, a) -> {
                            return NTxTemplateFilter.of(templates).selectOneUrl(sval);
                        })
                        .getValue();
                if (value == null) {
                    throw new NValidationException(NMsg.ofC("Invalid template url: %s", value));
                }
                templateUrl = value;
            } else {
                templateUrl = NTxTemplateFilter.of(() -> info.engine.getTemplates()).selectOneUrl(templateUrl);
            }
            for (NPath f : info.options.get(NewActionOptions.class).paths) {
                info.engine.createProject(f, NBlankable.isBlank(templateUrl) ? null : NPath.of(templateUrl), x -> info.options.vars.get(x));
            }
        } else {
            if (info.options.get(NewActionOptions.class).paths.isEmpty()) {
                info.mainFrame.getService().showNewProject(null);
            } else {
                for (NPath path : info.options.get(NewActionOptions.class).paths) {
                    info.mainFrame.getService().showNewProject(path.toString());
                }
            }
        }
        if (info.options.get(NewActionOptions.class).openViewer) {
            if (info.mainFrame == null) {
                runActionShowFrame(info);
            }
            for (NPath path : info.options.get(NewActionOptions.class).paths) {
                info.mainFrame.getService().openProject(path);
            }
        }
        if (info.options.get(NewActionOptions.class).generatePdf) {
            runActionGenerate(info.options.get(NewActionOptions.class).paths, null, info);
        }
    }


    private void runActionOpenInViewer(Info info) {
        List<NPath> paths = new ArrayList<>();
        if (info.mainFrame == null) {
            runActionShowFrame(info);
        }

        if (info.options.get(ShowActionOptions.class) != null) {
            paths.addAll(info.options.get(ShowActionOptions.class).paths);
        }

        if (paths.isEmpty()) {
            if (info.options.get(ShowFrameActionOptions.class).ifNoProjectViewCurrentDirectory) {
                paths.add(NPath.ofUserDirectory());
            }
        }

        for (NPath path : paths) {
            info.mainFrame.getService().openProject(path);
        }
    }


}
