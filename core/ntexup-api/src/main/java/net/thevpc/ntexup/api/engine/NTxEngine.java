/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.api.engine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.InputStream;
import java.security.KeyPair;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.parser.NTxItemParser;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.nuts.reflect.NMutableClassLoader;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

/**
 * @author vpc
 */
public interface NTxEngine {
    String CURRENT_VERSION = "1.0.0.0";
    String FILE_EXT = "ntx";
    String FILE_DOT_EXT = ".ntx";

    NTxLogger log();

    List<NTxImageTypeRendererFactory> imageTypeRendererFactories();

    ImportDependencyResults importDefaultDependencies();

    ImportDependencyResults importDependencies(String... deps);

    NMutableClassLoader getEngineClassLoader();

    <S> List<S> loadServices(Class<S> serviceClass);

    void addDependencyLoadedListener(NTxDependencyLoadedListener listener);

    void dump();

    void dump(Consumer<NMsg> out);

    NTxEngineTools tools();

    NTxLogger addLog(NTxLogger messages);

    NOptional<NTxFunction> findFunction(String name);

    NTxLogger removeLog(NTxLogger messages);

    List<NTxNodeParser> nodeTypeFactories();

    NTxItemParser itemParser();

//    NOptional<NTxFunction> findFunction(NTxItem node, String name, NTxFunctionArg... args);

    NTxNode newDefaultNode(String id);

    NOptional<NTxNodeParser> nodeTypeParser(String id);

    NTxDocumentFactory documentFactory();

    NTxEngine parseNode(NElement element, NTxResolutionContext ctx, Consumer<NOptional<NTxItem>> consumer);

    NOptional<NTxDocumentStreamRenderer> newStreamRenderer(String type);

    NOptional<NTxDocumentStreamRenderer> newPdfRenderer();

    NOptional<NTxDocumentStreamRenderer> newHtmlRenderer();

    NOptional<NTxDocumentScreenRenderer> newScreenRenderer();

    NOptional<NTxDocumentRenderer> newRenderer(String type);

    NTxResolutionContext newContext(NTxNode node, NTxDocument document, NTxCompiledDocument compiledDocument, NTxCompiledPage compiledPage, NTxResolutionContext parentContext);

    boolean validateNode(NTxNode node);

    NTxCompiledDocument loadDocument(NPath of);

    NTxCompiledDocument asCompiledDocument(NTxDocument of);

    NOptional<NTxItem> loadNode(NTxNode into, NPath of, NTxCompiledDocument document);

    NTxCompiledDocument loadDocument(InputStream is);

    NElement toElement(NTxDocument doc, boolean semantic);

    NElement toElement(NTxNode node, boolean semantic);

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

    NOptional<NTxTextRendererFlavor> textRendererFlavor(String id);

    List<NTxTextRendererFlavor> textRendererFlavors();

    BufferedImage renderImage(NTxCompiledPage page, NTxNodeRendererConfig config);

    void renderPage(NTxCompiledPage page, NTxNodeRendererConfig config,
                    Graphics2D g,
                    ImageObserver imageObserver, Runnable repainter
    );

    byte[] renderImageBytes(NTxCompiledPage page, NTxNodeRendererConfig config);

    NOptional<NTxNodeRenderer> getRenderer(String type);

    NTxEngine setAuthorKey(KeyPair authorPublicKey);

    <T> NOptional<T> getEnv(String name);

    <T> NOptional<T> computeIfAbsent(String name, Function<String, T> fct);

    List<NTxStyleRule> getDefaultStyles();

    NTxEngine setEnv(String env, Object value);


    void compileNode(NTxNode node, NTxDocument document, NTxCompiledDocument compiledDocument, NTxCompiledPage compiledPage, NTxResolutionContext context, CompileNodeVisitor visitor);

    void compileNode(NTxResolutionContext ctx, CompileNodeVisitor visitor);

    void defaultCompileNodeProperties(NTxNode node, NTxResolutionContext context);

    void defaultCompileNodeChildren(NTxNode node, NTxResolutionContext context);

    /**
     * Resolves a node within the document tree using an <b>Ancestral-First Radial Search</b>.
     * <p>
     * This resolution strategy is designed for document-centric architectures (NTexUp)
     * where visual and hierarchical proximity dictates logical relationships. It prioritizes
     * the direct lineage of the node before expanding to siblings and distant branches.
     * </p>
     * * <b>The Search Protocol:</b>
     * <ol>
     * <li><b>Phase 1: Ancestral Spine (The "Climb"):</b> Traverses vertically from the
     * {@code startNode} to the root. Only nodes on this direct path are checked.
     * This facilitates "Property Inheritance," where a parent can provide a
     * configuration override for its entire subtree.</li>
     * <li><b>Phase 2: Radial Expansion (The "Ripple"):</b> If the spine yields no results,
     * the search re-initiates from the {@code startNode} and moves upward. At each
     * level, it performs a Deep-Scan (DFS) of all sibling subtrees. Branches already
     * processed in previous steps are skipped to ensure each node is visited only once.</li>
     * </ol>
     * * <b>Performance & Soundness:</b>
     * <ul>
     * <li><b>Complexity:</b> O(N), where N is the total number of nodes in the document tree.</li>
     * <li><b>Determinism:</b> The search is deterministic and respects "Scoped Shadowing,"
     * ensuring that local definitions in the same branch are found before global ones
     * of the same name.</li>
     * <li><b>Fail-Never:</b> Returns a {@link NOptional#ofNamedEmpty(String)} if no match
     * is found, allowing the compiler to continue execution for logging or UI hints.</li>
     * </ul>
     * * @param propertyName        The property key to evaluate (e.g., "name" or "id").
     *
     * @param propertyValueFilter A predicate to validate the property value (e.g., matching a specific ID).
     * @return An {@link NOptional} containing the first matching {@link NTxNode},
     * or a named empty optional if the search is exhausted.
     */
    NOptional<NTxNode> findNodeByProperty(String propertyName, Predicate<NElement> propertyValueFilter, NTxResolutionContext context);

    NTxFunctionCallContext createFunctionArgs(String functionName, NElement[] callArgs, NTxResolutionContext context);

    NTxEngine setAuthorEmail(String authorEmail);

    String getAuthorName();

    String getAuthorEmail();

    NTxEngine setAuthorName(String authorName);

    String getAuthorUrl();
    String getAuthorOrcid();

    NTxEngine setAuthorUrl(String authorUrl);

    KeyPair getAuthorKey();
}
