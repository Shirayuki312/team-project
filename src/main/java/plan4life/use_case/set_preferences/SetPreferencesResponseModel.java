package plan4life.use_case.set_preferences;

public class SetPreferencesResponseModel {

    private final String theme;
    private final String language;

    public SetPreferencesResponseModel(String theme, String language) {
        this.theme = theme;
        this.language = language;
    }

    public String getTheme() { return theme; }
    public String getLanguage() { return language; }
}