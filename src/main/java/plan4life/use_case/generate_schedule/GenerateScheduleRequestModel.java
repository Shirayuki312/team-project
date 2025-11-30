package plan4life.use_case.generate_schedule;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.List;


public class GenerateScheduleRequestModel {
    private final String routineDescription;
    private final Map<String, String> fixedActivities;
    private final List<String> freeActivities;


    public GenerateScheduleRequestModel(String routineDescription,
                                        Map<String, String> fixedActivities,
                                        List<String> freeActivities) {
        this.routineDescription =
                Objects.requireNonNull(routineDescription, "routineDescription must not be null");
        this.fixedActivities =
                fixedActivities == null ? Collections.emptyMap() : Collections.unmodifiableMap(fixedActivities);
        this.freeActivities =
                freeActivities == null ? Collections.emptyList() : Collections.unmodifiableList(freeActivities);
    }

    public String getRoutineDescription() {
        return routineDescription;
    }

    public List<String> getFreeActivities() {
        return freeActivities;
    }

    // Uses Map to get activities
    public Map<String, String> getFixedActivities() {
        return fixedActivities;
    }
}
