package plan4life.use_case.add_activity;

import java.util.List;

public class AddActivityResponseModel {
    private final String message;
    private final List<String> pendingActivities;

    public AddActivityResponseModel(String message, List<String> pendingActivities) {
        this.message = message;
        this.pendingActivities = pendingActivities;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getPendingActivities() {
        return pendingActivities;
    }
}
