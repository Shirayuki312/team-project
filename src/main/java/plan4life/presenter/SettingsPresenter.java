package plan4life.presenter;

import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesResponseModel;
import plan4life.view.CalendarViewInterface;

import javax.swing.*;

public class SettingsPresenter implements SetPreferencesOutputBoundary {

    private CalendarViewInterface view;

    public void setView(CalendarViewInterface view) {
        this.view = view;
    }

    @Override
    public void prepareSuccessView(SetPreferencesResponseModel response) {
        // 1. success！
        JOptionPane.showMessageDialog(null, "Settings Saved!");

        // 2. change color and lauguage
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