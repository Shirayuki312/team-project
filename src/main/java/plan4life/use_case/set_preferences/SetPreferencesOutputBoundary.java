package plan4life.use_case.set_preferences;

/**
 * The Output Boundary interface for the Set Preferences use case.
 * This is implemented by the Presenter.
 */
public interface SetPreferencesOutputBoundary {

    // (成功时调用)
    void prepareSuccessView(SetPreferencesResponseModel responseModel);

    // (失败时调用)
    void prepareFailView(String error);
}
