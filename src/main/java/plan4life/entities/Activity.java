package plan4life.entities;

public class Activity {
    private final String description;
    private final float duration;

    public Activity(String description) {
        this.description = description;
        this.duration = -1;
    }

    public Activity(String description, float duration) {
        this.description = description;
        this.duration = duration;
    }

    public float getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }
}
