package plan4life.ai;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Describes a flexible routine item that the LLM can schedule anywhere within the plan.
 */
public class RoutineEventInput extends TimeBoundEvent {
    public RoutineEventInput(DayOfWeek day, LocalTime startTime, int durationMinutes, String name) {
        super(day, startTime, durationMinutes, name);
    }
}