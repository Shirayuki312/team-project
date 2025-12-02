package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;
import java.util.Objects;

public class GenerateScheduleResponseModel {
    private final Schedule schedule;
    private final String message;

    public GenerateScheduleResponseModel(Schedule schedule) {
        this(schedule, null);
    }

    public GenerateScheduleResponseModel(Schedule schedule, String message) {
        this.schedule = schedule;
        this.message = message;
        this.schedule = Objects.requireNonNull(schedule, "schedule must not be null.");
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public String getMessage() {
        return message;
    }
}
}
