package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.io.NPath;

public class ShowHtmlActionOptions extends ActionOptions {
    public boolean html = false;
    public NPath path;
//    public List<NPath> paths = new ArrayList<>();
    public ShowHtmlActionOptions() {
        super(Action.SHOW_HTML);
    }
}
