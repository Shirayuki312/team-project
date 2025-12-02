package plan4life.data_access;

import plan4life.entities.Reminder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory DAO for storing reminders.
 * This keeps timers & reminder entities in memory only.
 */
public class InMemoryReminderDAO implements ReminderDataAccessInterface {

    private final Map<String, Reminder> storage = new HashMap<>();

    @Override
    public synchronized void saveReminder(Reminder reminder) {
        storage.put(reminder.getId(), reminder);
    }

    @Override
    public synchronized void deleteReminder(String id) {
        storage.remove(id);
    }

    @Override
    public synchronized Reminder getReminder(String id) {
        return storage.get(id);
    }

    @Override
    public synchronized List<Reminder> getAllReminders() {
        return new ArrayList<>(storage.values());
    }
}
