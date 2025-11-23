package plan4life.data_access;
import plan4life.entities.UserPreferences;

public interface UserPreferencesDataAccessInterface {
    void save(UserPreferences preferences);
    UserPreferences load();
}