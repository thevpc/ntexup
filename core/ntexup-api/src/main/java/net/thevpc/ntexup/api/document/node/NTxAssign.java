package net.thevpc.ntexup.api.document.node;

/**
 * Helpers for {@code assign}/{@code defaultassign} nodes.
 * <p>
 * The compiler keeps assignment nodes inside compiled pages so that the render
 * context can be seeded with the values of the variables used by the page. Such
 * compiler-generated assignments are <b>static</b>: their value was already
 * evaluated at compile time and cannot change between two render passes. They
 * are marked with {@link #STATIC_USER_KEY} so the assign renderer can skip the
 * dependency-graph notification (which would otherwise invalidate the page
 * currently being rendered, producing an endless render loop).
 */
public final class NTxAssign {

    public static final String STATIC_USER_KEY = "ntexup.assign.static";

    private NTxAssign() {
    }

    public static NTxNode markStatic(NTxNode node) {
        if (node != null) {
            node.setUserObject(STATIC_USER_KEY, Boolean.TRUE);
        }
        return node;
    }

    public static boolean isStatic(NTxNode node) {
        return node != null && Boolean.TRUE.equals(node.getUserObject(STATIC_USER_KEY).orNull());
    }
}
