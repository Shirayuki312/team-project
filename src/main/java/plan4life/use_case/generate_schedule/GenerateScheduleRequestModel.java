package plan4life.use_case.generate_schedule;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Request model for generating a schedule.
 *
 * fixedActivities:
 *    String with one fixed activity per line in the form:
 *       "Mon 09:00-10:00 60 Gym"
 *
 * routineDescription:
 *    Free-form user text describing lifestyle and constraints.
 */
public class GenerateScheduleRequestModel {

    private final String routineDescription;
    private final String fixedActivities;
    private final List<String> freeActivities;

    public GenerateScheduleRequestModel(String routineDescription,
                                        String fixedActivities,
                                        List<String> freeActivities) {
        this.routineDescription = Objects.requireNonNull(routineDescription, "routineDescription must not be null");
        this.fixedActivities = fixedActivities == null ? "" : fixedActivities;
        this.freeActivities = freeActivities == null ? Collections.emptyList() : Collections.unmodifiableList(freeActivities);
    }

    public String getRoutineDescription() {
        return routineDescription;
    }

    public List<String> getFreeActivities() {
        return freeActivities;
    }

    public String getFixedActivities() {
        return fixedActivities;
    }
}