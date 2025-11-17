package plan4life.entities;

public class Activity {
    private final String description;
    private final float duration;
    private final Tag tag;

    public Activity(String description, Tag tag) {
        this.description = description;
        this.duration = -1;
        this.tag = tag;
    }

    public Activity(String description, float duration, Tag tag) {
        this.description = description;
        this.duration = duration;
        this.tag = tag;
    }
}
