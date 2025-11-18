package plan4life.use_case.add_activity;

public class AddActivityRequestModel {
    private final String name;
    private final float durationHours;
    private final String tagName;

    public AddActivityRequestModel(String name, float durationHours, String tagName) {
        this.name = name;
        this.durationHours = durationHours;
        this.tagName = tagName;
    }

    public String getName() {
        return name;
    }

    public float getDurationHours() {
        return durationHours;
    }

    public String getTagName() {
        return tagName;
    }
}
