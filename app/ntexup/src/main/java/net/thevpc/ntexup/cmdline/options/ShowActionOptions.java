package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.io.NPath;

import java.util.ArrayList;
import java.util.List;

public class ShowActionOptions extends ActionOptions {
    public List<NPath> paths = new ArrayList<>();
    public ShowActionOptions() {
        super(Action.SHOW_DOCUMENT);
    }
}
