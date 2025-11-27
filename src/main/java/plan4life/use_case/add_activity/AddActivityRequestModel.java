package plan4life.use_case.add_activity;

public class AddActivityRequestModel {
    private final int scheduleId;
    private final String description;
    private final float duration;

    public AddActivityRequestModel(int scheduleId, String description, float duration) {
        this.scheduleId = scheduleId;
        this.description = description;
        this.duration = duration;
    }

    // getters
    public int getScheduleId() { return scheduleId; }
    public String getDescription() { return description; }
    public float getDuration() { return duration; }
}