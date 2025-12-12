package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.command.NSysEditorFamily;

import java.util.LinkedHashSet;
import java.util.Set;

public class EditorActionOptions extends ActionOptions {
    private Set<NSysEditorFamily> syntaxInfo=new LinkedHashSet<>();
    private boolean force;

    public EditorActionOptions() {
        super(Action.EDITOR);
    }


    public boolean isForce() {
        return force;
    }

    public EditorActionOptions setForce(boolean force) {
        this.force = force;
        return this;
    }

    public Set<NSysEditorFamily> getSyntaxInfo() {
        return syntaxInfo;
    }
}
