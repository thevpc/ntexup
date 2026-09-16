package net.thevpc.ntexup.cmdline.options;

import net.thevpc.ntexup.api.renderer.NTxPageOrientation;
import net.thevpc.nuts.io.NPath;

import java.util.ArrayList;
import java.util.List;

public class GenerateActionOptions extends ActionOptions {
    public List<NPath> paths = new ArrayList<>();
    public NPath output;
    public boolean outputDirectory;
    public OutputFormat outputFormat = OutputFormat.PDF;
    public List<Integer> pages = new ArrayList<>();
    public int dpi;
    public String imageFormat;
    public int gridX = 1;
    public int gridY = 1;
    public float marginTop = -1;
    public float marginBottom = -1;
    public float marginLeft = -1;
    public float marginRight = -1;
    public Integer pageWidth;
    public Integer pageHeight;
    public NTxPageOrientation orientation;
    public boolean showPageNumber;

    public GenerateActionOptions() {
        super(Action.GENERATE);
    }

    public GenerateActionOptions addPath(NPath path) {
        paths.add(path);
        return this;
    }
}