package plan4life.presenter;

import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;
// [修复] 下面这一行是你缺失的 import
import plan4life.use_case.set_preferences.SetPreferencesResponseModel;
import plan4life.view.CalendarViewInterface;

import javax.swing.*;

public class SettingsPresenter implements SetPreferencesOutputBoundary {

    private CalendarViewInterface view;

    // Main.java 需要调用这个方法把 View 传进来
    public void setView(CalendarViewInterface view) {
        this.view = view;
    }

    @Override
    public void prepareSuccessView(SetPreferencesResponseModel response) {
        // 1. 弹窗提示成功
        JOptionPane.showMessageDialog(null, "Settings Saved!");

        // 2. 通知 View 改变颜色和语言
        if (view != null) {
            view.updateLanguage(response.getLanguage());
            view.updateTheme(response.getTheme());
        }
    }

    @Override
    public void prepareFailView(String error) {
        JOptionPane.showMessageDialog(null, "Error: " + error);
    }
}