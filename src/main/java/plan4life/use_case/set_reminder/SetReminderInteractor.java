package plan4life.use_case.set_reminder;

import plan4life.data_access.ReminderDataAccessInterface;
import plan4life.entities.Reminder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Interactor for the SetReminder use case.
 * - Persists reminder data via DAO
 * - Schedules and cancels timers
 * - Notifies presenter when reminders are scheduled / fired / cancelled
 */
public class SetReminderInteractor implements SetReminderInputBoundary {

    private final ReminderDataAccessInterface reminderDAO;
    private final SetReminderOutputBoundary presenter;

    // Keeps track of active timers by reminder id
    private final Map<String, Timer> timers = new HashMap<>();

    public SetReminderInteractor(ReminderDataAccessInterface reminderDAO,
                                 SetReminderOutputBoundary presenter) {
        this.reminderDAO = reminderDAO;
        this.presenter = presenter;
    }

    private String buildReminderId(SetReminderRequestModel request) {
        // Deterministic id based on title + start + end.
        // This matches both setReminder(...) and cancelReminder(...)
        return request.getTitle() + "|" + request.getStart() + "|" + request.getEnd();
    }

    private void cancelTimer(String id) {
        Timer t = timers.remove(id);
        if (t != null) {
            t.cancel();
        }
    }

    @Override
    public void setReminder(SetReminderRequestModel requestModel) {
        String id = buildReminderId(requestModel);

        // Cancel any existing timer for this reminder
        cancelTimer(id);

        LocalDateTime reminderTime =
                requestModel.getStart().minusMinutes(requestModel.getMinutesBefore());
        long delayMillis = Duration.between(LocalDateTime.now(), reminderTime).toMillis();

        Reminder reminder = new Reminder(
                id,
                requestModel.getTitle(),
                requestModel.getStart(),
                requestModel.getEnd(),
                reminderTime,
                requestModel.getMinutesBefore(),
                requestModel.getAlertType(),
                requestModel.getUrgencyLevel(),
                requestModel.isSendMessage(),
                requestModel.isSendEmail(),
                requestModel.isPlaySound(),
                requestModel.isImportant()
        );

        // Persist
        reminderDAO.saveReminder(reminder);

        SetReminderResponseModel scheduledResponse =
                SetReminderResponseModel.fromEntity(reminder);
        presenter.presentReminderScheduled(scheduledResponse);

        // If the time is already in the past, fire immediately
        if (delayMillis <= 0) {
            presenter.presentReminderFired(scheduledResponse);
            return;
        }

        Timer timer = new Timer(true);
        timers.put(id, timer);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SetReminderResponseModel fireResponse =
                        SetReminderResponseModel.fromEntity(reminder);
                presenter.presentReminderFired(fireResponse);
            }
        }, delayMillis);
    }

    @Override
    public void cancelReminder(SetReminderRequestModel requestModel) {
        String id = buildReminderId(requestModel);

        cancelTimer(id);
        reminderDAO.deleteReminder(id);

        // Build a lightweight Reminder object just for presenter
        Reminder dummy = new Reminder(
                id,
                requestModel.getTitle(),
                requestModel.getStart(),
                requestModel.getEnd(),
                null,
                requestModel.getMinutesBefore(),
                requestModel.getAlertType(),
                requestModel.getUrgencyLevel(),
                requestModel.isSendMessage(),
                requestModel.isSendEmail(),
                requestModel.isPlaySound(),
                false
        );

        SetReminderResponseModel resp =
                SetReminderResponseModel.fromEntity(dummy);
        presenter.presentReminderCancelled(resp);
    }
}
