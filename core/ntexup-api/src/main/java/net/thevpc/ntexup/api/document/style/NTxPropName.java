package net.thevpc.ntexup.api.document.style;

/**
 *
 * @author vpc
 */
public class NTxPropName {

    public static final String WIDTH="width";
    public static final String HEIGHT="height";

    /**
     * Defines the target coordinate within the parent's region where the component is placed.
     * <p>In NTexUp's purely relative system, a value of 50 represents 50% of the
     * allowed region's dimension. If specified with a 'P' suffix (e.g., 50%P),
     * it resolves against the full Page dimensions instead of the immediate parent.</p>
     * <b>Role:</b> The "World" side of the external placement pin.
     */
    public static final String POSITION="position";

    /**
     * Defines the dimensions of the component's bounding box relative to its parent.
     * <p>Like all NTexUp metrics, this is percentage-based. A size of (100, 100)
     * instructs the component to occupy the full width and height of its
     * allowed region.</p>
     * <b>Role:</b> Determines the "Frame" for both layout and internal content.
     */
    public static final String SIZE="size";
    /**
     * The 'Pin' point inside the component's own area.
     * * Value is a percentage of the component's calculated size.
     * * 50 is equivalent to 50% (0.5).
     */
    public static final String ORIGIN="origin";

    /**
     * The 'Pin' point inside the internal drawing (e.g., Circle/Image).
     * * Value is a percentage of the intrinsic content size.
     * * Ensures the 'Ink' remains aligned correctly when it
     * doesn't fill the entire component box (e.g., aspect ratio constraints).
     */
    public static final String CONTENT_ORIGIN = "content-origin";

    /**
     * A high-level shortcut property that synchronizes {@link #POSITION} and {@code ORIGIN}.
     * <p>Setting {@code at: center} (or 50) automatically sets the parent's
     * target position to 50% and the component's own origin (handle) to 50%.
     * This ensures the component is perfectly centered on that point without
     * manual coordinate mapping.</p>
     * <b>Role:</b> Unified "External" placement.
     */
    public static final String AT="at";

    /**
     * A high-level shortcut property that synchronizes {@link #CONTENT_POSITION}
     * and {@code CONTENT_ORIGIN}.
     * <p>Used primarily for components with intrinsic aspect ratios (like Circles or Images).
     * It defines where the "Ink" sits inside the component's {@link #SIZE} box.
     * For example, {@code align: left} ensures the content touches the left edge
     * of its box even if it doesn't fill the width.</p>
     * <b>Role:</b> Unified "Internal" content alignment.
     */
    public static final String ALIGN="align";

    /**
     * Defines the target coordinate within the component's own bounds for internal drawing.
     * <p>This allows for precise placement of the content "Ink" inside the
     * component's frame. It operates on the same relative scale (0-100) as
     * the external {@link #POSITION}.</p>
     * <b>Role:</b> The "World" side of the internal content pin.
     */
    public static final String CONTENT_POSITION="content-position";


    public static final String FONT_FAMILY="font-family";
    public static final String FONT_SIZE="font-size";
    public static final String STROKE="stroke";
    public static final String FONT_BOLD="font-bold";
    public static final String FONT_ITALIC="font-italic";
    public static final String FONT_UNDERLINED="font-underlined";
    public static final String FONT_STRIKE="font-strike";
    public static final String FOREGROUND_COLOR="foreground-color";
    public static final String BACKGROUND_COLOR="background-color";
    public static final String COLORS ="colors";
    public static final String LINE_COLOR="line-color";
    public static final String GRID_COLOR="grid-color";
    public static final String DRAW_GRID="draw-grid";
    public static final String FILL_BACKGROUND="fill-background";
    public static final String DRAW_CONTOUR="draw-contour";
    public static final String COMPONENT_NAME="component-name";
    public static final String PRESERVE_ASPECT_RATIO ="preserve-aspect-ratio";
    public static final String ROUND_CORNER="round-corner";
    public static final String COLSPAN="colspan";
    public static final String ROWSPAN="rowspan";
    public static final String COLWEIGHT="colweight";
    public static final String ROWWEIGHT="rowweight";
    public static final String THEED="threed";
    public static final String RAISED="raised";
    public static final String COLUMNS="columns";
    public static final String POINTS="points";
    public static final String POINT="point";
    public static final String COUNT="count";
    public static final String ROWS="rows";
    public static final String ROWS_WEIGHT="rows-weight";
    public static final String COLUMNS_WEIGHT="columns-weight";
    public static final String CLASS ="class";
    public static final String HIDE ="disabled";
    public static final String NAME="name";
    public static final String MAX_X="xmax";
    public static final String MAX_Y="ymax";
    public static final String VALUE="value";
    public static final String DISPLAY_NAME="display-name";
    public static final String MODE="mode";
    public static final String FILE="file";
    public static final String LANG="lang";
    public static final String FROM="from";
    public static final String START_ARROW ="start-arrow";
    public static final String END_ARROW ="end-arrow";
    public static final String TO="to";
    public static final String CTRL="ctrl";
    public static final String CTRL1="ctrl1";
    public static final String CTRL2="ctrl2";
    public static final String PADDING="padding";
    public static final String MARGIN="margin";
    public static final String ROTATE="rotate";
    public static final String SHADOW="shadow";

    public static final String DEBUG = "debug";
    public static final String DEBUG_COLOR="debug-color";
    public static final String ELLIPSE_H = "ellipse-height" ;
    public static final String SEGMENT_COUNT = "segment-count" ;
    public static final String TOP_COLOR = "top-color" ;
    public static final String INNER_RADIUS = "inner-radius" ;
    public static final String START_ANGLE = "start-angle" ;
    public static final String EXTENT_ANGLE = "extent-angle" ;
    public static final String END_ANGLE = "end-angle" ;
    public static final String SLICE_COUNT = "slice-count" ;
    public static final String SLICES = "slices" ;
    public static final String DASH = "dash" ;

    public static final String TRANSPARENT_COLOR = "transparent-color";
    public static final String ARGS = "args";
}
