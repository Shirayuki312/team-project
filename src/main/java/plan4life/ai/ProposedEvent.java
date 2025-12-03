package plan4life.ai;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Represents an event returned by the LLM.
 */
public class ProposedEvent extends TimeBoundEvent {
    private final boolean locked;

    public ProposedEvent(DayOfWeek day, LocalTime startTime, int durationMinutes, String name, boolean locked) {
        super(day, startTime, durationMinutes, name);
        this.locked = locked;
    }

    public boolean isLocked() {
        return locked;
    }
}