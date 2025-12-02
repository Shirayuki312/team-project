package plan4life.use_case.lock_activity;

public class MockLockActivityPresenter implements LockActivityOutputBoundary {

    private LockActivityResponseModel lastResponse;

    @Override
    public void present(LockActivityResponseModel response) {
        this.lastResponse = response;
    }

    public LockActivityResponseModel getLastResponse() {
        return lastResponse;
    }
}
