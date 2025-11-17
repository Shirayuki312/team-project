package plan4life.entities;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Tag {
    private final String name;
    private final Color color;
    private final List<Activity> activities;

    public Tag(String name, Color color) {
        this.name = name;
        this.color = color;
        this.activities = new ArrayList<>();
    }
}
