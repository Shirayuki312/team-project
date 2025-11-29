package plan4life.use_case.add_activity;

public class MockAddActivityPresenter implements AddActivityOutputBoundary {

    private AddActivityResponseModel lastResponse;

    @Override
    public void present(AddActivityResponseModel response) {
        this.lastResponse = response;
    }

    public AddActivityResponseModel getLastResponse() {
        return lastResponse;
    }
}