package plan4life.use_case.set_reminder;

public interface SetReminderOutputBoundary {

    /**
     * Called right after a reminder has been scheduled (timer created & persisted).
     */
    void presentReminderScheduled(SetReminderResponseModel responseModel);

    /**
     * Called when the reminder time is reached (timer fires).
     */
    void presentReminderFired(SetReminderResponseModel responseModel);

    /**
     * Called when an existing reminder is cancelled.
     */
    void presentReminderCancelled(SetReminderResponseModel responseModel);
}

