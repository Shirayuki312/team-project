package plan4life.use_case.add_activity;

public class AddActivityController {
    private final AddActivityInputBoundary interactor;

    public AddActivityController(AddActivityInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void addActivity(int scheduleId, String description, float duration) {
        AddActivityRequestModel request = new AddActivityRequestModel(scheduleId, description, duration);
        interactor.execute(request);
    }
}