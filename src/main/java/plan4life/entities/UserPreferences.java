package plan4life.entities;

public class UserPreferences {
    // 我们要保存的4个设置
    private final String theme;
    private final String language;
    private final int defaultReminderMinutes;
    private final String timeZoneId;

    public UserPreferences(String theme, String language, int defaultReminderMinutes, String timeZoneId) {
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