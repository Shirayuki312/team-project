package plan4life.entities;

public class Activity {
    private final String description;
    private final float duration;
    private final String startTime;

    public Activity(String description, float duration) {
        this.description = description;
        this.duration = duration;
        this.startTime = null;
    }
    public Activity(String description, float duration, String startTime) {
        this.description = description;
        this.duration = duration;
        this.startTime = startTime;
    }

    public float getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }

    public String getStartTime() { return startTime; }

    public boolean isFixed() {
        return startTime != null;
    }
}
