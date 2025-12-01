package plan4life.ai;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Describes an event that must stay locked in place.
 */
public class FixedEventInput extends TimeBoundEvent {
    private final boolean locked;

    public FixedEventInput(DayOfWeek day, LocalTime startTime, int durationMinutes, String name) {
        this(day, startTime, durationMinutes, name, true);
    }

    public FixedEventInput(DayOfWeek day, LocalTime startTime, int durationMinutes, String name, boolean locked) {
        super(day, startTime, durationMinutes, name);
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }
}