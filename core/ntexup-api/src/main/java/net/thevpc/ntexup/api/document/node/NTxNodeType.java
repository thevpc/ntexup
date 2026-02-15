package net.thevpc.ntexup.api.document.node;

public class NTxNodeType {
    public static final String PAGE_GROUP="page-group";
    public static final String PAGE="page";

    /**
     * A transparent aggregator that provides no logical isolation.
     * * <p>Fragments are purely organizational. They do not create a new
     * {@code NTxResolutionContext}; instead, they operate directly on the
     * parent's scope. Upon compilation, the fragment "dissolves," and its
     * children are inflated directly into the parent container.</p>
     * * <b>Behavior:</b> No Context / Inflates into Parent.
     */
    public static final String FRAGMENT ="fragment";

    /**
     * A structural unit that provides both logical and layout isolation.
     * * <p>Groups create a new child {@code NTxResolutionContext}, protecting
     * the parent scope from internal assignments. Unlike blocks or fragments,
     * a group is a permanent node in the render tree, often serving as a
     * coordinate system or layout container for its children.</p>
     * * <b>Behavior:</b> Creates Context / Retained as Unit.
     */
    public static final String GROUP ="group";

    /**
     * A logical scope boundary that is transparent to the render tree.
     * * <p>Blocks are used for lexical isolation. They create a new child
     * {@code NTxResolutionContext} for internal logic and variable safety,
     * but the block node itself is removed during compilation. Its
     * processed children are then inflated into the parent container.</p>
     * * <b>Behavior:</b> Creates Context / Inflates into Parent.
     */
    public static final String BLOCK ="block";

    public static final String FLOW="flow";
    public static final String GRID="grid";
    public static final String ROW="row";
    public static final String COLUMN="column";
    public static final String PLAIN ="plain";
    public static final String RECTANGLE="rectangle";
    public static final String SPHERE="sphere";
    public static final String ELLIPSOID="ellipsoid";
    public static final String SQUARE="square";
    public static final String ORDERED_LIST="ordered-list";
    public static final String UNORDERED_LIST="unordered-list";
    public static final String POLYGON="polygon";
    public static final String POLYLINE="polyline";
    public static final String SCENE3D="scene3d";
    public static final String LINE="line";
    public static final String QUAD_CURVE="quad-curve";
    public static final String CUBIC_CURVE="cubic-curve";
    public static final String ARC="arc";
    public static final String IMAGE="image";
    public static final String EQUATION="eq";
    public static final String TEXT ="text";
    public static final String ELLIPSE="ellipse";
    public static final String CIRCLE="circle";
    public static final String TRIANGLE="triangle";
    public static final String PENTAGON="pentagon";
    public static final String HEXAGON="hexagon";
    public static final String HEPTAGON="heptagon";
    public static final String OCTAGON="octagon";
    public static final String NONAGON="nonagon";
    public static final String DECAGON="decagon";
    public static final String RHOMBUS="rhombus";
    public static final String TRAPEZOID="trapezoid";
    public static final String PARALLELOGRAM="parallelogram";
    public static final String FILLER="filler";
    public static final String VOID="void";
    public static final String CYLINDER="cylinder";
    public static final String SOURCE="source";
    public static final String NTF="ntf";
    public static final String UNNAMED="unnamed";
    public static final String DONUT="donut";
    public static final String PIE="pie";

    public static final String ARROW = "arrow";
    public static final String PLOT2D = "plot2d";

    public static final String CTRL_ASSIGN ="assign";
    public static final String CTRL_EXPR ="expr";
    public static final String CTRL_CALL ="call";
    public static final String CTRL_FOR ="for";
    public static final String CTRL_DEFINE ="define";
    public static final String CTRL_INCLUDE ="include";
    public static final String CTRL_UNCOMPILED ="uncompiled";
    public static final String CTRL_IMPORT ="import";
    public static final String CTRL_NAME ="ctrlname";
    public static final String CTRL_COMPLEX_RESULT ="ctrlcomplexresult";
    public static final String CTRL_SIMPLE_RESULT ="ctrlsimpleresult";
    public static final String CTRL_ERROR ="ctrlerror";

    public static final String CTRL_IF = "if";
}
