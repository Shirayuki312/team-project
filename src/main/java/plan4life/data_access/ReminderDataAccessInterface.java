package plan4life.data_access;

import plan4life.entities.Reminder;

import java.util.List;

public interface ReminderDataAccessInterface {

    void saveReminder(Reminder reminder);

    void deleteReminder(String id);

    Reminder getReminder(String id);

    List<Reminder> getAllReminders();
}
