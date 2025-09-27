package net.thevpc.ntexup.main.components;

import net.thevpc.ntexup.api.engine.NTxTemplateInfo;
import net.thevpc.ntexup.config.UserConfig;
import net.thevpc.ntexup.main.NTxUIHelper;
import net.thevpc.ntexup.main.NTxServiceHelper;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NStringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class NewProjectPanel extends JPanel {
    JTextField projectName;
    PathField rootFolder;
    NTxServiceHelper serviceHelper;
    NewProjectTemplatePanel templatePanel;
    NewProjectPropsPanel props;
    UserConfigPanel userConfPanel;

    public NewProjectPanel(NTxServiceHelper serviceHelper) {
        super(new GridBagLayout());
        this.serviceHelper = serviceHelper;

        add(new JLabel("Project Name"), NTxUIHelper.forLabel(0, 0));
        add(projectName = new JTextField(), NTxUIHelper.forEditor(1, 0));
        projectName.setText("new-project");

        add(new JLabel("Root Folder"), NTxUIHelper.forLabel(0, 1));
        add(rootFolder = new PathField(), NTxUIHelper.forEditor(1, 1));
        rootFolder.path.setText(System.getProperty("user.dir"));
        {
            templatePanel = new NewProjectTemplatePanel(serviceHelper);
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 3;
            g.weightx = 4;
            g.weighty = 2;
            g.gridwidth = 2;
            g.gridheight = 2;
            g.fill = GridBagConstraints.BOTH;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(2, 2, 2, 2);
            templatePanel.setBorder(BorderFactory.createTitledBorder("Template Properties"));
            add(templatePanel, g);
        }
        {
            props = new NewProjectPropsPanel(serviceHelper);
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 5;
            g.weightx = 4;
            g.weighty = 2;
            g.gridwidth = 2;
            g.gridheight = 2;
            g.fill = GridBagConstraints.BOTH;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(2, 2, 2, 2);
            props.setBorder(BorderFactory.createTitledBorder("Document Properties"));
            add(props, g);
        }
        {
            userConfPanel = new UserConfigPanel(serviceHelper.usersConfig().loadUserConfigs());
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 7;
            g.weightx = 4;
            g.weighty = 2;
            g.gridwidth = 2;
            g.gridheight = 2;
            g.fill = GridBagConstraints.BOTH;
            g.anchor = GridBagConstraints.WEST;
            g.insets = new Insets(2, 2, 2, 2);
            userConfPanel.setBorder(BorderFactory.createTitledBorder("Author Properties"));
            add(userConfPanel, g);
        }
    }

    public UserConfig getUserConfig() {
        return userConfPanel.getUserConfig();
    }

    public Map<String, String> getPropValues() {
        Map<String, String> propValues = props.getPropValues();
        UserConfig userConfig = getUserConfig();
        propValues.put("template.username", userConfig.getUsername());
        propValues.put("template.fullName", userConfig.getFullName());
        propValues.put("template.author", userConfig.getFullName());
        propValues.put("template.firstName", userConfig.getFirstName());
        propValues.put("template.lastName", userConfig.getLastName());
        propValues.put("template.emailAddress", userConfig.getEmailAddress());
        propValues.put("template.affiliation", userConfig.getAffiliation());
        return propValues;
    }


    public String getSelectedProjectName() {
        return projectName.getText();
    }


    public void setSelectedRootFolder(String a) {
        rootFolder.path.setText(NStringUtils.firstNonBlank(a, System.getProperty("user.dir")));
    }

    public String getSelectedRootFolder() {
        return rootFolder.path.getText();
    }

    public NTxTemplateInfo getSelectedTemplate() {
        return templatePanel.getSelectedTemplate();
    }

    public boolean showDialog(Component parent) {
        int i = JOptionPane.showOptionDialog(parent, this, "New Project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        return i == JOptionPane.OK_OPTION;
    }
}
