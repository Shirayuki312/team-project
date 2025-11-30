package plan4life.use_case.block_off_time;

public class MockBlockOffTimePresenter implements BlockOffTimeOutputBoundary {

    private BlockOffTimeResponseModel lastResponse;

    @Override
    public void present(BlockOffTimeResponseModel response) {
        this.lastResponse = response;
    }

    public BlockOffTimeResponseModel getLastResponse() {
        return lastResponse;
    }
}

