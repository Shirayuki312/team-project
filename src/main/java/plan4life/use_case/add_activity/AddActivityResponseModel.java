package plan4life.use_case.add_activity;

import plan4life.entities.Activity;
import plan4life.entities.Schedule;

import java.util.List;

public class AddActivityResponseModel {
    private final boolean success;
    private final String message;
    private final List<Activity> updatedActivities;
    private final Schedule updatedSchedule;

    public AddActivityResponseModel(boolean success, String message, List<Activity> updatedActivities, Schedule updatedSchedule) {
        this.success = success;
        this.message = message;
        this.updatedActivities = updatedActivities;
        this.updatedSchedule = updatedSchedule;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Activity> getUpdatedActivities() { return updatedActivities; }
    public Schedule getUpdatedSchedule() { return updatedSchedule; }
}