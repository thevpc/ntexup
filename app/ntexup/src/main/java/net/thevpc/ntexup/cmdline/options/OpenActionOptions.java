package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.io.NPath;

import java.util.ArrayList;
import java.util.List;

public class OpenActionOptions extends ActionOptions {
    public List<NPath> paths = new ArrayList<>();
    public OpenActionOptions() {
        super(Action.OPEN);
    }
}
