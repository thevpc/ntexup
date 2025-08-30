package net.thevpc.ntexup.cmdline;

import com.formdev.flatlaf.FlatLightLaf;
import net.thevpc.ntexup.main.MainFrame;
import net.thevpc.nuts.cmdline.NCmdLine;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.nswing.NSwingUtils;

import javax.swing.*;

public class NTxViewerProcessor {
    public void runViewer(NCmdLine cmdLine,Options options) {
        while (!cmdLine.isEmpty()) {
            cmdLine.matcher()
                    .with("--reopen").matchTrueFlag(a -> options.reopen = true)
                    .with("--documentation").matchFlag(a -> options.documentation = true)
                    .with("--open").matchEntry(a -> options.paths.add(NPath.of(a.stringValue())))
                    .with("--new").matchTrueFlag(a -> options.action = Action.NEW)
                    .with("--view").matchTrueFlag(a -> options.requireViewer())
                    .with("--view-log").matchTrueFlag(a -> options.requireViewer().showLogs = true)
                    .withNonOption().matchAny(a -> options.paths.add(NPath.of(a.image())))
                    .requireDefaults();
        }
        if (options.documentation) {
            options.paths.add(NPath.of("github://thevpc/ntexup-doc-slides/"));
        }
        NSwingUtils.setSharedWorkspaceInstance();
        FlatLightLaf.setup(new com.formdev.flatlaf.FlatDarculaLaf());
        MainFrame mainFrame = new MainFrame();
        SwingUtilities.invokeLater(() -> {
            mainFrame.setVisible(true);
            if (options.reopen) {
                NPath p = mainFrame.getService().getLatestProjectPath();
                if (p != null) {
                    options.paths.add(p);
                }
            }
            if (options.showLogs) {
                mainFrame.getService().showDebug();
            }
            if (options.paths.isEmpty()) {

            } else {
                for (NPath path : options.paths) {
                    mainFrame.getService().openProject(path);
                }
            }
        });
    }
}
