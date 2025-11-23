package plan4life.view;

import javax.swing.*;
import java.awt.*;

public class SettingsView extends JDialog {

    private JComboBox<String> reminderCombo;
    private JRadioButton lightModeRadio;
    private JRadioButton darkModeRadio;
    private JComboBox<String> timezoneCombo;
    private JComboBox<String> languageCombo;
    private JButton saveButton;
    private JButton cancelButton;

    public SettingsView(Frame owner) {
        super(owner, "Settings", true);

        setLayout(new BorderLayout());
        setSize(400, 300);
        setLocationRelativeTo(owner);

        // --- Form Panel ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Default Reminder
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Default Reminder:"), gbc);
        gbc.gridx = 1;
        String[] reminderOptions = {"15 minutes before", "30 minutes before", "1 hour before"};
        reminderCombo = new JComboBox<>(reminderOptions);
        formPanel.add(reminderCombo, gbc);

        // Theme
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Theme:"), gbc);
        gbc.gridx = 1;
        lightModeRadio = new JRadioButton("Light Mode");
        darkModeRadio = new JRadioButton("Dark Mode");
        lightModeRadio.setSelected(true);
        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightModeRadio);
        themeGroup.add(darkModeRadio);
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        themePanel.add(lightModeRadio);
        themePanel.add(darkModeRadio);
        formPanel.add(themePanel, gbc);

        // Time Zone
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Time Zone:"), gbc);
        gbc.gridx = 1;
        String[] timezoneOptions = {"America/Toronto", "Asia/Shanghai", "Europe/London"};
        timezoneCombo = new JComboBox<>(timezoneOptions);
        formPanel.add(timezoneCombo, gbc);

        // Language
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Language:"), gbc);
        gbc.gridx = 1;
        String[] langOptions = {"English", "简体中文", "Français"};
        languageCombo = new JComboBox<>(langOptions);
        formPanel.add(languageCombo, gbc);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public JButton getSaveButton() { return saveButton; }
    public JButton getCancelButton() { return cancelButton; }
    public JComboBox<String> getReminderCombo() { return reminderCombo; }
    public JRadioButton getLightModeRadio() { return lightModeRadio; }
    public JRadioButton getDarkModeRadio() { return darkModeRadio; }
    public JComboBox<String> getTimezoneCombo() { return timezoneCombo; }
    public JComboBox<String> getLanguageCombo() { return languageCombo; }
}