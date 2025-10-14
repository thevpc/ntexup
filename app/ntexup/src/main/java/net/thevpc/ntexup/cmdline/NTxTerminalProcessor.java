package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxTemplateFilter;
import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.repo.RepoBuilderTool;
import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.io.NAsk;
import net.thevpc.nuts.io.NOut;
import net.thevpc.nuts.core.NSession;
import net.thevpc.nuts.text.*;
import net.thevpc.nuts.util.NValidationException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathRenameOptions;
import net.thevpc.nuts.util.*;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class NTxTerminalProcessor {

    public void runTerminal(Options options) {


        switch (options.action) {
            case NEW: {
                NTxEngine engine = new DefaultNTxEngine();
                if (options.dump) {
                    engine.dump();
                }
                NTxTemplateInfo[] templates = engine.getTemplates();
                if (NBlankable.isBlank(options.templateUrl)) {
                    NTextBuilder sb = NTextBuilder.of();
                    sb.append("Enter template url. You can choose from the following :").newLine();
                    for (int i = 0; i < templates.length; i++) {
                        NTxTemplateInfo template = templates[i];
                        sb.append(NMsg.ofC("[%-3s] %-25s : %s", NText.ofStyled("#" + (i + 1), NTextStyle.number()), NMsg.ofStyledPrimary1(template.id()), NMsg.ofStyledPath(template.url())))
                                .newLine();
                    }
                    String value = NAsk.of().forString(NMsg.ofC("%s", sb))
                            .setValidator((sval, a) -> {
                                NOptional<NTxTemplateInfo> u = NTxTemplateFilter.of(templates).selectOne(sval);
                                return u.get().url();
                            })
                            .getValue();
                    if (value == null) {
                        throw new NValidationException(NMsg.ofC("Invalid template url: %s", value));
                    }
                    options.templateUrl = value;
                } else {
                    options.templateUrl = NTxTemplateFilter.of(templates).selectOne(options.templateUrl).get().url();
                }
                if (options.paths.isEmpty()) {
                    options.paths.add(NPath.ofUserDirectory());
                }
                for (NPath f : options.paths) {
                    engine.createProject(f, NPath.of(options.templateUrl), x -> options.vars.get(x));
                }
                break;
            }
            case LIST_TEMPLATES: {
                NTxEngine engine = new DefaultNTxEngine();
                if (options.dump) {
                    engine.dump();
                }
                NTxTemplateInfo[] templates = engine.getTemplates();
                if (NSession.of().isPlainOut()) {
                    int idLayoutWidth = Math.max(Arrays.stream(templates).mapToInt(x -> x.id().length()).max().orElse(0),3);
                    int nameLayoutWidth = Math.max(Arrays.stream(templates).mapToInt(x -> NStringUtils.trim(x.name()).length()).max().orElse(0),3);
                    for (NTxTemplateInfo template : templates) {
                        NId id = NId.get(template.id()).orNull();
                        if(id!=null) {
                            NOut.println(NMsg.ofC("%-"+idLayoutWidth+"s %-"+nameLayoutWidth+"s %-3s %s",
                                    id,
                                    NMsg.ofStyledPrimary3(NStringUtils.trim(template.name())),
                                    template.recommended() ? NMsg.ofStyledError(" (*)") : "",
                                    NMsg.ofStyledPath(template.url())
                            ));
                        }else{
                            NOut.println(NMsg.ofC("%-"+idLayoutWidth+"s %-"+nameLayoutWidth+"s %-3s %s",
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
                    if (doc.pages().isEmpty()) {
                        engine.log().log(NMsg.ofC("no pages to render : %s", path.normalize().toAbsolute()).asError());
                        return;
                    }
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
