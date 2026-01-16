/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.api.engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.NTxDocumentLoadingResult;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.eval.NTxCompilePageContext;
import net.thevpc.ntexup.api.eval.NTxVarProvider;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.nuts.core.NMutableClassLoader;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
public interface NTxEngine {
    String CURRENT_VERSION = "0.8.9.0";
    String FILE_EXT = "ntx";
    String FILE_DOT_EXT = ".ntx";

    NTxLogger log();

    List<NTxImageTypeRendererFactory> imageTypeRendererFactories();

    boolean importDefaultDependencies();

    boolean importDependencies(String... deps);

    NMutableClassLoader getEngineClassLoader();

    <S> List<S> loadServices(Class<S> serviceClass);

    void addDependencyLoadedListener(NTxDependencyLoadedListener listener);

    void dump();

    void dump(Consumer<NMsg> out);

    NTxEngineTools tools();

    NTxLogger addLog(NTxLogger messages);

    NTxLogger removeLog(NTxLogger messages);

    List<NTxNodeParser> nodeTypeFactories();

    NOptional<NTxFunction> findFunction(NTxItem node, String name, NTxFunctionArg... args);

    NTxNode newDefaultNode(String id);

    NOptional<NTxNodeParser> nodeTypeParser(String id);

    NTxDocumentFactory documentFactory();

    NOptional<NTxItem> newNode(NElement element, NTxNodeFactoryParseContext ctx);

    NOptional<NTxDocumentStreamRenderer> newStreamRenderer(String type);

    NOptional<NTxDocumentStreamRenderer> newPdfRenderer();

    NOptional<NTxDocumentStreamRenderer> newHtmlRenderer();

    NOptional<NTxDocumentScreenRenderer> newScreenRenderer();

    NOptional<NTxDocumentRenderer> newRenderer(String type);

    NTxDocumentLoadingResult compileDocument(NTxDocument document);

    List<NTxNode> compilePageNode(NTxNode node, NTxDocument document);

    List<NTxNode> compilePageNode(NTxNode node, NTxCompilePageContext context);

    List<NTxNode> compileItem(NTxItem node, NTxCompilePageContext context);

    boolean validateNode(NTxNode node);

    NTxDocumentLoadingResult loadDocument(NPath of);

    NTxCompiledDocument loadCompiledDocument(NPath of);

    NTxCompiledDocument asCompiledDocument(NTxDocument of);

    NOptional<NTxItem> loadNode(NTxNode into, NPath of, NTxDocument document);

    NTxDocumentLoadingResult loadDocument(InputStream is);

    NElement toElement(NTxDocument doc);

    NElement toElement(NTxNode node);

    NOptional<NTxProp> computeProperty(NTxNode node, String... propertyNames);

    List<NTxStyleRule> computeStyles(NTxNode node);

    List<NTxStyleRule> computeDeclaredStyles(NTxNode node);

    Set<String> computeDeclaredStylesClasses(NTxNode node);

    List<NTxProp> computeInheritedProperties(NTxNode node);

    List<NTxProp> computeProperties(NTxNode node);

    <T> NOptional<T> computePropertyValue(NTxNode node, String... propertyNames);

    NTxGraphics createGraphics(Graphics2D g2d);

    boolean isNtxProject(NPath path);

    void createProject(NPath path, NPath templateUrl, Function<String, String> vars);

    NTxTemplateInfo[] getTemplates();

    NOptional<NElement> evalExpression(NElement expression, NTxNode node, NTxVarProvider varProvider);

    NOptional<NElement> resolveVarValue(String varName, NTxNode node);
    NOptional<NElement> resolveVarValue(String varName, NTxNode node, NTxVarProvider varProvider);

    NOptional<NTxVar> findVar(String varName, NTxNode node, NTxVarProvider varProvider);

    NOptional<NTxNode> findNodeByProperty(String varName, String varValue, NTxNode node, NTxVarProvider varProvider);

    NPath resolvePath(NElement path, NTxNode node);

    NOptional<NTxTextRendererFlavor> textRendererFlavor(String id);

    List<NTxTextRendererFlavor> textRendererFlavors();

    BufferedImage renderImage(NTxCompiledPage page, NTxNodeRendererConfig config);

    byte[] renderImageBytes(NTxCompiledPage page, NTxNodeRendererConfig config);

    NOptional<NTxNodeRenderer> getRenderer(String type);


    <T> NOptional<T> getEnv(String name);

    <T> NOptional<T> computeIfAbsent(String name, Function<String, T> fct);

    NTxEngine setEnv(String env, Object value);
}
