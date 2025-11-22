package plan4life.use_case.set_preferences;

public class SetPreferencesRequestModel {
    private String theme;
    private String language;
    private int defaultReminderMinutes;
    private String timeZoneId;

    public SetPreferencesRequestModel(String theme, String language, int defaultReminderMinutes, String timeZoneId) {
        this.theme = theme;
        this.language = language;
        this.defaultReminderMinutes = defaultReminderMinutes;
        this.timeZoneId = timeZoneId;
    }

    public String getTheme() { return theme; }
    public String getLanguage() { return language; }
    public int getDefaultReminderMinutes() { return defaultReminderMinutes; }
    public String getTimeZoneId() { return timeZoneId; }
}