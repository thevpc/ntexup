package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.io.NPath;

import java.util.ArrayList;
import java.util.List;

public class ShowFrameActionOptions extends ActionOptions {
    public boolean viewLog = false;
    public boolean ifNoProjectViewCurrentDirectory = false;
//    public List<NPath> paths = new ArrayList<>();
    public ShowFrameActionOptions() {
        super(Action.SHOW_FRAME);
    }
}
