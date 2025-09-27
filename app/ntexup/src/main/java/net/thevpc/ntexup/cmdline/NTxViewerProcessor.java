package net.thevpc.ntexup.cmdline;

import com.formdev.flatlaf.FlatLightLaf;
import net.thevpc.ntexup.main.MainFrame;
import net.thevpc.ntexup.util.NTexupUtils;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.nswing.NSwingUtils;

import javax.swing.*;

public class NTxViewerProcessor {
    public void runViewer(Options options) {
        NSwingUtils.setSharedWorkspaceInstance();
        FlatLightLaf.setup(new com.formdev.flatlaf.FlatDarculaLaf());
        MainFrame mainFrame = new MainFrame();
        NTexupUtils.runUiAsync(()->{
            mainFrame.setVisible(true);
        });

        if (options.reopen) {
            NPath p = mainFrame.getService().getLatestProjectPath();
            if (p != null) {
                options.paths.add(p);
            }
        }
        if (options.showLogs) {
            mainFrame.getService().showDebug();
        }
        switch (options.action){
            case OPEN:{
                if (options.paths.isEmpty()) {

                } else {
                    for (NPath path : options.paths) {
                        mainFrame.getService().openProject(path);
                    }
                }
                break;
            }
            case NEW:{
                if (options.paths.isEmpty()) {
                    mainFrame.getService().showNewProject(null);
                } else {
                    for (NPath path : options.paths) {
                        mainFrame.getService().showNewProject(path.toString());
                    }
                }
                break;
            }
        }

    }
}
