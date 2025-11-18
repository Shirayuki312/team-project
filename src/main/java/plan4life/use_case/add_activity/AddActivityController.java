package plan4life.use_case.add_activity;

public class AddActivityController {

    private final AddActivityInputBoundary interactor;

    public AddActivityController(AddActivityInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void addActivity(String name, float durationHours, String tagName) {
        AddActivityRequestModel request = new AddActivityRequestModel(name, durationHours, tagName);
        interactor.execute(request);
    }
}
