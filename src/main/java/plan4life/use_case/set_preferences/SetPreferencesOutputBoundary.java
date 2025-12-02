package plan4life.use_case.set_preferences;

/**
 * The Output Boundary interface for the Set Preferences use case.
 * This is implemented by the Presenter.
 */
public interface SetPreferencesOutputBoundary {

    void prepareSuccessView(SetPreferencesResponseModel responseModel);

    void prepareFailView(String error);
}
