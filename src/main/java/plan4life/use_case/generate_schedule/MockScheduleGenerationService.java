package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;

import java.util.*;

public class MockScheduleGenerationService implements ScheduleGenerationService {

    private static final int DAYS = 7;          // 0–6
    private static final int HOURS_PER_DAY = 24;

    @Override
    public Schedule generate(String routineDescription,
                             Map<String, String> fixedActivities) {

        Schedule s = new Schedule(1, "week");

        // Step 1: base random schedule
        s.populateRandomly();

        // Step 2: decode fixed activities
        if (fixedActivities != null) {
            for (Map.Entry<String, String> e : fixedActivities.entrySet()) {
                String activityDesc = e.getKey();
                String encoded = e.getValue(); // "day:start:duration"

                String[] parts = encoded.split(":");
                if (parts.length < 3) continue;

                try {
                    int day = Integer.parseInt(parts[0].trim());
                    int start = Integer.parseInt(parts[1].trim());
                    float duration = Float.parseFloat(parts[2].trim());

                    s.placeActivityDuration(day, start, duration, activityDesc);
                    s.lockSlotKey(day + ":" + start);

                } catch (Exception ignore) {
                    // malformed user input should never crash schedule generation
                }
            }
        }

        return s;
    }

    @Override
    public String findFreeSlot(Schedule schedule) {
        Map<String, String> existing = schedule.getActivities();

        for (int day = 0; day < DAYS; day++) {
            for (int hour = 0; hour < HOURS_PER_DAY; hour++) {

                String key = day + ":" + hour;

                if (!existing.containsKey(key) &&
                        !schedule.isLockedKey(key)) {

                    return key;
                }
            }
        }
        return null;
    }

    @Override
    public void assignActivityToSlot(Schedule schedule,
                                     String description,
                                     float durationHours) {

        int requiredHours = Math.max(1, (int)Math.ceil(durationHours));

        for (int day = 0; day < DAYS; day++) {
            for (int hour = 0; hour <= HOURS_PER_DAY - requiredHours; hour++) {

                boolean canFit = true;

                for (int i = 0; i < requiredHours; i++) {
                    String key = day + ":" + (hour + i);

                    if (schedule.getActivities().containsKey(key)
                            || schedule.isLockedKey(key)) {

                        canFit = false;
                        break;
                    }
                }

                if (canFit) {
                    schedule.placeActivityDuration(day, hour, durationHours, description);
                    return;
                }
            }
        }
    }
}
