package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.io.NPath;

import java.util.ArrayList;
import java.util.List;

public class GenerateActionOptions extends ActionOptions {
    public List<NPath> paths = new ArrayList<>();
    public NPath output;
    public OutputFormat outputFormat = OutputFormat.PDF;

    public GenerateActionOptions() {
        super(Action.GENERATE);
    }
}
