package plan4life.view;

import javax.swing.*;
import java.awt.*;

/**
 * The settings dialog window (JDialog).
 */
public class SettingsView extends JDialog {

    private JComboBox<String> reminderCombo;
    private JRadioButton lightModeRadio;
    private JRadioButton darkModeRadio;
    private JComboBox<String> timezoneCombo;
    private JComboBox<String> languageCombo;
    private JButton saveButton;
    private JButton cancelButton;

    /**
     * Constructor for the SettingsView.
     * @param owner The parent Frame (our CalendarFrame) that this dialog belongs to.
     */
    public SettingsView(Frame owner) {
        // Call the super constructor (JDialog)
        // title: "Settings"
        // modal: 'true' (blocks interaction with the parent window while open)
        super(owner, "Settings", true);

        // Basic dialog setup
        setLayout(new BorderLayout());
        setSize(400, 300);
        setLocationRelativeTo(owner); // Open in the center
        setLocationRelativeTo(owner); // Open in the center of the parent window

        // --- 1. Center Form Panel ---
        // This panel holds all the labels and input fields.
        // GridBagLayout is complex but powerful for forms.
        JPanel formPanel = new JPanel(new GridBagLayout());
        // GridBagConstraints control component placement and size
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10); // 5px top/bottom, 10px left/right padding
        gbc.anchor = GridBagConstraints.WEST; // Align components to the left

        // --- Default Reminder ---
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridx = 0; // Column 0
        gbc.gridy = 0; // Row 0
        formPanel.add(new JLabel("Default Reminder:"), gbc);
        gbc.gridx = 1;

        gbc.gridx = 1; // Column 1
        String[] reminderOptions = {"15 minutes before", "30 minutes before", "1 hour before"};
        reminderCombo = new JComboBox<>(reminderOptions);
        formPanel.add(reminderCombo, gbc);

        // --- Theme (Light/Dark Mode) ---
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridx = 0; // Column 0
        gbc.gridy = 1; // Row 1
        formPanel.add(new JLabel("Theme:"), gbc);
        gbc.gridx = 1;

        gbc.gridx = 1; // Column 1
        lightModeRadio = new JRadioButton("Light Mode");
        darkModeRadio = new JRadioButton("Dark Mode");
        lightModeRadio.setSelected(true);
        lightModeRadio.setSelected(true); // Default to light mode
        // ButtonGroup ensures only one radio button (light or dark) can be selected
        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(lightModeRadio);
        themeGroup.add(darkModeRadio);
        // Use a sub-panel to keep the radio buttons together
        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        themePanel.add(lightModeRadio);
        themePanel.add(darkModeRadio);
        formPanel.add(themePanel, gbc);

        // --- Time Zone ---
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridx = 0; // Column 0
        gbc.gridy = 2; // Row 2
        formPanel.add(new JLabel("Time Zone:"), gbc);
        gbc.gridx = 1;

        gbc.gridx = 1; // Column 1
        String[] timezoneOptions = {"America/Toronto", "Asia/Shanghai", "Europe/London"};
        timezoneCombo = new JComboBox<>(timezoneOptions);
        formPanel.add(timezoneCombo, gbc);

        // --- Language ---
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Language:"), gbc);
        gbc.gridx = 1;
        String[] langOptions = {"English", "简体中文"};
        languageCombo = new JComboBox<>(langOptions);
        formPanel.add(languageCombo, gbc);

        // --- 2. Bottom Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // --- 3. Add Panels to Dialog ---
        add(formPanel, BorderLayout.CENTER); // Form in the middle
        add(buttonPanel, BorderLayout.SOUTH); // Buttons at the bottom
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // --- Getters ---
    public JButton getSaveButton() { return saveButton; }
    public JButton getCancelButton() { return cancelButton; }
    public JComboBox<String> getReminderCombo() { return reminderCombo; }
    public JRadioButton getLightModeRadio() { return lightModeRadio; }
    public JRadioButton getDarkModeRadio() { return darkModeRadio; }
    public JComboBox<String> getTimezoneCombo() { return timezoneCombo; }
    public JComboBox<String> getLanguageCombo() { return languageCombo; }
}