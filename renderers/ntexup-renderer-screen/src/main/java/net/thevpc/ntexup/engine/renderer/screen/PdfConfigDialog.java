package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxPageOrientation;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.log.NLog;
import net.thevpc.nuts.swing.GBC;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.log.NLog;
import net.thevpc.nuts.swing.GBC;
import net.thevpc.nuts.text.NMsg;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

public class PdfConfigDialog extends JDialog {

    public static class PageSizeItem {
        final String name;
        final int width;
        final int height;

        public PageSizeItem(String name, int width, int height) {
            this.name = name;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final DocumentView documentView;
    private final NTxEngine engine;

    private JTextField outputFileField;
    private JButton browseButton;
    private JCheckBox openAfterExportCheckBox;

    private JRadioButton landscapeRadioButton;
    private JRadioButton portraitRadioButton;
    private JComboBox<PageSizeItem> sizePageComboBox;

    private JComboBox<String> gridPresetComboBox;
    private JSpinner gridXSpinner;
    private JSpinner gridYSpinner;

    private JSpinner marginTopSpinner;
    private JSpinner marginBottomSpinner;
    private JSpinner marginLeftSpinner;
    private JSpinner marginRightSpinner;

    private JCheckBox showPageNumberCheckBox;
    private JCheckBox showFileNameCheckBox;
    private JCheckBox showDateCheckBox;

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton exportButton;
    private JButton cancelButton;

    private boolean confirmed;
    private boolean updatingGridPreset;

    public PdfConfigDialog(Frame parent, DocumentView documentView) {
        super(parent, "Export to PDF", true);
        this.documentView = documentView;
        this.engine = documentView.engine();

        initComponents();
        initValues();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(16, 20, 12, 20));

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel titleLabel = new JLabel("Export Document to PDF");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 15f));
        titleLabel.setBorder(new EmptyBorder(0, 0, 0, 12));

