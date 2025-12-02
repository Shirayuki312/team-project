package plan4life.use_case.set_reminder;

import plan4life.data_access.ReminderDataAccessInterface;
import plan4life.entities.Reminder;

/**
 * Interactor that persists reminders (set / cancel).
 */
public class SetReminderInteractor implements SetReminderInputBoundary {

    private final ReminderDataAccessInterface reminderDAO;
    private final SetReminderOutputBoundary presenter;

    public SetReminderInteractor(ReminderDataAccessInterface reminderDAO,
                                 SetReminderOutputBoundary presenter) {
        this.reminderDAO = reminderDAO;
        this.presenter = presenter;
    }

    @Override
    public void setReminder(SetReminderRequestModel requestModel) {
        String id = requestModel.buildReminderId();

        Reminder reminder = new Reminder(
                id,
                requestModel.getEventTitle(),
                requestModel.getEventStart(),
                requestModel.getEventEnd(),
                requestModel.getMinutesBefore(),
                requestModel.getAlertType(),
                requestModel.getUrgency(),
                requestModel.isSendMessage(),
                requestModel.isSendEmail(),
                requestModel.isPlaySound(),
                true
        );

        reminderDAO.save(reminder);

        SetReminderResponseModel response =
                new SetReminderResponseModel(
                        "Reminder saved for " + requestModel.getEventTitle()
                );
        presenter.present(response);
    }

    @Override
    public void cancelReminder(SetReminderRequestModel requestModel) {
        String id = requestModel.buildReminderId();
        reminderDAO.deleteById(id);

        SetReminderResponseModel response =
                new SetReminderResponseModel(
                        "Reminder cancelled for " + requestModel.getEventTitle()
                );
        presenter.present(response);
    }
}

