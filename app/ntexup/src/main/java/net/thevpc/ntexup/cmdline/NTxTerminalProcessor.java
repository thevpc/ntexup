package net.thevpc.ntexup.cmdline;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.repo.RepoBuilderTool;
import net.thevpc.nuts.NCmdLineException;
import net.thevpc.nuts.NOut;
import net.thevpc.nuts.NSession;
import net.thevpc.nuts.NValidationException;
import net.thevpc.nuts.boot.NBootException;
import net.thevpc.nuts.boot.reserved.util.NBootMsg;
import net.thevpc.nuts.cmdline.NArg;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathRenameOptions;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.text.NTextBuilder;
import net.thevpc.nuts.text.NTextStyle;
import net.thevpc.nuts.util.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NTxTerminalProcessor {

    public void runTerminal(Options options) {


        switch (options.action) {
            case NEW: {
                NTxEngine engine = new DefaultNTxEngine();
                if (options.dump) {
                    engine.dump();
                }
                if (NBlankable.isBlank(options.templateUrl)) {
                    NTextBuilder sb = NTextBuilder.of();
                    sb.append("Enter template url. You can choose from the following :").newLine();
                    NTxTemplateInfo[] templates = engine.getTemplates();
                    for (int i = 0; i < templates.length; i++) {
                        NTxTemplateInfo template = templates[i];
                        sb.append(NMsg.ofC("[%-3s] %-25s : %s",NText.ofStyled("#"+(i+1),NTextStyle.number()),NMsg.ofStyledPrimary1(template.name()),NMsg.ofStyledPath(template.url())))
                        .newLine();
                    }
                    String value = NAsk.of().forString(NMsg.ofC("%s", sb))
                            .setValidator((sval,a)->{
                                NTxTemplateInfo ok=null;
                                List<NTxTemplateInfo> found = Arrays.stream(templates).filter(x -> x.url().equals(sval)).collect(Collectors.toList());
                                if(!found.isEmpty()){
                                    ok = found.get(0);
                                }else{
                                    found = Arrays.stream(templates).filter(x -> NStringUtils.trim(x.name()).equalsIgnoreCase(NStringUtils.trim(sval))).collect(Collectors.toList());
                                    if(!found.isEmpty()) {
                                        ok = found.get(0);
                                    }else{
                                        if(sval.startsWith("#")){
                                            NOptional<Integer> z = NLiteral.of(sval.substring(1).trim()).asInt();
                                            if(z.isPresent()){
                                                int zi = z.get();
                                                if(zi>=1 && zi<=templates.length){
                                                    ok=templates[zi-1];
                                                }
                                            }
                                        }
                                        if(ok==null){
                                            NOptional<Integer> z = NLiteral.of(sval.trim()).asInt();
                                            if(z.isPresent()){
                                                int zi = z.get();
                                                if(zi>=1 && zi<=templates.length){
                                                    ok=templates[zi-1];
                                                }
                                            }
                                        }
                                        if(ok==null){
                                            found = Arrays.stream(templates).filter(x -> NStringUtils.trim(x.name()).contains(NStringUtils.trim(sval))).collect(Collectors.toList());
                                            if(found.size()==1){
                                                ok=found.get(0);
                                            }else if(found.size()>1){
                                                throw new NValidationException(NMsg.ofC("ambiguous selection matches : %s", found.stream().map(Object::toString).collect(Collectors.joining(", "))));
                                            }
                                        }
                                    }
                                }
                                if(ok!=null){
                                    return ok.url();
                                }
                                throw new NValidationException(NMsg.ofC("Invalid template url: %s", sval));
                            })
                            .getValue();
                    if(value==null){
                        throw new NValidationException(NMsg.ofC("Invalid template url: %s", value));
                    }
                    options.templateUrl=value;
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
