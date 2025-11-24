package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;
import java.util.Objects;

public class GenerateScheduleResponseModel {
    private final Schedule schedule;

    public GenerateScheduleResponseModel(Schedule schedule) {
        this.schedule = Objects.requireNonNull(schedule, "schedule must not be null.");
    }

    public Schedule getSchedule() {
        return schedule;
    }
}
