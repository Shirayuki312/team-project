package plan4life.controller;

import plan4life.view.Event;
import plan4life.view.Event.UrgencyLevel;

import plan4life.use_case.generate_schedule.GenerateScheduleInputBoundary;
import plan4life.use_case.generate_schedule.GenerateScheduleRequestModel;
import plan4life.use_case.lock_activity.LockActivityInputBoundary;
import plan4life.use_case.lock_activity.LockActivityRequestModel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * CalendarController coordinates:
 * 1. Use-case interactions for generating and regenerating schedules.
 * 2. The "Set Important Reminder" logic (Use Case 7), including:
 *    - marking events as important
 *    - storing reminder metadata (time, alert type, urgency, channels)
 *    - scheduling and canceling reminder timers
 */
public class CalendarController {

    private final GenerateScheduleInputBoundary generateScheduleInteractor;
    private final LockActivityInputBoundary lockActivityInteractor;


    /** Each Event may have an associated Timer that fires at the reminder time. */
    private final Map<Event, Timer> reminderTimers = new HashMap<>();

    /** All events known to the controller (for "apply to all events"). */
    private final List<Event> events = new ArrayList<>();

    public CalendarController(GenerateScheduleInputBoundary generateScheduleInteractor,
                              LockActivityInputBoundary lockActivityInteractor) {
        this.generateScheduleInteractor = generateScheduleInteractor;
        this.lockActivityInteractor = lockActivityInteractor;
    }


    public void generateSchedule(String routineDescription, String fixedActivities) {
        GenerateScheduleRequestModel request =
                new GenerateScheduleRequestModel(routineDescription, fixedActivities);
    public void generateSchedule(String routineDescription, Map<String, String> fixedActivities) {
        GenerateScheduleRequestModel request = new GenerateScheduleRequestModel(routineDescription, fixedActivities);
        generateScheduleInteractor.execute(request);
    }

    public void lockAndRegenerate(int scheduleId, Set<String> lockedSlots) {
        LockActivityRequestModel request =
                new LockActivityRequestModel(scheduleId, lockedSlots);
        lockActivityInteractor.execute(request);
    }


    /**
     * Registers an event within the system.
     * Required to support "Apply reminder to all events".
     */
    public void registerEvent(Event event) {
        if (event != null && !events.contains(event)) {
            events.add(event);
        }
    }

    public boolean hasAnyEvent() {
        return !events.isEmpty();
    }

    public List<Event> getEvents() {
        return Collections.unmodifiableList(events);
    }


    /**
     * Backwards-compatible version that only sets minutesBefore and alertType.
     * Uses MEDIUM urgency, no message/email, and sound depending on alertType.
     */
    public void setImportantReminder(Event event,
                                     int minutesBefore,
                                     String alertType) {
        boolean playSound = "Message with sound".equalsIgnoreCase(alertType);
        setImportantReminderForEvent(
                event,
                minutesBefore,
                alertType,
                UrgencyLevel.MEDIUM,
                false,   // sendMessage
                false,   // sendEmail
                playSound
        );
    }

    public void setImportantReminderForAllEvents(Event event, int minutesBefore,
                                                 String alertType) {
        boolean playSound = "Message with sound".equalsIgnoreCase(alertType);
        setImportantReminderForAllEvents(event,
                minutesBefore,
                alertType,
                UrgencyLevel.MEDIUM,
                false,
                false,
                playSound
        );
    }


    public void setImportantReminderForEvent(Event event,
                                             int minutesBefore,
                                             String alertType,
                                             UrgencyLevel urgencyLevel,
                                             boolean sendMessage,
                                             boolean sendEmail,
                                             boolean playSound) {
        if (event == null) return;

        event.setImportant(true);
        event.setReminderMinutesBefore(minutesBefore);
        event.setAlertType(alertType);
        event.setUrgencyLevel(urgencyLevel);
        event.setSendMessage(sendMessage);
        event.setSendEmail(sendEmail);
        event.setPlaySound(playSound);

        // Track this event globally
        registerEvent(event);

        cancelReminderTimer(event);

        LocalDateTime reminderTime = event.getStart().minusMinutes(minutesBefore);
        long delay = Duration.between(LocalDateTime.now(), reminderTime).toMillis();

        if (delay <= 0) {
            showReminderPopup(event);
            return;
        }

        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> showReminderPopup(event));
            }
        }, delay);

        reminderTimers.put(event, timer);
    }


    /**
     * Applies the same reminder configuration to all registered events.
     * If there are no events yet, shows an informational message.
     */
    public void setImportantReminderForAllEvents(Event event, int minutesBefore,
                                                 String alertType,
                                                 UrgencyLevel urgencyLevel,
                                                 boolean sendMessage,
                                                 boolean sendEmail,
                                                 boolean playSound) {
        if (events.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "There is no event yet. Please create an event first.",
                    "No events",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        for (Event e : events) {
            setImportantReminderForEvent(
                    e,
                    minutesBefore,
                    alertType,
                    urgencyLevel,
                    sendMessage,
                    sendEmail,
                    playSound
            );
        }
    }

    /** Cancel only the timer associated with an event, without modifying its fields. */
    private void cancelReminderTimer(Event event) {
        Timer t = reminderTimers.remove(event);
        if (t != null) {
            t.cancel();
        }
    }

    /**
     * User deselects "Mark as Important": clear reminder metadata
     * and cancel any scheduled reminder.
     */
    public void cancelImportantReminder(Event event) {
        if (event == null) return;

        event.setImportant(false);
        event.setReminderMinutesBefore(null);
        event.setAlertType(null);
        event.setPlaySound(false);
        event.setSendEmail(false);
        event.setSendMessage(false);

        cancelReminderTimer(event);
    }



    /**
     * Displays the reminder pop-up and, depending on the event settings,
     * may also play a sound and show simulated "message" and "email" notifications.
     */
    private void showReminderPopup(Event event) {
        boolean shouldBeep =
                event.isPlaySound()
                        || "Message with sound".equalsIgnoreCase(event.getAlertType());

        if (shouldBeep) {
            Toolkit.getDefaultToolkit().beep();
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Reminder: ").append(event.getTitle());
        msg.append(" (").append(event.getUrgencyLevel().name()).append(")");

        JOptionPane.showMessageDialog(
                null,
                msg.toString(),
                "Important Reminder",
                JOptionPane.INFORMATION_MESSAGE
        );

        if (event.isSendMessage()) {
            JOptionPane.showMessageDialog(
                    null,
                    "A message notification for this event has been sent "
                            + "to your messaging inbox.",
                    "Message Sent",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        if (event.isSendEmail()) {
            JOptionPane.showMessageDialog(
                    null,
                    "An email reminder for this event has been generated.",
                    "Email Sent",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

    }
}




