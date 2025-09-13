package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxPageOrientation;
import net.thevpc.nuts.util.NLiteral;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PdfConfigDialog extends JDialog {
    private JRadioButton portraitRadioButton;
    private JRadioButton landscapeRadioButton;
    private JTextField gridXField;
    private JTextField gridYField;
    private JComboBox<String> sizePageComboBox;
    private JCheckBox showPageNumberCheckBox;
    private JCheckBox showFileNameCheckBox;
    private JCheckBox showDateCheckBox;
    private JTextField marginTopField;
    private JTextField marginBottomField;
    private JTextField marginLeftField;
    private JTextField marginRightField;
    private boolean confirmed;
    private Runnable onConfirm;

    public PdfConfigDialog(Frame parent, Runnable onConfirm) {
        super(parent, "PDF Configuration", true);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        this.onConfirm = onConfirm;
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(33, 150, 243));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel titleLabel = new JLabel("PDF Configuration");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(0, 2, 10, 10));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPanel.setBackground(Color.WHITE);

        addLabelAndComponent(contentPanel, "Orientation:", createOrientationPanel());
        addLabelAndComponent(contentPanel, "Grid X:", gridXField = createTextField());
        addLabelAndComponent(contentPanel, "Grid Y:", gridYField = createTextField());
        addLabelAndComponent(contentPanel, "Page Size:", sizePageComboBox = createComboBox(new String[]{"Small (300x300)", "Medium (600x600)", "Large (900x900)", "Max (1200x1200)"}));
        addLabelAndComponent(contentPanel, "Show Page Number:", showPageNumberCheckBox = createCheckBox());
        addLabelAndComponent(contentPanel, "Show File Name:", showFileNameCheckBox = createCheckBox());
        addLabelAndComponent(contentPanel, "Show Date:", showDateCheckBox = createCheckBox());
        addLabelAndComponent(contentPanel, "Margin Top:", marginTopField = createTextField());
        addLabelAndComponent(contentPanel, "Margin Bottom:", marginBottomField = createTextField());
        addLabelAndComponent(contentPanel, "Margin Left:", marginLeftField = createTextField());
        addLabelAndComponent(contentPanel, "Margin Right:", marginRightField = createTextField());

        add(contentPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        footerPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = createButton("OK", new Color(76, 175, 80));
        okButton.addActionListener(e -> {
            if(onConfirm!=null){
                new Thread(()->{
                    onConfirm.run();
                    confirmed = true;
                    SwingUtilities.invokeLater(()->setVisible(false));
                }).start();
            }
        });

        JButton cancelButton = createButton("Cancel", new Color(244, 67, 54));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            setVisible(false);
        });

        footerPanel.add(okButton);
        footerPanel.add(cancelButton);
        add(footerPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void addLabelAndComponent(JPanel panel, String labelText, JComponent component) {
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(33, 150, 243));
        label.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(label);
        panel.add(component);
    }

    private JPanel createOrientationPanel() {
        JPanel orientationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        orientationPanel.setBackground(Color.WHITE);
        ButtonGroup orientationGroup = new ButtonGroup();
        portraitRadioButton = createRadioButton("Portrait", false);
        landscapeRadioButton = createRadioButton("Landscape", true);
        orientationGroup.add(portraitRadioButton);
        orientationGroup.add(landscapeRadioButton);
        orientationPanel.add(portraitRadioButton);
        orientationPanel.add(landscapeRadioButton);
        return orientationPanel;
    }

    private JRadioButton createRadioButton(String text, boolean selected) {
        JRadioButton radioButton = new JRadioButton(text, selected);
        radioButton.setBackground(Color.WHITE);
        radioButton.setForeground(new Color(33, 150, 243));
        radioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        return radioButton;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setBackground(new Color(245, 245, 245));
        textField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        return textField;
    }

    private JComboBox<String> createComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setBackground(new Color(245, 245, 245));
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        return comboBox;
    }

    private JCheckBox createCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBackground(Color.WHITE);
        checkBox.setForeground(new Color(33, 150, 243));
        checkBox.setFont(new Font("Arial", Font.PLAIN, 14));
        return checkBox;
    }

    private JButton createButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(100, 40));
        return button;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public NTxDocumentStreamRendererConfig getConfig() {
        NTxDocumentStreamRendererConfig config = new NTxDocumentStreamRendererConfig();
        config.setOrientation(portraitRadioButton.isSelected() ? NTxPageOrientation.PORTRAIT : NTxPageOrientation.LANDSCAPE);
        config.setGridX(NLiteral.of(gridXField.getText()).asInt().orElse(1));
        config.setGridY(NLiteral.of(gridYField.getText()).asInt().orElse(1));
        switch ((String) sizePageComboBox.getSelectedItem()) {
            case "Small (300x300)":
                config.setPageWidth(300);
                config.setPageHeight(300);
                break;
            case "Medium (600x600)":
                config.setPageWidth(600);
                config.setPageHeight(600);
                break;
            case "Large (900x900)":
                config.setPageWidth(900);
                config.setPageHeight(900);
                break;
            case "Max (1200x1200)":
                config.setPageWidth(1200);
                config.setPageHeight(1200);
                break;
        }
        config.setShowPageNumber(showPageNumberCheckBox.isSelected());
        config.setShowFileName(showFileNameCheckBox.isSelected());
        config.setShowDate(showDateCheckBox.isSelected());
        config.setMarginTop(NLiteral.of(marginTopField.getText()).asFloat().orElse(0f));
        config.setMarginBottom(NLiteral.of(marginBottomField.getText()).asFloat().orElse(0f));
        config.setMarginLeft(NLiteral.of(marginLeftField.getText()).asFloat().orElse(0f));
        config.setMarginRight(NLiteral.of(marginRightField.getText()).asFloat().orElse(0f));
        return config;
    }
}
