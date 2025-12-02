package plan4life.data_access;

import plan4life.entities.Reminder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory implementation of ReminderDataAccessInterface.
 * This is enough for your course project and unit tests.
 */
public class InMemoryReminderDAO implements ReminderDataAccessInterface {

    private final Map<String, Reminder> storage = new HashMap<>();

    @Override
    public void save(Reminder reminder) {
        if (reminder == null) return;
        storage.put(reminder.getId(), reminder);
    }

    @Override
    public Reminder findById(String id) {
        return storage.get(id);
    }

    @Override
    public void deleteById(String id) {
        storage.remove(id);
    }

    @Override
    public List<Reminder> findAll() {
        return new ArrayList<>(storage.values());
    }
}
