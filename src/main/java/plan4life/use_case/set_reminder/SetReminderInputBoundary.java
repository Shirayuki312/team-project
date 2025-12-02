package plan4life.use_case.set_reminder;

public interface SetReminderInputBoundary {

    void setReminder(SetReminderRequestModel requestModel);

    void cancelReminder(SetReminderRequestModel requestModel);
}


