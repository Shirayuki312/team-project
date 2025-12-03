package plan4life.ai;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Core data structure representing something that happens at a fixed time.
 */
public class TimeBoundEvent {
    private final DayOfWeek day;
    private final LocalTime startTime;
    private final int durationMinutes;
    private final String name;

    public TimeBoundEvent(DayOfWeek day, LocalTime startTime, int durationMinutes, String name) {
        this.day = Objects.requireNonNull(day, "day");
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.durationMinutes = durationMinutes;
        this.name = Objects.requireNonNull(name, "name");
    }

    public DayOfWeek getDay() {
        return day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getName() {
        return name;
    }
}