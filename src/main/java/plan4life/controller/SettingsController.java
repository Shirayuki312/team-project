package plan4life.controller;

import plan4life.view.SettingsView;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesRequestModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SettingsController {

    private final SettingsView settingsView;
    private final SetPreferencesInputBoundary interactor;

    // [关键修复] 构造函数现在必须接收 2 个参数：View 和 Interactor
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
        try {
            // 1. 从 View 获取数据
            String theme = settingsView.getLightModeRadio().isSelected() ? "Light Mode" : "Dark Mode";
            String language = (String) settingsView.getLanguageCombo().getSelectedItem();
            String timeZone = (String) settingsView.getTimezoneCombo().getSelectedItem();

            // 解析提醒时间
            String reminderStr = (String) settingsView.getReminderCombo().getSelectedItem();
            int reminderMinutes = 15;
            if (reminderStr != null) {
                if (reminderStr.contains("30")) reminderMinutes = 30;
                if (reminderStr.contains("1 hour")) reminderMinutes = 60;
            }

            // 2. 创建 RequestModel
            SetPreferencesRequestModel request = new SetPreferencesRequestModel(
                    theme, language, reminderMinutes, timeZone
            );

            // 3. 调用 Use Case
            if (interactor != null) {
                interactor.execute(request);
            }

            // 4. 关闭窗口
            settingsView.dispose();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}