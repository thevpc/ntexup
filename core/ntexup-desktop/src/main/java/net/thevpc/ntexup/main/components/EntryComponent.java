package net.thevpc.ntexup.main.components;

import net.thevpc.ntexup.config.NTxProject;
import net.thevpc.ntexup.main.NTxServiceHelper;
import net.thevpc.nuts.io.NPath;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EntryComponent extends JPanel {
    private final NTxServiceHelper serviceHelper;
    private final JList<NTxProject> recentFilesComponent;
    private final DefaultListModel<NTxProject> recentFilesComponentModel;
    private final JTextField searchField;
    private final List<NTxProject> allRecentProjects = new ArrayList<>();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public EntryComponent(NTxServiceHelper serviceHelper) {
        super(new BorderLayout(16, 16));
        this.serviceHelper = serviceHelper;
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headerPanel = createHeader();
        add(headerPanel, BorderLayout.NORTH);

        // Center split
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        // Left Action Buttons
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.35;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 12);
        centerPanel.add(createActionsPanel(), gbc);

        // Right Recent Files
        gbc.gridx = 1;
        gbc.weightx = 0.65;
        gbc.insets = new Insets(0, 0, 0, 0);

        recentFilesComponentModel = new DefaultListModel<>();
        recentFilesComponent = new JList<>(recentFilesComponentModel);
        recentFilesComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recentFilesComponent.setCellRenderer(new RecentProjectCellRenderer());

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search recent presentations...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterRecentFiles(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterRecentFiles(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterRecentFiles(); }
        });

        setupListInteractions();

        centerPanel.add(createRecentPanel(), gbc);
        add(centerPanel, BorderLayout.CENTER);

        reload();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor") != null ? UIManager.getColor("Component.borderColor") : Color.LIGHT_GRAY),
                new EmptyBorder(0, 0, 12, 0)
        ));

        JLabel titleLabel = new JLabel("ntexup");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(UIManager.getColor("Label.foreground"));

        JLabel subtitleLabel = new JLabel("Declarative Document & Presentation Generator");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(0x64748b));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel createActionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Quick Actions"));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(8, 8, 6, 8);

        JButton newProjectBtn = createActionButton("➕  New Project...", "Create a new presentation from a template");
        newProjectBtn.addActionListener(e -> {
            serviceHelper.showNewProject();
            reload();
        });
        panel.add(newProjectBtn, g);

        g.gridy = 1;
        JButton openFileBtn = createActionButton("📂  Open Presentation...", "Open an existing .ntx presentation or project folder");
        openFileBtn.addActionListener(e -> {
            serviceHelper.showOpenFile();
            reload();
        });
        panel.add(openFileBtn, g);

        g.gridy = 2;
        JButton showDebugBtn = createActionButton("⚙  Debug & Logs", "View compiler logs and diagnostics");
        showDebugBtn.addActionListener(e -> serviceHelper.showDebug());
        panel.add(showDebugBtn, g);

        // Filler
        g.gridy = 3;
        g.weighty = 1.0;
        g.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), g);

        return panel;
    }

    private JButton createActionButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setMargin(new Insets(8, 12, 8, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel createRecentPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Recent Presentations"));

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.add(searchField, BorderLayout.CENTER);

        JButton clearFilterBtn = new JButton("Clear");
        clearFilterBtn.addActionListener(e -> searchField.setText(""));
        topBar.add(clearFilterBtn, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(recentFilesComponent);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor") != null ? UIManager.getColor("Component.borderColor") : Color.LIGHT_GRAY));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void setupListInteractions() {
        recentFilesComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    openSelectedProject();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    int index = recentFilesComponent.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        recentFilesComponent.setSelectedIndex(index);
                        showContextMenu(e);
                    }
                }
            }
        });

        recentFilesComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openSelectedProject();
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedProject();
                }
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        NTxProject selected = recentFilesComponent.getSelectedValue();
        if (selected == null) return;

        JPopupMenu menu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(ev -> openSelectedProject());
        menu.add(openItem);

        NPath path = NPath.of(selected.getPath());
        File file = path.toFile().orNull();
        if (file != null && file.exists()) {
            JMenuItem explorerItem = new JMenuItem(file.isDirectory() ? "Open Folder in Explorer" : "Show in Explorer");
            explorerItem.addActionListener(ev -> {
                try {
                    Desktop.getDesktop().open(file.isDirectory() ? file : file.getParentFile());
                } catch (IOException ex) {
                    // ignore
                }
            });
            menu.add(explorerItem);
        }

        menu.addSeparator();
        JMenuItem removeItem = new JMenuItem("Remove from History");
        removeItem.addActionListener(ev -> removeSelectedProject());
        menu.add(removeItem);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void openSelectedProject() {
        NTxProject selected = recentFilesComponent.getSelectedValue();
        if (selected != null) {
            serviceHelper.openProject(NPath.of(selected.getPath()));
            reload();
        }
    }

    private void removeSelectedProject() {
        NTxProject selected = recentFilesComponent.getSelectedValue();
        if (selected != null) {
            serviceHelper.removeRecentProject(NPath.of(selected.getPath()));
            reload();
        }
    }

    public void reload() {
        allRecentProjects.clear();
        for (NTxProject p : serviceHelper.config().getRecentProjects()) {
            if (p != null && p.getPath() != null) {
                allRecentProjects.add(p);
            }
        }
        filterRecentFiles();
    }

    private void filterRecentFiles() {
        String filter = searchField.getText().trim().toLowerCase();
        DefaultListModel<NTxProject> model = new DefaultListModel<>();
        for (NTxProject p : allRecentProjects) {
            if (filter.isEmpty()) {
                model.addElement(p);
            } else {
                NPath path = NPath.of(p.getPath());
                if (path.name().toLowerCase().contains(filter) || p.getPath().toLowerCase().contains(filter)) {
                    model.addElement(p);
                }
            }
        }
        recentFilesComponent.setModel(model);
    }

    private static class RecentProjectCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof NTxProject) {
                NTxProject pr = (NTxProject) value;
                NPath p = NPath.of(pr.getPath());
                boolean exists = p.exists();
                String name = p.name();
                String pathStr = pr.getPath();
                Date lastAccess = pr.getLastAccess();
                String dateStr = lastAccess != null ? DATE_FORMAT.format(lastAccess) : "";

                Color titleColor = isSelected ? list.getSelectionForeground() : (exists ? new Color(0x1e293b) : Color.GRAY);
                Color pathColor = isSelected ? list.getSelectionForeground() : new Color(0x64748b);
                Color dateColor = isSelected ? list.getSelectionForeground() : new Color(0x94a3b8);

                String html = String.format(
                        "<html><body style='padding: 3px 0;'>" +
                        "<div style='font-size: 13px; font-weight: bold; color: %s;'>%s %s</div>" +
                        "<div style='font-size: 10px; color: %s;'>%s &bull; <span style='color: %s;'>%s</span></div>" +
                        "</body></html>",
                        toHexColor(titleColor),
                        exists ? "📄" : "⚠️",
                        name,
                        toHexColor(pathColor),
                        pathStr,
                        toHexColor(dateColor),
                        dateStr.isEmpty() ? "" : "Last accessed: " + dateStr
                );
                setText(html);
                setEnabled(exists);
            }
            setBorder(new EmptyBorder(4, 8, 4, 8));
            return this;
        }

        private static String toHexColor(Color c) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }
    }
}
