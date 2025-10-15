package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.io.NPath;

import java.util.ArrayList;
import java.util.List;

public class NewActionOptions extends ActionOptions {
    public String templateUrl;
    public boolean openViewer;
    public boolean generatePdf;
    public NPath generatePdfOutput;
    public List<NPath> paths = new ArrayList<>();
    public NewActionOptions() {
        super(Action.NEW);
    }
}
