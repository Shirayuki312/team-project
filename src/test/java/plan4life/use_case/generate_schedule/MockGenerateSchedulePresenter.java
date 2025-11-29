package plan4life.use_case.generate_schedule;

public class MockGenerateSchedulePresenter implements GenerateScheduleOutputBoundary {

    private GenerateScheduleResponseModel lastResponse;

    @Override
    public void present(GenerateScheduleResponseModel response) {
        this.lastResponse = response;
    }

    public GenerateScheduleResponseModel getLastResponse() {
        return lastResponse;
    }
}

