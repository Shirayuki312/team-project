package plan4life.view;

import plan4life.entities.Schedule;

import java.util.List;

public interface CalendarViewInterface {
    void displaySchedule(Schedule schedule);
    void showMessage(String message);
    void updateActivityList(List<String> activities);
}
