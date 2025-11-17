package plan4life.entities;

import java.util.ArrayList;
import java.util.List;

public class User {
    private final String username;
    private final UserPreferences userPreferences;
    private final List<Schedule> schedules;

    public User(String username, UserPreferences userPreferences) {
        this.username = username;
        this.userPreferences = userPreferences;

        this.schedules = new ArrayList<>();
        this.schedules.add(new Schedule(0, "day"));
        this.schedules.add(new Schedule(1, "week"));
    }
}
