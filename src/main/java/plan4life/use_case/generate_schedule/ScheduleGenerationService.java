package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;
import java.util.List;
import java.util.Map;

public interface ScheduleGenerationService {

    Schedule generate(String routineDescription,
                      Map<String, String> fixedActivities);

    String findFreeSlot(Schedule schedule);

    void assignActivityToSlot(Schedule schedule,
                              String activityDescription,
                              float durationHours);
}


