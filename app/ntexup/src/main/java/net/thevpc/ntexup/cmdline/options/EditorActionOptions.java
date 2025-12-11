package net.thevpc.ntexup.cmdline.options;

import net.thevpc.ntexup.cmdline.NEditorSyntaxInstaller;

public class EditorActionOptions extends ActionOptions {
    private NEditorSyntaxInstaller.Info syntaxInfo=new NEditorSyntaxInstaller.Info();

    public EditorActionOptions() {
        super(Action.EDITOR);
    }


    public NEditorSyntaxInstaller.Info getSyntaxInfo() {
        return syntaxInfo;
    }
}
