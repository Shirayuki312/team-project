package plan4life.presenter;

import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesResponseModel;
import javax.swing.JOptionPane;

public class SettingsPresenter implements SetPreferencesOutputBoundary {

    @Override
    public void prepareSuccessView(SetPreferencesResponseModel response) {
        JOptionPane.showMessageDialog(null, "Settings Saved Successfully!");
    }

    @Override
    public void prepareFailView(String error) {
        JOptionPane.showMessageDialog(null, "Error: " + error);
    }
}