package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;

public class GenerateScheduleResponseModel {
    private final Schedule schedule;
    private final String message;

    public GenerateScheduleResponseModel(Schedule schedule) {
        this(schedule, null);
    }

    public GenerateScheduleResponseModel(Schedule schedule, String message) {
        this.schedule = schedule;
        this.message = message;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public String getMessage() {
        return message;
    }
}