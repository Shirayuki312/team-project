package plan4life.controller;
import plan4life.view.Event;

import javax.swing.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import plan4life.use_case.generate_schedule.*;
import plan4life.use_case.lock_activity.*;

import java.util.Set;

public class CalendarController {
    private final GenerateScheduleInputBoundary generateScheduleInteractor;
    private final LockActivityInputBoundary lockActivityInteractor;
    private final Map<Event, Timer> reminderTimers = new HashMap<>();

    public CalendarController(GenerateScheduleInputBoundary generateScheduleInteractor,
                              LockActivityInputBoundary lockActivityInteractor) {
        this.generateScheduleInteractor = generateScheduleInteractor;
        this.lockActivityInteractor = lockActivityInteractor;
    }

    public void generateSchedule(String routineDescription,
                                 Map<String, String> fixedActivities,
                                 List<String>freeActivities) {
        GenerateScheduleRequestModel request = new GenerateScheduleRequestModel(routineDescription,
                fixedActivities, freeActivities);
        generateScheduleInteractor.execute(request);
    }

    // Update: include schedule id so interactor can load/save proper schedule
    public void lockAndRegenerate(int scheduleId, Set<String> lockedSlots) {
        LockActivityRequestModel request = new LockActivityRequestModel(scheduleId, lockedSlots);
        lockActivityInteractor.execute(request);
    }
    public void setImportantReminder(Event event,
                                     int minutesBefore,
                                     String alertType) {
        if (event == null) return;

        event.setImportant(true);
        event.setReminderMinutesBefore(minutesBefore);
        event.setAlertType(alertType);

        cancelImportantReminder(event);

        LocalDateTime reminderTime = event.getStart().minusMinutes(minutesBefore);
        long delay = Duration.between(LocalDateTime.now(), reminderTime).toMillis();

        if (delay <= 0) {
            showReminderPopup(event, alertType);
            return;
        }

        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() ->
                        showReminderPopup(event, alertType));
            }
        }, delay);

        reminderTimers.put(event, timer);
    }

    public void cancelImportantReminder(Event event) {
        if (event == null) return;

        event.setImportant(false);
        event.setReminderMinutesBefore(null);

        Timer t = reminderTimers.remove(event);
        if (t != null) {
            t.cancel();
        }
    }

    private void showReminderPopup(Event event, String alertType) {
        JOptionPane.showMessageDialog(
                null,
                "Reminder: " + event.getTitle(),
                "Important Reminder",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}


