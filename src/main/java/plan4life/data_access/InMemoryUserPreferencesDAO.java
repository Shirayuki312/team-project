package plan4life.data_access;
import plan4life.entities.UserPreferences;

public class InMemoryUserPreferencesDAO implements UserPreferencesDataAccessInterface {
    private UserPreferences preferences;

    @Override
    public void save(UserPreferences preferences) {
        this.preferences = preferences;
        System.out.println("Data Saved: " + preferences.getTheme() + ", " + preferences.getTimeZoneId());
    }

    @Override
    public UserPreferences load() {
        // 如果还没保存过，返回默认值
        if (this.preferences == null) {
            return new UserPreferences("Light Mode", "English", 15, "America/Toronto");
        }
        return this.preferences;
    }
}
