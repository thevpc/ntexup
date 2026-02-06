package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;

import java.util.*;

public class DefaultNTxNodeFactoryParseContext implements NTxNodeFactoryParseContext {
    private final NTxDocument document;
    private final NElement element;
    private final NTxEngine engine;
    private final List<NTxNode> nodePath = new ArrayList<>();
    private final NTxSource source;

    public DefaultNTxNodeFactoryParseContext(
            NTxDocument document
            , NElement element
            , NTxEngine engine
            ,
            List<NTxNode> nodePath
            , NTxSource source
    ) {
        this.document = document;
        this.element = element == null ? null : NTxUtils.addCompilerDeclarationPath(element, source);
        this.engine = engine;
        this.source = source;
        this.nodePath.addAll(nodePath);
    }

    private boolean isVarName(String name) {
        return name.matches("[a-zA-Z][a-zA-Z0-9_-]*]]");
    }

    @Override
    public NElement asPathRef(NElement element) {
        if (element.isAnyString()) {
            String pathString = element.asStringValue().get();
            if (isVarName(pathString)) {
                return element;
            }
            if (!pathString.trim().isEmpty()) {
                for (int i = nodePath.size() - 1; i >= 0; i--) {
                    NTxSource resource = NTxUtils.sourceOf(nodePath.get(i));
                    if (resource != null) {
                        if (resource.path().isPresent()) {
                            NPath nPath = NTxUtils.resolvePath(element, resource);
                            if (nPath != null) {
                                return NElement.ofString(nPath.toString());
                            }
                        }
                    }
                }
            }
        }
        return element;
    }

    @Override
    public NTxLogger messages() {
        return engine().log();
    }

    public NTxDocument document() {
        return document;
    }

    @Override
    public NTxNodeFactoryParseContext push(NTxNode node) {
        return push(node, null);
    }

    @Override
    public NTxNodeFactoryParseContext push(NElement element) {
        return push(null, element);
    }

    @Override
    public NTxNodeFactoryParseContext push(NTxNode node, NElement element) {
        if (node == null && element == null) {
            return this;
        }
        List<NTxNode> nodePath2 = new ArrayList<>();
        nodePath2.addAll(Arrays.asList(nodePath()));
        if (node != null) {
            nodePath2.add(node);
        }
        if (element == null) {
            element = this.element;
        }
        return new DefaultNTxNodeFactoryParseContext(document(), element, engine(), nodePath2, source);
    }

    @Override
    public NTxDocumentFactory documentFactory() {
        return engine().documentFactory();
    }

    public NTxSource source() {
        return source;
    }

    @Override
    public NTxNode node() {
        if (nodePath.isEmpty()) {
            return null;
        }
        return nodePath.get(nodePath.size() - 1);
    }

    @Override
    public NTxNode[] nodePath() {
        return nodePath.toArray(new NTxNode[0]);
    }

    @Override
    public NTxEngine engine() {
        return engine;
    }

    @Override
    public NElement element() {
        return element;
    }

//    @Override
//    public NPath resolvePath(NPath path) {
//        if (path.isAbsolute()) {
//            return path;
//        }
//        return resolvePath(path.toString());
//    }

//    @Override
//    public NPath resolvePath(NStringElement path) {
//        if(path==null){
//            return null;
//        }
//        String s = path.asStringValue().get();
//        if (NBlankable.isBlank(s)) {
//            return null;
//        }
//        return null;
//    }
//
//    @Override
//    public NPath resolvePath(String path) {
//        if (NBlankable.isBlank(path)) {
//            return null;
//        }
//        if (GitHelper.isGithubFolder(path)) {
//            return resolvePath(GitHelper.resolveGithubPath(path, messages()));
//        }
//        NTxResource src = source;
//        if (src == null) {
//            if (nodePath != null) {
//                for (int i = nodePath.size() - 1; i >= 0; i--) {
//                    if (nodePath.get(i) != null) {
//                        NTxResource ss = NTxUtils.sourceOf(nodePath.get(i));
//                        if (ss != null) {
//                            src = ss;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//        return NTxUtils.resolvePath(NElement.ofString(path),src);
//    }
}
