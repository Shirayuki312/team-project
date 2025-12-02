package plan4life.data_access;

import plan4life.entities.Reminder;

import java.util.List;

/**
 * Gateway interface for storing and loading Reminder entities.
 */
public interface ReminderDataAccessInterface {

    void save(Reminder reminder);

    Reminder findById(String id);

    void deleteById(String id);

    List<Reminder> findAll();
}
