package net.thevpc.ntexup.main.components;

import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.main.NTxServiceHelper;
import net.thevpc.ntexup.main.NTxUIHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class NewProjectTemplatePanel extends JPanel {
    JComboBox templateRepo;
    JComboBox templateName;
    JComboBox templateLayout;
    JComboBox templateVersion;
    NTxServiceHelper serviceHelper;
    NTxTemplateInfo[] templates;
    DefaultComboBoxModel templateRepos = new DefaultComboBoxModel();
    DefaultComboBoxModel templateNames = new DefaultComboBoxModel();
    DefaultComboBoxModel templateLayouts = new DefaultComboBoxModel();
    DefaultComboBoxModel templateVersions = new DefaultComboBoxModel();

    public NewProjectTemplatePanel(NTxServiceHelper serviceHelper) {
        super(new GridBagLayout());
        this.serviceHelper = serviceHelper;
        int row = 0;
        add(new JLabel("Repository"), NTxUIHelper.forLabel(0, row));
        add(templateRepo = new JComboBox<>(), NTxUIHelper.forEditor(1, row));
        row++;
        add(new JLabel("Name"), NTxUIHelper.forLabel(0, row));
        add(templateName = new JComboBox<>(), NTxUIHelper.forEditor(1, row));
        row++;
        add(new JLabel("Layout"), NTxUIHelper.forLabel(0, row));
        add(templateLayout = new JComboBox<>(), NTxUIHelper.forEditor(1, row));
        row++;
        add(new JLabel("Version"), NTxUIHelper.forLabel(0, row));
        add(templateVersion = new JComboBox<>(), NTxUIHelper.forEditor(1, row));
        row++;
        templateRepo.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                updateTemplateNames();
            }
        });
        templateName.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                updateTemplateLayouts();
                updateTemplateVersions();
            }
        });

        templateRepo.setModel(templateRepos);
        templateName.setModel(templateNames);
        templateLayout.setModel(templateLayouts);
        templateVersion.setModel(templateVersions);

        updateTemplates();
    }

    private void updateTemplates() {
        templates = serviceHelper.engine().getTemplates();
        templateRepos.removeAllElements();
        for (String value : Arrays.stream(templates).map(x -> x.repoName()).distinct().sorted().collect(Collectors.toList())) {
            templateRepos.addElement(value);
        }

        templateNames.removeAllElements();
        for (NTxTemplateInfo defaultTemplateUrl : templates) {
            templateNames.addElement(defaultTemplateUrl.name());
        }
        updateTemplateNames();
    }

    private void updateTemplateNames() {
        templateNames.removeAllElements();
        String selectedRepo = (String) templateRepos.getSelectedItem();
        for (String value : Arrays.stream(templates)
                .filter(x -> Objects.equals(selectedRepo, x.repoName()))
                .map(x -> x.name())
                .distinct().sorted().collect(Collectors.toList())) {
            templateNames.addElement(value);
        }
        updateTemplateVersions();
        updateTemplateLayouts();
    }

    private void updateTemplateVersions() {
        templateVersions.removeAllElements();
        String selectedRepo = (String) templateRepos.getSelectedItem();
        String selectedName = (String) templateNames.getSelectedItem();
        for (String value : Arrays.stream(templates)
                .filter(x -> Objects.equals(selectedRepo, x.repoName()) && Objects.equals(selectedName, x.name()))
                .map(x -> x.version())
                .distinct().sorted().collect(Collectors.toList())) {
            templateVersions.addElement(value);
        }
    }

    private void updateTemplateLayouts() {
        templateLayouts.removeAllElements();
        String selectedRepo = (String) templateRepos.getSelectedItem();
        String selectedName = (String) templateNames.getSelectedItem();
        for (String value : Arrays.stream(templates)
                .filter(x -> Objects.equals(selectedRepo, x.repoName()) && Objects.equals(selectedName, x.name()))
                .map(x -> x.layout())
                .distinct().sorted().collect(Collectors.toList())) {
            templateLayouts.addElement(value);
        }
    }

    public NTxTemplateInfo getSelectedTemplate() {
        if(templates==null){
            return null;
        }
        String selectedRepo = (String) templateRepos.getSelectedItem();
        String selectedName = (String) templateName.getSelectedItem();
        String selectedLayout = (String) templateLayout.getSelectedItem();
        String selectedVersion = (String) templateVersion.getSelectedItem();

        List<NTxTemplateInfo> found = Arrays.stream(templates)
                .filter(x -> Objects.equals(selectedRepo, x.repoName())
                        && Objects.equals(selectedName, x.name())
                        && Objects.equals(selectedLayout, x.layout())
                        && Objects.equals(selectedVersion, x.version())
                ).collect(Collectors.toList());
        return found.isEmpty()?null:found.get(0);
    }


}
