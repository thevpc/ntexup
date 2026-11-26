package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.swing.GBC;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Viewer-side "Export" dialog: renders the currently loaded document to any
 * of the formats the command line knows (images, static html folder /
 * standalone page / zip bundle, {@code .ntxp} container) or packs the deck
 * sources into a zip. PDF keeps its own richer dialog
 * ({@link PdfConfigDialog}); it is reachable from here too.
 * <p>
 * Rendering runs on a background thread, the dialog only shows progress, so
 * the viewer stays responsive.
 */
public class ExportDialog extends JDialog {

    /** every export the viewer offers */
    public enum Format {
        PDF("PDF document (.pdf)", "pdf", false),
        IMAGES("PNG images (one per slide, folder)", "", true),
        HTML_FOLDER("HTML site (folder)", "", true),
        HTML_STANDALONE("HTML single page (.html)", "html", false),
        HTML_ZIP("HTML site bundle (.zip)", "zip", false),
        NTXP("ntexup player container (.ntxp)", "ntxp", false),
        SOURCES_ZIP("Deck sources (.zip)", "zip", false);

        final String label;
        final String extension;
        final boolean folder;

        Format(String label, String extension, boolean folder) {
            this.label = label;
            this.extension = extension;
            this.folder = folder;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** folders never worth packing into a sources zip */
    private static final Set<String> SKIPPED_SOURCE_FOLDERS = Set.of(".git", ".svn", ".hg", "target", "dist", "node_modules", ".idea");

    private final DocumentView documentView;
    private final NTxEngine engine;
    private final JComboBox<Format> formatCombo = new JComboBox<>(Format.values());
    private final JTextField outputField = new JTextField(36);
    private final JButton browseButton = new JButton("Browse…");
    private final JCheckBox openAfter = new JCheckBox("Open when done", true);
    private final JLabel hintLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton exportButton = new JButton("Export");
    private final JButton cancelButton = new JButton("Close");
    /** output path the user typed/picked; null while we still follow the format default */
    private boolean outputEdited;
    private boolean updatingOutput;

    public ExportDialog(Frame owner, DocumentView documentView, Format initial) {
        super(owner, "Export Document", true);
        this.documentView = documentView;
        this.engine = documentView.engine();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(new EmptyBorder(16, 18, 14, 18));
        setContentPane(content);

        JLabel header = new JLabel("Export \"" + documentTitle() + "\"");
        header.setFont(header.getFont().deriveFont(Font.BOLD, header.getFont().getSize2D() + 3f));
        content.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        form.add(new JLabel("Format"), GBC.of(0, row).anchorWest().insets(4, 0, 4, 12).build());
        form.add(formatCombo, GBC.of(1, row).colspan(2).anchorWest().weightx(1).fillHorizontal().insets(4, 0, 4, 0).build());
        row++;
        form.add(new JLabel("Output"), GBC.of(0, row).anchorWest().insets(4, 0, 4, 12).build());
        form.add(outputField, GBC.of(1, row).anchorWest().weightx(1).fillHorizontal().insets(4, 0, 4, 6).build());
        form.add(browseButton, GBC.of(2, row).anchorWest().insets(4, 0, 4, 0).build());
        row++;
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize2D() - 1f));
        form.add(hintLabel, GBC.of(1, row).colspan(2).anchorWest().weightx(1).fillHorizontal().insets(0, 0, 6, 0).build());
        row++;
        form.add(openAfter, GBC.of(1, row).colspan(2).anchorWest().insets(2, 0, 2, 0).build());
        content.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(8, 6));
        progressBar.setVisible(false);
        south.add(progressBar, BorderLayout.NORTH);
        south.add(statusLabel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(cancelButton);
        buttons.add(exportButton);
        south.add(buttons, BorderLayout.SOUTH);
        content.add(south, BorderLayout.SOUTH);

        formatCombo.setSelectedItem(initial == null ? Format.PDF : initial);
        formatCombo.addActionListener(e -> onFormatChanged());
        outputField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void edited() {
                if (!updatingOutput) {
                    outputEdited = true;
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                edited();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                edited();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                edited();
            }
        });
        browseButton.addActionListener(e -> browse());
        cancelButton.addActionListener(e -> dispose());
        exportButton.addActionListener(e -> startExport());
        getRootPane().setDefaultButton(exportButton);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        onFormatChanged();
        pack();
        setMinimumSize(new Dimension(Math.max(560, getWidth()), getHeight()));
        setLocationRelativeTo(owner);
    }

    // ------------------------------------------------------------------

    private Format format() {
        Format f = (Format) formatCombo.getSelectedItem();
        return f == null ? Format.PDF : f;
    }

    private void onFormatChanged() {
        Format f = format();
        if (!outputEdited || outputField.getText().trim().isEmpty()) {
            setOutputText(defaultOutput(f).getAbsolutePath());
        } else {
            // keep the user's folder/name, adapt the extension
            setOutputText(adaptExtension(new File(outputField.getText().trim()), f).getAbsolutePath());
        }
        switch (f) {
            case PDF:
                hintLabel.setText("Opens the PDF options (page size, grid, margins…).");
                exportButton.setText("Options…");
                break;
            case IMAGES:
                hintLabel.setText("One PNG file per slide, written into this folder.");
                exportButton.setText("Export");
                break;
            case HTML_FOLDER:
                hintLabel.setText("index.html + player assets; serve or open index.html.");
                exportButton.setText("Export");
                break;
            case HTML_STANDALONE:
                hintLabel.setText("A single self-contained .html file (assets inlined).");
                exportButton.setText("Export");
                break;
            case HTML_ZIP:
                hintLabel.setText("The HTML site packed as one zip archive.");
                exportButton.setText("Export");
                break;
            case NTXP:
                hintLabel.setText("Pre-rendered playback container for the ntexup players.");
                exportButton.setText("Export");
                break;
            case SOURCES_ZIP:
                hintLabel.setText("The .ntx sources and resources (target/, .git/ … excluded).");
                exportButton.setText("Export");
                break;
        }
    }

    private void setOutputText(String text) {
        updatingOutput = true;
        try {
            outputField.setText(text);
        } finally {
            updatingOutput = false;
        }
    }

    private String documentTitle() {
        NTxCompiledDocument d = documentView.compiledDocument();
        String t = d == null ? null : d.title();
        return (t == null || t.trim().isEmpty()) ? "document" : t.trim();
    }

    /** the deck folder (or file) the viewer loaded, when it lives on disk */
    private File sourceFile() {
        NTxCompiledDocument d = documentView.compiledDocument();
        if (d == null) {
            return null;
        }
        NTxSource source = d.source();
        if (source == null) {
            return null;
        }
        NPath path = source.path().orNull();
        return path == null ? null : path.toFile().orNull();
    }

    private File defaultOutput(Format f) {
        File src = sourceFile();
        File dir;
        String base;
        if (src != null) {
            if (src.isDirectory()) {
                dir = new File(src, "target");
                base = src.getName();
            } else {
                File parent = src.getAbsoluteFile().getParentFile();
                dir = new File(parent, "target");
                String n = src.getName();
                int dot = n.lastIndexOf('.');
                base = dot > 0 ? n.substring(0, dot) : n;
                if ("main".equals(base) && parent != null) {
                    base = parent.getName();
                }
            }
        } else {
            dir = new File(".");
            base = documentTitle().replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        switch (f) {
            case IMAGES:
                return new File(dir, base + "-images");
            case HTML_FOLDER:
                return new File(dir, base + "-html");
            case HTML_ZIP:
                return new File(dir, base + "-html.zip");
            case SOURCES_ZIP:
                return new File(dir, base + "-sources.zip");
            default:
                return new File(dir, base + "." + f.extension);
        }
    }

    private static File adaptExtension(File file, Format f) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        if (f.folder) {
            return new File(file.getParentFile(), base);
        }
        return new File(file.getParentFile(), base + "." + f.extension);
    }

    private void browse() {
        Format f = format();
        File current = new File(outputField.getText().trim());
        JFileChooser fc = new JFileChooser(current.getParentFile() != null && current.getParentFile().exists()
                ? current.getParentFile() : new File("."));
        fc.setDialogTitle(f.folder ? "Select Output Folder" : "Select Output File");
        if (f.folder) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setSelectedFile(current);
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(f.label, f.extension));
        }
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            File sf = fc.getSelectedFile();
            if (!f.folder && !sf.getName().toLowerCase().endsWith("." + f.extension)) {
                sf = new File(sf.getParentFile(), sf.getName() + "." + f.extension);
            }
            outputField.setText(sf.getAbsolutePath());
        }
    }

    // ------------------------------------------------------------------

    private void startExport() {
        Format f = format();
        if (f == Format.PDF) {
            dispose();
            new PdfConfigDialog((Frame) getOwner(), documentView).setVisible(true);
            return;
        }
        String text = outputField.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please choose an output location.", getTitle(), JOptionPane.WARNING_MESSAGE);
            return;
        }
        File target = f.folder ? new File(text) : adaptExtensionIfMissing(new File(text), f);
        if (target.exists() && (!f.folder || hasChildren(target))) {
            int r = JOptionPane.showConfirmDialog(this,
                    (f.folder ? "The folder is not empty:\n" : "The file already exists:\n") + target.getAbsolutePath()
                            + "\n\nDo you want to " + (f.folder ? "write into it" : "replace it") + "?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r != JOptionPane.YES_OPTION) {
                return;
            }
        }
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        NTxCompiledDocument document = documentView.compiledDocument();
        File source = sourceFile();
        if (f == Format.SOURCES_ZIP && source == null) {
            JOptionPane.showMessageDialog(this, "This document has no sources on disk.", getTitle(), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (f != Format.SOURCES_ZIP && document == null) {
            JOptionPane.showMessageDialog(this, "No document is loaded.", getTitle(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        setBusy(true, "Exporting " + f.label + "…");
        final File finalTarget = target;
        Thread t = new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                if (f == Format.SOURCES_ZIP) {
                    zipSources(source, finalTarget);
                } else {
                    render(document, f, finalTarget);
                }
                long ms = System.currentTimeMillis() - start;
                engine.log().log(NMsg.ofC("exported %s : %s in %sms", f.name().toLowerCase(), finalTarget.getAbsolutePath(), ms).asInfo());
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, "Exported to " + finalTarget.getAbsolutePath());
                    if (openAfter.isSelected()) {
                        openResult(f, finalTarget);
                    }
                    dispose();
                });
            } catch (Throwable ex) {
                engine.log().log(NMsg.ofC("export %s failed: %s", f.name().toLowerCase(), ex).asSevere());
                SwingUtilities.invokeLater(() -> {
                    setBusy(false, "Export failed.");
                    JOptionPane.showMessageDialog(ExportDialog.this,
                            "Export failed:\n" + (ex.getMessage() != null ? ex.getMessage() : ex.toString()),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "ntexup-export");
        t.setDaemon(true);
        t.start();
    }

    private static boolean hasChildren(File folder) {
        String[] list = folder.list();
        return list != null && list.length > 0;
    }

    private static File adaptExtensionIfMissing(File file, Format f) {
        if (file.getName().toLowerCase().endsWith("." + f.extension)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + "." + f.extension);
    }

    private void setBusy(boolean busy, String status) {
        exportButton.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
        browseButton.setEnabled(!busy);
        formatCombo.setEnabled(!busy);
        outputField.setEnabled(!busy);
        openAfter.setEnabled(!busy);
        progressBar.setVisible(busy);
        progressBar.setIndeterminate(busy);
        statusLabel.setText(status);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void render(NTxCompiledDocument document, Format f, File target) {
        NOptional<NTxDocumentStreamRenderer> r;
        switch (f) {
            case IMAGES:
                r = engine.newImageRenderer();
                break;
            case NTXP:
                r = engine.newStreamRenderer("ntxp");
                break;
            default:
                r = engine.newStreamRenderer("html-static");
                break;
        }
        NTxDocumentStreamRenderer renderer = r.get();
        renderer.setStreamRendererConfig(new NTxDocumentStreamRendererConfig());
        if (f == Format.HTML_STANDALONE) {
            // same key as HtmlStaticDocumentRenderer.PROP_STANDALONE (no compile-time dependency)
            renderer.setProperty("html-static.standalone", Boolean.TRUE);
        }
        NPath out = NPath.of(target);
        if (f.folder) {
            out = out.mkdirs();
        }
        renderer.setOutput(out);
        renderer.render(document);
    }

    /** zips the deck folder (or single file), skipping build and VCS folders */
    static void zipSources(File source, File zip) throws IOException {
        Path src = source.toPath().toAbsolutePath().normalize();
        Path zipPath = zip.toPath().toAbsolutePath().normalize();
        try (OutputStream os = Files.newOutputStream(zipPath);
             ZipOutputStream zos = new ZipOutputStream(os)) {
            if (Files.isRegularFile(src)) {
                addEntry(zos, src, src.getFileName().toString());
                return;
            }
            String rootName = src.getFileName() == null ? "deck" : src.getFileName().toString();
            Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!dir.equals(src) && SKIPPED_SOURCE_FOLDERS.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toAbsolutePath().normalize().equals(zipPath)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String rel = src.relativize(file).toString().replace(File.separatorChar, '/');
                    addEntry(zos, file, rootName + "/" + rel);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void addEntry(ZipOutputStream zos, Path file, String name) throws IOException {
        ZipEntry e = new ZipEntry(name);
        e.setTime(Files.getLastModifiedTime(file).toMillis());
        zos.putNextEntry(e);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    private void openResult(Format f, File target) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            File toOpen = target;
            if (f == Format.HTML_FOLDER) {
                File index = new File(target, "index.html");
                if (index.isFile()) {
                    toOpen = index;
                }
            } else if (f == Format.NTXP || f == Format.SOURCES_ZIP || f == Format.HTML_ZIP) {
                // archives: reveal the containing folder rather than launching an unknown handler
                toOpen = target.getAbsoluteFile().getParentFile();
            }
            if (toOpen != null) {
                Desktop.getDesktop().open(toOpen);
            }
        } catch (Exception ex) {
            engine.log().log(NMsg.ofC("failed to open %s: %s", target, ex).asFinestFail(ex));
        }
    }
}
