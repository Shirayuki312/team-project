package plan4life.view;

import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;

import java.util.List;

public interface CalendarViewInterface {
    void showMessage(String message);
    void displaySchedule(Schedule schedule);
    void applyBlockedTimeUpdate(Schedule schedule, List<BlockedTime> changedBlocks);

    void updateLanguage(String languageCode);
    void updateTheme(String themeName);
}