        JLabel descLabel = new JLabel("Configure destination, page layout, and appearance options.");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 12f));
        Color disabledFg = UIManager.getColor("Label.disabledForeground");
        if (disabledFg != null) {
            descLabel.setForeground(disabledFg);
        }

        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(descLabel);
        headerPanel.add(titleBlock, BorderLayout.CENTER);

        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(headerPanel, BorderLayout.CENTER);
        northWrapper.add(new JSeparator(), BorderLayout.SOUTH);
        add(northWrapper, BorderLayout.NORTH);

        // Content Form Panel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        int row = 0;

        // --- Section: Destination ---
        contentPanel.add(createSectionHeader("Destination"), GBC.of(0, row++).colspan(2).fillHorizontal().insets(0, 0, 8, 0).build());

        outputFileField = new JTextField();
        outputFileField.setColumns(28);
        browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseOutputFile());

        JPanel fileChooserPanel = new JPanel(new BorderLayout(6, 0));
        fileChooserPanel.setOpaque(false);
        fileChooserPanel.add(outputFileField, BorderLayout.CENTER);
        fileChooserPanel.add(browseButton, BorderLayout.EAST);

        addFormField(contentPanel, row++, "Output File:", fileChooserPanel);

        openAfterExportCheckBox = new JCheckBox("Open PDF when export completes", true);
        contentPanel.add(openAfterExportCheckBox, GBC.of(1, row++).anchorWest().insets(0, 4, 12, 0).build());

        // --- Section: Page & Layout ---
        contentPanel.add(createSectionHeader("Page & Layout"), GBC.of(0, row++).colspan(2).fillHorizontal().insets(0, 0, 8, 0).build());

        // Orientation
        JPanel orientationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        orientationPanel.setOpaque(false);
        ButtonGroup orientationGroup = new ButtonGroup();
        landscapeRadioButton = new JRadioButton("Landscape", true);
        portraitRadioButton = new JRadioButton("Portrait", false);
        orientationGroup.add(landscapeRadioButton);
        orientationGroup.add(portraitRadioButton);
        orientationPanel.add(landscapeRadioButton);
        orientationPanel.add(portraitRadioButton);
        addFormField(contentPanel, row++, "Orientation:", orientationPanel);

        // Page Size
        sizePageComboBox = new JComboBox<>(new PageSizeItem[]{
                new PageSizeItem("A4 (210 × 297 mm)", 595, 842),
                new PageSizeItem("A3 (297 × 420 mm)", 842, 1191),
                new PageSizeItem("Letter (8.5 × 11 in)", 612, 792),
                new PageSizeItem("Presentation Widescreen (16:9 - 960 × 540 pt)", 960, 540),
                new PageSizeItem("Presentation Standard (4:3 - 800 × 600 pt)", 800, 600),
                new PageSizeItem("Presentation HD (1280 × 720 pt)", 1280, 720),
        });
        addFormField(contentPanel, row++, "Page Size:", sizePageComboBox);

        // Grid (Slides per Page)
        gridPresetComboBox = new JComboBox<>(new String[]{
                "1 slide per page (1 × 1)",
                "2 slides per page (1 × 2)",
                "4 slides per page (2 × 2 - Handout)",
                "6 slides per page (2 × 3)",
                "Custom Grid..."
        });

        gridXSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        gridYSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        ((JSpinner.DefaultEditor) gridXSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor) gridYSpinner.getEditor()).getTextField().setColumns(3);

        JPanel gridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        gridPanel.setOpaque(false);
        gridPanel.add(gridPresetComboBox);
        gridPanel.add(new JLabel("  Cols:"));
        gridPanel.add(gridXSpinner);
        gridPanel.add(new JLabel("Rows:"));
        gridPanel.add(gridYSpinner);
        addFormField(contentPanel, row++, "Slides per Page:", gridPanel);

        gridPresetComboBox.addActionListener(e -> {
            if (updatingGridPreset) return;
            updatingGridPreset = true;
            try {
                int idx = gridPresetComboBox.getSelectedIndex();
                switch (idx) {
                    case 0:
                        gridXSpinner.setValue(1);
                        gridYSpinner.setValue(1);
                        break;
                    case 1:
                        gridXSpinner.setValue(1);
                        gridYSpinner.setValue(2);
                        break;
                    case 2:
                        gridXSpinner.setValue(2);
                        gridYSpinner.setValue(2);
                        break;
                    case 3:
                        gridXSpinner.setValue(2);
                        gridYSpinner.setValue(3);
                        break;
                }
            } finally {
                updatingGridPreset = false;
            }
        });

        javax.swing.event.ChangeListener spinnerListener = e -> {
            if (updatingGridPreset) return;
            updatingGridPreset = true;
            try {
                int gx = (Integer) gridXSpinner.getValue();
                int gy = (Integer) gridYSpinner.getValue();
                if (gx == 1 && gy == 1) {
                    gridPresetComboBox.setSelectedIndex(0);
                } else if (gx == 1 && gy == 2) {
                    gridPresetComboBox.setSelectedIndex(1);
                } else if (gx == 2 && gy == 2) {
                    gridPresetComboBox.setSelectedIndex(2);
                } else if (gx == 2 && gy == 3) {
                    gridPresetComboBox.setSelectedIndex(3);
                } else {
                    gridPresetComboBox.setSelectedIndex(4);
                }
            } finally {
                updatingGridPreset = false;
            }
        };
        gridXSpinner.addChangeListener(spinnerListener);
        gridYSpinner.addChangeListener(spinnerListener);

        // --- Section: Margins & Annotations ---
        contentPanel.add(createSectionHeader("Margins & Annotations"), GBC.of(0, row++).colspan(2).fillHorizontal().insets(8, 0, 8, 0).build());

        // Margins
        marginTopSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 200.0, 5.0));
        marginBottomSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 200.0, 5.0));
        marginLeftSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 200.0, 5.0));
        marginRightSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 200.0, 5.0));
        ((JSpinner.DefaultEditor) marginTopSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor) marginBottomSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor) marginLeftSpinner.getEditor()).getTextField().setColumns(3);
        ((JSpinner.DefaultEditor) marginRightSpinner.getEditor()).getTextField().setColumns(3);

        JPanel marginsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        marginsPanel.setOpaque(false);
        marginsPanel.add(new JLabel("Top:"));
        marginsPanel.add(marginTopSpinner);
        marginsPanel.add(new JLabel("Bottom:"));
        marginsPanel.add(marginBottomSpinner);
        marginsPanel.add(new JLabel("Left:"));
        marginsPanel.add(marginLeftSpinner);
        marginsPanel.add(new JLabel("Right:"));
        marginsPanel.add(marginRightSpinner);
        addFormField(contentPanel, row++, "Margins (pt):", marginsPanel);

        // Annotations
        JPanel annotationsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        annotationsPanel.setOpaque(false);
        showPageNumberCheckBox = new JCheckBox("Page numbers", true);
        showFileNameCheckBox = new JCheckBox("Document title", false);
        showDateCheckBox = new JCheckBox("Generation date", false);
        annotationsPanel.add(showPageNumberCheckBox);
        annotationsPanel.add(showFileNameCheckBox);
        annotationsPanel.add(showDateCheckBox);
        addFormField(contentPanel, row++, "Include:", annotationsPanel);

        add(contentPanel, BorderLayout.CENTER);

        // Footer Panel
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(new EmptyBorder(10, 20, 14, 20));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statusPanel.setOpaque(false);
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(130, 14));
        progressBar.setVisible(false);
        statusPanel.add(progressBar);
        statusPanel.add(statusLabel);
        footerPanel.add(statusPanel, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonsPanel.setOpaque(false);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            confirmed = false;
            setVisible(false);
        });

        exportButton = new JButton("Export PDF");
        exportButton.addActionListener(e -> startExport());

        buttonsPanel.add(cancelButton);
        buttonsPanel.add(exportButton);
        footerPanel.add(buttonsPanel, BorderLayout.EAST);

        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.add(new JSeparator(), BorderLayout.NORTH);
        southWrapper.add(footerPanel, BorderLayout.CENTER);
        add(southWrapper, BorderLayout.SOUTH);

        // Key bindings
        getRootPane().setDefaultButton(exportButton);
        getRootPane().registerKeyboardAction(
                e -> setVisible(false),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void initValues() {
        File defaultFile = defaultOutputFile();
        outputFileField.setText(defaultFile.getAbsolutePath());
    }

    private JPanel createSectionHeader(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(new EmptyBorder(0, 0, 0, 6));
        panel.add(label, GBC.of(0, 0).anchorWest().insets(0, 0, 0, 8).build());
        JSeparator sep = new JSeparator();
        panel.add(sep, GBC.of(1, 0).weightx(1).fillHorizontal().anchorCenter().build());
        return panel;
    }

    private void addFormField(JPanel panel, int row, String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(new EmptyBorder(0, 0, 0, 6));
        panel.add(label, GBC.of(0, row).anchorWest().insets(4, 4, 4, 12).build());
        panel.add(component, GBC.of(1, row).anchorWest().weightx(1).fillHorizontal().insets(4, 0, 4, 0).build());
    }

    private File defaultOutputFile() {
        if (documentView != null && documentView.compiledDocument() != null) {
            NTxSource source = documentView.compiledDocument().source();
            if (source != null) {
                NPath path = source.path().orNull();
                if (path != null) {
                    File file = path.toFile().orNull();
                    if (file != null) {
                        if (file.isDirectory()) {
                            return new File(file, file.getName() + ".pdf");
                        } else {
                            String name = file.getName();
                            int dot = name.lastIndexOf('.');
                            String base = (dot > 0) ? name.substring(0, dot) : name;
                            return new File(file.getParentFile(), base + ".pdf");
                        }
                    }
                }
            }
            String title = documentView.compiledDocument().title();
            if (title != null && !title.trim().isEmpty()) {
                String safe = title.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
                return new File(new File("."), safe + ".pdf");
            }
        }
        return new File(new File("."), "document.pdf");
    }

    private void browseOutputFile() {
        File current = null;
        String text = outputFileField.getText().trim();
        if (!text.isEmpty()) {
            current = new File(text);
        }
        File currentDir = (current != null && current.getParentFile() != null && current.getParentFile().exists())
                ? current.getParentFile()
                : new File(".");
        JFileChooser fc = new JFileChooser(currentDir);
        fc.setDialogTitle("Select PDF Output File");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (current != null) {
            fc.setSelectedFile(current);
        }
        fc.setFileFilter(new FileNameExtensionFilter("PDF Documents (*.pdf)", "pdf"));
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File sf = fc.getSelectedFile();
            if (sf != null) {
                if (!sf.getName().toLowerCase().endsWith(".pdf")) {
                    sf = new File(sf.getParentFile(), sf.getName() + ".pdf");
                }
                outputFileField.setText(sf.getAbsolutePath());
            }
        }
    }

    private void setEditableEditors(boolean editable) {
        exportButton.setEnabled(editable);
        cancelButton.setEnabled(editable);
        browseButton.setEnabled(editable);
        outputFileField.setEnabled(editable);
        openAfterExportCheckBox.setEnabled(editable);
        portraitRadioButton.setEnabled(editable);
        landscapeRadioButton.setEnabled(editable);
        sizePageComboBox.setEnabled(editable);
        gridPresetComboBox.setEnabled(editable);
        gridXSpinner.setEnabled(editable);
        gridYSpinner.setEnabled(editable);
        showPageNumberCheckBox.setEnabled(editable);
        showFileNameCheckBox.setEnabled(editable);
        showDateCheckBox.setEnabled(editable);
        marginTopSpinner.setEnabled(editable);
        marginBottomSpinner.setEnabled(editable);
        marginLeftSpinner.setEnabled(editable);
        marginRightSpinner.setEnabled(editable);
    }

    private void startExport() {
        String pathText = outputFileField.getText().trim();
        if (pathText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an output file destination.", "Export to PDF", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File target = new File(pathText);
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getParentFile(), target.getName() + ".pdf");
            outputFileField.setText(target.getAbsolutePath());
        }

        File parentDir = target.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (target.exists()) {
            int r = JOptionPane.showConfirmDialog(this,
                    "The file already exists:\n" + target.getAbsolutePath() + "\n\nDo you want to replace it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final File finalTarget = target;
        final NTxDocumentStreamRendererConfig config = getConfig();

        setEditableEditors(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Exporting PDF...");

        new Thread(() -> {
            try {
                NTxDocumentStreamRenderer renderer = engine.newPdfRenderer().get();
                renderer.setStreamRendererConfig(config);
                renderer.setOutput(NPath.of(finalTarget));
                renderer.render(documentView.compiledDocument());

                confirmed = true;
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    statusLabel.setText("Export complete!");
                    if (openAfterExportCheckBox.isSelected() && Desktop.isDesktopSupported()) {
                        try {
                            Desktop.getDesktop().open(finalTarget);
                        } catch (Exception ex) {
                            engine.log().log(NMsg.ofC("Failed to open PDF: %s", ex).asFinestFail(ex));
                        }
                    }
                    setVisible(false);
                });
            } catch (Throwable t) {
                engine.log().log(NMsg.ofC("Failed to export PDF: %s", t).asSevere());
                SwingUtilities.invokeLater(() -> {
                    setEditableEditors(true);
                    progressBar.setVisible(false);
                    statusLabel.setText("Export failed.");
                    JOptionPane.showMessageDialog(PdfConfigDialog.this,
                            "Failed to generate PDF:\n" + (t.getMessage() != null ? t.getMessage() : t.toString()),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public NTxDocumentStreamRendererConfig getConfig() {
        NTxDocumentStreamRendererConfig config = new NTxDocumentStreamRendererConfig();
        config.setOrientation(portraitRadioButton.isSelected() ? NTxPageOrientation.PORTRAIT : NTxPageOrientation.LANDSCAPE);
        config.setGridX((Integer) gridXSpinner.getValue());
        config.setGridY((Integer) gridYSpinner.getValue());

        PageSizeItem selectedSize = (PageSizeItem) sizePageComboBox.getSelectedItem();
        if (selectedSize != null) {
            config.setPageWidth(selectedSize.width);
            config.setPageHeight(selectedSize.height);
        }

        config.setShowPageNumber(showPageNumberCheckBox.isSelected());
        config.setShowFileName(showFileNameCheckBox.isSelected());
        config.setShowDate(showDateCheckBox.isSelected());
        config.setMarginTop(((Number) marginTopSpinner.getValue()).floatValue());
        config.setMarginBottom(((Number) marginBottomSpinner.getValue()).floatValue());
        config.setMarginLeft(((Number) marginLeftSpinner.getValue()).floatValue());
        config.setMarginRight(((Number) marginRightSpinner.getValue()).floatValue());
        return config;
    }

    public void doSavePDf(NTxCompiledDocument document, NTxDocumentStreamRendererConfig config, Component parentComponent) {
        startExport();
    }
}
