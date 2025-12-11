package net.thevpc.ntexup.cmdline.options;

import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NStringUtils;

import java.util.ArrayList;
import java.util.List;

public class EditorActionOptions extends ActionOptions {
    public boolean idea;
    public boolean kate;
    public boolean gedit;
    public boolean jedit;
    public boolean vim;
    public boolean vscode;
    public boolean nodepadpp;
    public boolean force;

    public EditorActionOptions() {
        super(Action.EDITOR);
    }

    public void withEditor(String name) {
        for (String s : NStringUtils.split(NStringUtils.trim(name).toLowerCase(), ",;:|")) {
            switch (s) {
                case "idea":
                case "intellij": {
                    idea = true;
                    break;
                }
                case "kate": {
                    kate = true;
                    break;
                }
                case "gedit": {
                    gedit = true;
                    break;
                }
                case "vi":
                case "vim": {
                    vim = true;
                    break;
                }
                case "jedit": {
                    jedit = true;
                    break;
                }
                case "nodepad++":
                case "nodepadpp":
                {
                    nodepadpp = true;
                    break;
                }
                case "vscode":
                {
                    vscode = true;
                    break;
                }
                case "all": {
                    vim = true;
                    idea = true;
                    gedit = true;
                    kate = true;
                    jedit = true;
                    nodepadpp = true;
                    vscode = true;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("unknown editor: " + name);
                }
            }
        }
    }

}
