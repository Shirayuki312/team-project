package plan4life.use_case.set_reminder;

/**
 * Input boundary for the "set / cancel reminder" use case.
 */
public interface SetReminderInputBoundary {

    void setReminder(SetReminderRequestModel requestModel);

    void cancelReminder(SetReminderRequestModel requestModel);
}

