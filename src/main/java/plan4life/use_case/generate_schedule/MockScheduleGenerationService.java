package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;

import java.util.*;

/**
 * Simple mock generator used for local testing until the real LLM/RAG service is integrated.
 *
 * Capabilities:
 *  - preserves fixed activities
 *  - randomly populates remaining slots
 *  - finds free slots in the schedule
 *  - assigns activities with or without duration
 */
public class MockScheduleGenerationService implements ScheduleGenerationService {

    private static final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri"};
    private static final int START_HOUR = 9;
    private static final int END_HOUR = 17;

    /**
     * Baseline schedule generator for routine description + fixed activities.
     * Free activities will be added separately by assignActivityToSlot().
     */
    @Override
    public Schedule generate(String routineDescription,
                             Map<String, String> fixedActivities) {

        Schedule s = new Schedule(1, "week");

        // Step 1: Random base schedule, respecting locked slots (none at this stage)
        s.populateRandomly();

        // Step 2: Overwrite/add fixed activities
        if (fixedActivities != null) {
            for (Map.Entry<String, String> e : fixedActivities.entrySet()) {
                s.addActivity(e.getKey(), e.getValue());
                s.lockSlotKey(e.getKey());  // fixed items should be locked
            }
        }

        return s;
    }

    /**
     * Finds a free time slot of format: "Mon 9:00".
     */
    @Override
    public String findFreeSlot(Schedule schedule) {
        Map<String, String> existing = schedule.getActivities();

        for (String day : DAYS) {
            for (int hour = START_HOUR; hour <= END_HOUR; hour++) {
                String key = day + " " + hour + ":00";

                if (!existing.containsKey(key) &&
                        !schedule.getLockedSlotKeys().contains(key)) {
                    return key; // Found a free slot
                }
            }
        }

        return null; // no free slot available
    }

    /**
     * Assign an activity that has:
     *   - description
     *   - duration (hours)

     * Duration placement logic:
     *  - If duration < 1 hr → treat as 1 hr
     *  - Attempts to place activity in consecutive hour blocks
     *  - If cannot place continuous duration → returns without inserting
     */
    @Override
    public void assignActivityToSlot(Schedule schedule,
                                     String description,
                                     float durationHours) {

        if (description == null || description.isEmpty())
            return;

        int requiredHours = Math.max(1, Math.round(durationHours));

        // Try every day/hour combination
        for (String day : DAYS) {
            for (int hour = START_HOUR; hour <= END_HOUR - requiredHours + 1; hour++) {

                boolean canFit = true;

                // Check each required hour block
                for (int i = 0; i < requiredHours; i++) {
                    String key = day + " " + (hour + i) + ":00";

                    if (schedule.getActivities().containsKey(key) ||
                            schedule.getLockedSlotKeys().contains(key)) {
                        canFit = false;
                        break;
                    }
                }

                if (canFit) {
                    // Place the activity across all required blocks
                    for (int i = 0; i < requiredHours; i++) {
                        String key = day + " " + (hour + i) + ":00";
                        schedule.addActivity(key, description + " (" + durationHours + "h)");
                    }
                    return;
                }
            }
        }
    }
}
