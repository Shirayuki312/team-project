package plan4life.use_case.set_preferences;

import org.junit.jupiter.api.Test;
import plan4life.data_access.UserPreferencesDataAccessInterface;
import plan4life.entities.UserPreferences;

import static org.junit.jupiter.api.Assertions.*;

class SetPreferencesInteractorTest {

    @Test
    void successTest() {
        // 1. Arrange: Create input data for the use case
        SetPreferencesRequestModel inputData = new SetPreferencesRequestModel(
                "Dark", "English", 30, "EST"
        );

        // 2. Mocking the DAO: Verify that the Interactor saves the correct data
        UserPreferencesDataAccessInterface successDAO = new UserPreferencesDataAccessInterface() {
            @Override
            public void save(UserPreferences userPreferences) {
                // Assert that the data passed to the DAO matches the input data
                assertEquals("Dark", userPreferences.getTheme());
                assertEquals("English", userPreferences.getLanguage());
                assertEquals(30, userPreferences.getDefaultReminderMinutes());
                assertEquals("EST", userPreferences.getTimeZoneId());
            }

            @Override
            public UserPreferences load() {
                // Not used in this test, but required by the interface
                return null;
            }
        };

        // 3. Mocking the Presenter: Verify that the success view is prepared
        SetPreferencesOutputBoundary successPresenter = new SetPreferencesOutputBoundary() {
            @Override
            public void prepareSuccessView(SetPreferencesResponseModel responseModel) {
                // Assert that the response model contains the expected output
                assertEquals("Dark", responseModel.getTheme());
                assertEquals("English", responseModel.getLanguage());
            }

            @Override
            public void prepareFailView(String error) {
                // This method should not be called in the success test
                fail("Use case failure is unexpected.");
            }
        };

        // 4. Act: Execute the Interactor
        SetPreferencesInteractor interactor = new SetPreferencesInteractor(successPresenter, successDAO);
        interactor.execute(inputData);
    }

    @Test
    void failureTest() {
        // This test ensures 100% coverage by testing the catch block (failure scenario)

        // 1. Arrange: Create input data
        SetPreferencesRequestModel inputData = new SetPreferencesRequestModel(
                "Dark", "English", 30, "EST"
        );

        // 2. Mocking a failing DAO: Simulate a database exception
        UserPreferencesDataAccessInterface failingDAO = new UserPreferencesDataAccessInterface() {
            @Override
            public void save(UserPreferences userPreferences) {
                throw new RuntimeException("Database error");
            }

            @Override
            public UserPreferences load() {
                // Not used in this test, but required by the interface
                return null;
            }
        };

        // 3. Mocking the Presenter: Verify that the fail view is prepared with the correct error
        SetPreferencesOutputBoundary failurePresenter = new SetPreferencesOutputBoundary() {
            @Override
            public void prepareSuccessView(SetPreferencesResponseModel responseModel) {
                // This method should not be called in the failure test
                fail("Use case success is unexpected.");
            }

            @Override
            public void prepareFailView(String error) {
                // Assert that the error message matches the exception thrown by the DAO
                assertEquals("Database error", error);
            }
        };

        // 4. Act: Execute the Interactor
        SetPreferencesInteractor interactor = new SetPreferencesInteractor(failurePresenter, failingDAO);
        interactor.execute(inputData);
    }
}