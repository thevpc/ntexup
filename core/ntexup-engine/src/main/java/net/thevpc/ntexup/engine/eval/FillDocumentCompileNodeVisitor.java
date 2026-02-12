package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.extension.NTxFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FillDocumentCompileNodeVisitor implements CompileNodeVisitor,AutoCloseable{
    private final NTxNode container; // The node we are currently filling    boolean someChanges = false;
    private final NTxEngine engine; // The node we are currently filling    boolean someChanges = false;
    List<NTxNode> pending = new ArrayList<>();

    public FillDocumentCompileNodeVisitor(NTxNode container, NTxEngine engine) {
        this.container = container;
        this.engine = engine;
    }

    @Override
    public void visitNode(NTxNode node, NTxResolutionContext context) {
        if (isGroup(container)) {
            if (isBoundaryNode(node)) {
                // An explicit page/group is a "Hard Boundary".
                // We must flush current pending items so they don't
                // get merged with or follow this explicit boundary.
                flushPending();
                container.add(node);
            } else {
                // Definitions, Assignments, and Text all go here.
                // We keep them together to preserve their relative order.
                pending.add(node);
            }
        } else {
            // If we are already INSIDE an explicit Page,
            // just add it. The user has chosen this scope.
            container.add(node);
        }
    }

    private void flushPending() {
        if (pending.isEmpty()) return;

        // We create ONE page for all content, but we keep
        // the Structural nodes as siblings to that page
        // so they are visible to the REST of the PageGroup.

        NTxNode autoPage = null;

        for (NTxNode n : pending) {
            if (isStructuralNode(n)) {
                // Elevate to the Group level so next pages can see it
                container.add(n);
            } else {
                if (autoPage == null) {
                    autoPage = engine.documentFactory().of(NTxNodeType.PAGE);
                    autoPage.setSource(container.source());
                }
                autoPage.add(n);
            }
        }

        if (autoPage != null) {
            container.add(autoPage);
        }
        pending.clear();
    }

    /**
     * This must be called when the recursive 'compileNodeTree'
     * using THIS visitor instance finishes.
     */
    public void close() {
        flushPending();
    }


    private boolean isBoundaryNode(NTxNode node) {
        String type = node.type();
        return Objects.equals(type, NTxNodeType.PAGE) ||
                Objects.equals(type, NTxNodeType.PAGE_GROUP)
                ;
    }

    @Override
    public void visitRule(NTxStyleRule a, NTxResolutionContext context) {
        container.addRule(a);
    }

    @Override
    public void visitDefinition(NTxNodeDef a, NTxResolutionContext context) {
        container.add(a);
    }

    @Override
    public void visitFunction(NTxFunction a, NTxResolutionContext context) {
        throw new IllegalArgumentException("unsupported");
    }

    @Override
    public void visitProperty(NTxProp a, NTxResolutionContext context) {
        container.setProperty(a);
    }

    private boolean isGroup(NTxNode node) {
        String type = node.type();
        // PAGE_GROUP is your structural container (the "book").
        // GROUP is a logical layout container that might also hold pages.
        return Objects.equals(type, NTxNodeType.PAGE_GROUP) ||
                Objects.equals(type, NTxNodeType.GROUP);
    }

    private boolean isStructuralNode(NTxNode node) {
        String type = node.type();
        return Objects.equals(type, NTxNodeType.CTRL_ASSIGN) ||
                Objects.equals(type, NTxNodeType.CTRL_DEFINE) ||
                Objects.equals(type, NTxNodeType.CTRL_IMPORT) ||
                Objects.equals(type, NTxNodeType.CTRL_INCLUDE) ||
                // Also catch actual definition objects if your
                // compiler has already transformed them
                node instanceof NTxNodeDef;
    }
}
