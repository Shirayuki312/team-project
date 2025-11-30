package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;
import java.util.Map;

public interface ScheduleGenerationService {

    // Generate a Schedule given a routine description and any fixed activities.
    // @param routineDescription user natural language description (may leave it empty)
    // @param fixedActivities structured fixed activities map (timeslot -> activity)
    // @return Schedule populated according to inputs

    Schedule generate(String routineDescription, Map<String, String> fixedActivities);

    // Pick an available time slot in the schedule
    String findFreeSlot(Schedule schedule);

    // Smartly place a single activity into the schedule
    void assignActivityToSlot(Schedule schedule, String activityName);

}

