package plan4life.controller;

import plan4life.view.SettingsView;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;
// [关键修复] 下面这一行是你缺失的 import
import plan4life.use_case.set_preferences.SetPreferencesRequestModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsController {

    private final SettingsView settingsView;
    private final SetPreferencesInputBoundary interactor;

    public SettingsController(SettingsView view, SetPreferencesInputBoundary interactor) {
        this.settingsView = view;
        this.interactor = interactor;
        initController();
    }

    private void initController() {
        settingsView.getSaveButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
            }
        });

        settingsView.getCancelButton().addActionListener(e -> settingsView.dispose());
    }

    private void saveSettings() {
        String theme = settingsView.getLightModeRadio().isSelected() ? "Light Mode" : "Dark Mode";
        String language = (String) settingsView.getLanguageCombo().getSelectedItem();
        String timeZone = (String) settingsView.getTimezoneCombo().getSelectedItem();

        // time
        String reminderStr = (String) settingsView.getReminderCombo().getSelectedItem();
        int reminderMinutes = 15;
        if (reminderStr != null) {
            if (reminderStr.contains("30")) reminderMinutes = 30;
            if (reminderStr.contains("1 hour")) reminderMinutes = 60;
        }

        SetPreferencesRequestModel request = new SetPreferencesRequestModel(
                theme, language, reminderMinutes, timeZone
        );

        // 3.Use Case
        interactor.execute(request);

        settingsView.dispose();
    }
}