package net.thevpc.ntexup.main;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.NTxDocumentLoadingResult;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.config.NTxProject;
import net.thevpc.ntexup.config.NTxViewerConfigManager;
import net.thevpc.ntexup.config.UserConfig;
import net.thevpc.ntexup.config.UserConfigManager;
import net.thevpc.ntexup.debug.NTxDebugFrame;
import net.thevpc.ntexup.debug.NTxTsonPanel;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.main.components.NewProjectPanel;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererListener;
import net.thevpc.ntexup.api.renderer.NTxDocumentScreenRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.util.NTexupUtils;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.log.NLog;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NStringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class NTxServiceHelper {

    public static final FileFilter NTEXUP_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            return f.getName().toLowerCase().endsWith(".ntx");
        }

        @Override
        public String getDescription() {
            return "Ntexup File";
        }
    };
    MainFrame mainFrame;
    NTxEngine engine;
    UserConfigManager usersConfigManager;
    NTxViewerConfigManager configManager;
    NTxDocumentRendererListenerList currListeners = new NTxDocumentRendererListenerList();
    NTxDebugFrame debugFrame;

    public NTxServiceHelper(MainFrame mainFrame, NTxEngine engine) {
        this.mainFrame = mainFrame;
        this.engine = engine;
//        this.engine.importDefaultDependencies();
        this.debugFrame = new NTxDebugFrame(engine);
        this.engine.addLog(debugFrame.messages());
        //this.engine.dump();

        this.currListeners.add(new NTxDocumentRendererListener() {

            @Override
            public void onSaveDocument(NTxCompiledDocument document, NTxDocumentStreamRendererConfig config) {
                doSavePDf(document, config);
            }

        });
        this.configManager = new NTxViewerConfigManager();
        this.usersConfigManager = new UserConfigManager();
        this.currListeners.add(debugFrame.rendererListener());
        this.debugFrame.setOnClose(new Runnable() {
            @Override
            public void run() {
                //currentMessages.remove(debugFrame.messages());
                //currListeners.remove(debugFrame.rendererListener());
            }
        });
    }

    public NTxEngine engine() {
        return engine;
    }

    public void showDebug() {
        debugFrame.run();
    }

    public NPath getLatestProjectPath() {
        return config().getLatestProjectPath();
    }

    public NTxViewerConfigManager config() {
        return configManager;
    }

    public UserConfigManager usersConfig() {
        return usersConfigManager;
    }

    public void openProject(NPath path) {
        configManager.markAccessed(path);
        NTxDocumentScreenRenderer renderer = engine.newScreenRenderer().get();
        renderer.addRendererListener(currListeners);
        renderer.addRendererListener(new NTxDocumentRendererListener() {
            MainFrame.ProgressItem o;

            @Override
            public void onStartLoadingDocument() {
                o = mainFrame.addProgressItem();
            }


            @Override
            public void onEndLoadingDocument() {
                if (o != null) {
                    o.dispose();
                }
            }
        });
        renderer.renderPath(path);
    }

    public void showOpenFile() {
        NPath nPath = configManager.getLastAccessedPath();
        JFileChooser f = new JFileChooser();
        f.setFileFilter(NTEXUP_FILTER);
        f.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (nPath != null) {
            f.setCurrentDirectory(nPath.toFile().get());
        }
        int r = f.showOpenDialog(mainFrame.getContentPane());
        if (r == JFileChooser.APPROVE_OPTION) {
            File sf = f.getSelectedFile();
            if (sf != null) {
                openProject(NPath.of(sf));
            }
        }
    }

    public void showNewProject() {
        showNewProject(null);
    }

    public void showNewProject(String rootFolder) {
        NTexupUtils.runUiAsync(() -> {
            NewProjectPanel projectPanel = new NewProjectPanel(this);
            projectPanel.setSelectedRootFolder(rootFolder);
            if (projectPanel.showDialog(mainFrame.getContentPane())) {
                NTxTemplateInfo selectedTemplate = projectPanel.getSelectedTemplate();
                if (selectedTemplate == null) {
                    selectedTemplate = Arrays.stream(engine.getTemplates()).filter(NTxTemplateInfo::recommended).findAny().orElse(null);
                }
                if (selectedTemplate == null) {
                    JOptionPane.showMessageDialog(mainFrame.getContentPane(), "Missing template", "Warning", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                NPath rootPath = NPath.of(NStringUtils.firstNonBlank(projectPanel.getSelectedRootFolder(), "."));
                NPath newProjectPath;
                String baseName = NStringUtils.trim(projectPanel.getSelectedProjectName());
                if (NBlankable.isBlank(baseName)) {
                    baseName = "new-project";
                }
                int index = 1;
                newProjectPath = null;
                while (true) {
                    String nn = index == 1 ? baseName : (baseName + "-" + index);
                    newProjectPath = rootPath.resolve(nn);
                    if (!newProjectPath.exists()) {
                        break;
                    }
                    index++;
                }
                if (index != 1) {
                    if (JOptionPane.showConfirmDialog(mainFrame.getContentPane(), "Folder " + baseName + " already exists. Should we consider new name " + newProjectPath.getName(), "Warning", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        //usersConfig().saveUserConfig(newUserConfig);
                    } else {
                        return;
                    }
                }
                Map<String, String> propValues = projectPanel.getPropValues();
                engine.createProject(newProjectPath, NPath.of(selectedTemplate.url()), propValues::get);
                UserConfig newUserConfig = projectPanel.getUserConfig();
                UserConfig oldUser = usersConfig().loadUserConfig(newUserConfig.getId());
                if (oldUser != null && Objects.equals(newUserConfig, oldUser)) {
                    if (JOptionPane.showConfirmDialog(mainFrame.getContentPane(), "User Info changed. Do you want to save them?", "Warning", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        usersConfig().saveUserConfig(newUserConfig);
                    }
                } else if (oldUser == null) {
                    usersConfig().saveUserConfig(newUserConfig);
                }
                openProject(newProjectPath);
            }
        });
    }

    public void showNewFile() {
        NPath nPath = configManager.getLastAccessedPath();
        JFileChooser f = new JFileChooser();
        f.setFileFilter(NTEXUP_FILTER);
        f.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (nPath != null) {
            f.setCurrentDirectory(nPath.toFile().get());
        }
        int r = f.showSaveDialog(mainFrame.getContentPane());
        if (r == JFileChooser.APPROVE_OPTION) {
            File sf = f.getSelectedFile();
            if (sf != null) {
                if (sf.isFile()) {
                    return;
                } else {
                    if (!sf.getName().endsWith(NTxEngine.FILE_DOT_EXT)) {
                        sf = new File(sf.getParent(), sf.getName() + NTxEngine.FILE_DOT_EXT);
                    }
                    NPath.of(sf).writeString("//Ntexup File\n");
                }
            }
        }
    }

    public NTxEngine getEngine() {
        return engine;
    }

    public void doExit() {
        System.exit(0);
    }

    public void doSavePDf(NTxCompiledDocument document, NTxDocumentStreamRendererConfig config) {
        //we must recompile the document so
        NTxDocument raw = document.rawDocument();
        NTxCompiledDocument newCompiledDocument = engine.asCompiledDocument(engine.compileDocument(raw).document().get());
        JFileChooser f = new JFileChooser();
        f.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int r = f.showOpenDialog(mainFrame.getContentPane());
        if (r == JFileChooser.APPROVE_OPTION) {
            File sf = f.getSelectedFile();
            if (sf != null) {
                if (!sf.exists()) {
                    if (!sf.getName().endsWith(".pdf")) {
                        sf = new File(sf.getParent(), sf.getName() + ".pdf");
                    }
                }
                NPath outputPdfPath = NPath.of(sf);

                NTxDocumentStreamRenderer renderer = engine.newPdfRenderer().get();
                renderer.setStreamRendererConfig(config);
                renderer.setOutput(outputPdfPath);
                renderer.render(newCompiledDocument);

                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(sf);
                    } catch (IOException ex) {
                        NLog.of(NTxServiceHelper.class).log(NMsg.ofC("failed : %s", ex).asFinestFail(ex));
                    }
                } else {
                    System.out.println("Desktop is not supported on this system.");
                }
            }
        }
    }

}
