package net.thevpc.ntexup.cmdline.options;

public class DumpDocumentOptions extends ActionOptions {
    /**
     * Optional capture level forwarded to the dump renderer
     * ({@code ntxgraphics} or {@code graphics2d}).
     */
    public String level;
    /**
     * When true, the dump renderer also embeds the sha256 of the equivalent
     * raster (png) rendering of each page.
     */
    public boolean withImage;
    /**
     * When true, extensions are not asked to contribute extra semantic infos
     * (chart models, ...) to the dump; only the paint primitives are captured.
     */
    public boolean noInfos;

    public DumpDocumentOptions() {
        super(Action.DUMP);
    }
}
