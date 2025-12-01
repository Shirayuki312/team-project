package plan4life.use_case.set_preferences;

import plan4life.data_access.UserPreferencesDataAccessInterface;
import plan4life.entities.UserPreferences;

public class SetPreferencesInteractor implements SetPreferencesInputBoundary {

    final SetPreferencesOutputBoundary presenter;
    final UserPreferencesDataAccessInterface dataAccess;

    public SetPreferencesInteractor(SetPreferencesOutputBoundary presenter,
                                    UserPreferencesDataAccessInterface dataAccess) {
        this.presenter = presenter;
        this.dataAccess = dataAccess;
    }

    @Override
    public void execute(SetPreferencesRequestModel requestModel) {
        try {
            UserPreferences preferences = new UserPreferences(
                    requestModel.getTheme(),
                    requestModel.getLanguage(),
                    requestModel.getDefaultReminderMinutes(),
                    requestModel.getTimeZoneId()
            );

            dataAccess.save(preferences);

            SetPreferencesResponseModel responseModel = new SetPreferencesResponseModel(
                    preferences.getTheme(),
                    preferences.getLanguage()
            );

            presenter.prepareSuccessView(responseModel);

        } catch (Exception e) {
            presenter.prepareFailView(e.getMessage());
        }
    }
}