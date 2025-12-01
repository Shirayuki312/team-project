package plan4life.entities;

public class Activity {
    private final String description;
    private final float duration;
    private final String startTime;  // "14:00" or null
    private final Integer dayIndex;  // 0 = Monday … 6 = Sunday, or null

    // Free activity
    public Activity(String description, float duration) {
        this(description, duration, null, null);
    }

    // Fixed by start time only
    public Activity(String description, float duration, String startTime) {
        this(description, duration, startTime, null);
    }

    // Fully fixed: day + start time
    public Activity(String description, float duration, int dayIndex, String startTime) {
        this(description, duration, startTime, dayIndex);
    }

    // Core constructor
    private Activity(String description, float duration, String startTime, Integer dayIndex) {
        this.description = description;
        this.duration = duration;
        this.startTime = startTime;
        this.dayIndex = dayIndex;
    }

    // Extra constructor for Activity + duration + day only (no time)
    public static Activity withDayOnly(String description, float duration, int dayIndex) {
        return new Activity(description, duration, null, dayIndex);
    }

    public boolean isFixed() {
        return startTime != null || dayIndex != null;
    }

    public boolean hasStartTime() { return startTime != null; }
    public boolean hasDay() { return dayIndex != null; }

    public float getDuration() { return duration; }
    public String getDescription() { return description; }
    public String getStartTime() { return startTime; }
    public Integer getDayIndex() { return dayIndex; }
}

