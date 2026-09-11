package net.thevpc.ntexup.main.components;

import net.thevpc.ntexup.config.UserConfig;
import net.thevpc.ntexup.config.UserConfigs;
import net.thevpc.ntexup.main.NTxUIHelper;
import net.thevpc.nuts.util.NStringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserConfigPanel extends JPanel {
    JComboBox profileId = new JComboBox();
    JTextField username = new JTextField();
    JTextField emailAddress = new JTextField();
    JTextField fistName = new JTextField();
    JTextField lastName = new JTextField();
    JTextField affiliation = new JTextField();
    JTextField fullName = new JTextField();
    private UserConfigs configs;

    public UserConfigPanel(UserConfigs configs) {
        super(new GridBagLayout());
        this.configs = configs;
        int row = 0;

        profileId.setEditable(true);
        profileId.setModel(new DefaultComboBoxModel(Arrays.stream(configs.getUsers()).map(x -> x.getId()).toArray()));

        add(new JLabel("Profile"), NTxUIHelper.forLabel(0, row));
        add(profileId, NTxUIHelper.forEditor(1, row));
        row++;

        add(new JLabel("Full Name"), NTxUIHelper.forLabel(0, row));
        add(fullName, NTxUIHelper.forEditor(1, row));
        row++;

        add(new JLabel("Email"), NTxUIHelper.forLabel(0, row));
        add(emailAddress, NTxUIHelper.forEditor(1, row));
        row++;

        add(new JLabel("Affiliation"), NTxUIHelper.forLabel(0, row));
        add(affiliation, NTxUIHelper.forEditor(1, row));
        row++;

        add(new JLabel("Username"), NTxUIHelper.forLabel(0, row));
        add(username, NTxUIHelper.forEditor(1, row));
        row++;

        add(new JLabel("First Name"), NTxUIHelper.forLabel(0, row));
        add(fistName, NTxUIHelper.forEditor(1, row));
        row++;

        add(new JLabel("Last Name"), NTxUIHelper.forLabel(0, row));
        add(lastName, NTxUIHelper.forEditor(1, row));
        row++;
        profileId.setSelectedItem(NStringUtils.firstNonBlank(configs.getMain(), "default"));
        updateProps();
        profileId.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateProps();
            }
        });
    }

    private void updateProps() {
        Object u = profileId.getSelectedItem();
        UserConfig t = configs.findUser((String) u);
        if (t == null) {
            t = new UserConfig();
        }
        UserConfig finalT = t;
        SwingUtilities.invokeLater(() -> {
            fullName.setText(NStringUtils.firstNonBlank(finalT.getFullName(),System.getProperty("user.name")));
            affiliation.setText(finalT.getAffiliation());
            username.setText(NStringUtils.firstNonBlank(finalT.getUsername(),System.getProperty("user.name")));
            emailAddress.setText(NStringUtils.firstNonBlank(finalT.getEmailAddress(),username.getText()+"@email.com"));
            fistName.setText(NStringUtils.firstNonBlank(finalT.getFirstName(),extractFirstAndLastName(fullName.getText())[0]));
            lastName.setText(NStringUtils.firstNonBlank(finalT.getLastName(),extractFirstAndLastName(fullName.getText())[1]));
        });
    }

    private String[] extractFirstAndLastName(String a) {
        if(a!=null){
            a=NStringUtils.strip(a);
            int i = a.indexOf(' ');
            if(i>=0){
                return new String[]{a.substring(0, i),a.substring(i+1)};
            }
            return new String[]{a,""};
        }
        return new String[]{"",""};
    }
    public UserConfig getUserConfig() {
        Map<String, String> m = new LinkedHashMap<>();
        Object u = profileId.getSelectedItem();
        UserConfig t = new UserConfig();
        t.setId((String) u);
        t.setFirstName(fistName.getText());
        t.setLastName(lastName.getText());
        t.setFullName(fullName.getText());
        t.setAffiliation(affiliation.getText());
        t.setUsername(username.getText());
        t.setEmailAddress(emailAddress.getText());
        return t;
    }
}
