package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.platform.NSysEditorFamily;

import java.util.LinkedHashSet;
import java.util.Set;

public class EditorActionOptions extends ActionOptions {
    private Set<NSysEditorFamily> syntaxInfo=new LinkedHashSet<>();

    public EditorActionOptions() {
        super(Action.EDITOR);
    }
    public Set<NSysEditorFamily> getSyntaxInfo() {
        return syntaxInfo;
    }
}
