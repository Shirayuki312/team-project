package plan4life.view;

import plan4life.entities.Schedule;

public interface CalendarViewInterface {
    void showMessage(String message);
    void displaySchedule(Schedule schedule);

    void updateLanguage(String languageCode);
    void updateTheme(String themeName);
}