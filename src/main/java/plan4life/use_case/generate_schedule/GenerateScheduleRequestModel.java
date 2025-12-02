package plan4life.use_case.generate_schedule;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.List;

/**
 * Request model for generating a schedule.
 *
 * freeActivities:
 *    List of activity strings in the form:
 *       "Description:Duration"
 *    Example:
 *       "Gym:1.5"
 *       "Study:2"
 *
 * fixedActivities:
 *    Map<String,String> where:
 *       key   = activity description
 *       value = encoded parameters:
 *               "<dayIndex>:<startHour>:<duration>"
 *    Examples:
 *       "Gym"   -> "2:14:1.5"   (Wednesday, 2 PM, 1.5h)
 *       "Lunch" -> "4:12:1"     (Friday, noon, 1h)
 *
 * routineDescription:
 *    Free-form user text describing lifestyle and constraints.
 */
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

    public Map<String, String> getFixedActivities() {
        return fixedActivities;
    }

    public boolean hasFreeActivities() {
        return freeActivities != null && !freeActivities.isEmpty();
    }

    public boolean hasFixedActivities() {
        return fixedActivities != null && !fixedActivities.isEmpty();
    }
}
