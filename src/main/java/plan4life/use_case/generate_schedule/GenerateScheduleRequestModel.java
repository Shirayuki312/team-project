package plan4life.use_case.generate_schedule;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class GenerateScheduleRequestModel {
    private final String routineDescription;
    private final Map<String, String> fixedActivities;

    public GenerateScheduleRequestModel(String routineDescription, Map<String, String> fixedActivities) {
        this.routineDescription =
                Objects.requireNonNull(routineDescription, "routineDescription must not be null");
        this.fixedActivities =
                fixedActivities == null ? Collections.emptyMap() : Collections.unmodifiableMap(fixedActivities);
    }

    public String getRoutineDescription() {
        return routineDescription;
    }

    // Uses Map to get activities
    public Map<String, String> getFixedActivities() {
        return fixedActivities;
    }
}
