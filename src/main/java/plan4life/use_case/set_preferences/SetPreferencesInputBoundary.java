package plan4life.use_case.set_preferences;

/**
 * The Input Boundary interface for the Set Preferences use case.
 * This is implemented by the Interactor.
 */
public interface SetPreferencesInputBoundary {

    void execute(SetPreferencesRequestModel requestModel);
}