package plan4life.controller;

import plan4life.entities.Event;
import plan4life.entities.Event.UrgencyLevel;

import plan4life.use_case.generate_schedule.GenerateScheduleInputBoundary;
import plan4life.use_case.generate_schedule.GenerateScheduleRequestModel;
import plan4life.use_case.lock_activity.LockActivityInputBoundary;
import plan4life.use_case.lock_activity.LockActivityRequestModel;
import plan4life.use_case.set_reminder.SetReminderInputBoundary;
import plan4life.use_case.set_reminder.SetReminderRequestModel;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CalendarController coordinates:
 * 1. Use-case interactions for generating and regenerating schedules.
 * 2. The "Set Important Reminder" logic (Use Case 7), including:
 *    - marking events as important
 *    - storing reminder metadata (time, alert type, urgency, channels)
 *    - scheduling and canceling reminder timers
 */
public class CalendarController {

    // ==== Use case interactors ====
    private final GenerateScheduleInputBoundary generateScheduleInteractor;
    private final LockActivityInputBoundary lockActivityInteractor;


    /**
     * All events known to the controller (for "apply to all events").
     */
    private final List<Event> events = new ArrayList<>();
    // NEW: track which events already have a reminder configured
    private final Set<Event> eventsWithReminder = new HashSet<>();
    private final SetReminderInputBoundary setReminderInteractor;


    // =========================================================
    //                       Constructor
    // =========================================================
    public CalendarController(GenerateScheduleInputBoundary generateScheduleInteractor,
                              LockActivityInputBoundary lockActivityInteractor,
                              SetReminderInputBoundary setReminderInteractor) {
        this.generateScheduleInteractor = generateScheduleInteractor;
        this.lockActivityInteractor = lockActivityInteractor;
        this.setReminderInteractor = setReminderInteractor;
    }

    public void generateSchedule(String routineDescription,
                                 String fixedActivities,
                                 List<String> freeActivities) {
        GenerateScheduleRequestModel request = new GenerateScheduleRequestModel(routineDescription,
                fixedActivities, freeActivities);
        generateScheduleInteractor.execute(request);
    }

    // =========================================================
    //                 Lock & regenerate schedule
    // =========================================================

    /**
     * Used by CalendarFrame when user locks certain time slots.
     */
    public void lockAndRegenerate(int scheduleId, Set<String> lockedSlots) {
        Set<String> copy = (lockedSlots == null)
                ? new HashSet<>()
                : new HashSet<>(lockedSlots);

        LockActivityRequestModel request =
                new LockActivityRequestModel(scheduleId, copy);
        lockActivityInteractor.execute(request);
    }

    // =========================================================
    //                Event registration & querying
    // =========================================================

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


    // =========================================================
    //      Extended reminder API used by ReminderDialog
    // =========================================================

    /**
     * Sets a reminder with full options for a single event.
     * Now the actual reminder business logic (persist / notify) is delegated
     * to the SetReminder use case instead of being handled directly here.
     */
    public void setImportantReminderForEvent(Event event,
                                             int minutesBefore,
                                             String alertType,
                                             UrgencyLevel urgencyLevel,
                                             boolean sendMessage,
                                             boolean sendEmail,
                                             boolean playSound) {
        if (event == null) return;

        // Update view-layer event metadata so UI can still read these flags.
        event.setImportant(true);
        event.setReminderMinutesBefore(minutesBefore);
        event.setAlertType(alertType);
        event.setUrgencyLevel(urgencyLevel);
        event.setSendMessage(sendMessage);
        event.setSendEmail(sendEmail);
        event.setPlaySound(playSound);

        // Track this event globally (for "apply to all events").
        registerEvent(event);

        // Delegate to SetReminder use case (interactor + DAO + presenter).
        sendReminderToUseCase(
                event,
                minutesBefore,
                alertType,
                urgencyLevel,
                sendMessage,
                sendEmail,
                playSound,
                true    // isImportant = true
        );
        // NEW: remember that this event now has a reminder
        eventsWithReminder.add(event);
    }


    /**
     * Helper method to send reminder info to the SetReminder use case.
     * If isImportant = true  -> setReminder(...)
     * If isImportant = false -> cancelReminder(...)
     */
    private void sendReminderToUseCase(Event event,
                                       int minutesBefore,
                                       String alertType,
                                       UrgencyLevel urgencyLevel,
                                       boolean sendMessage,
                                       boolean sendEmail,
                                       boolean playSound,
                                       boolean isImportant) {
        if (setReminderInteractor == null || event == null) {
            return;
        }

        SetReminderRequestModel request = new SetReminderRequestModel(
                event.getTitle(),
                event.getStart(),
                event.getEnd(),
                minutesBefore,
                alertType,
                (urgencyLevel != null ? urgencyLevel.name() : null),
                sendMessage,
                sendEmail,
                playSound,
                isImportant
        );

        if (isImportant) {
            setReminderInteractor.setReminder(request);
        } else {
            setReminderInteractor.cancelReminder(request);
        }
    }


    public void setImportantReminderForAllEvents(Event sourceEvent,
                                                 int minutesBefore,
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

            // NEW: only touch
            //  - the current sourceEvent, OR
            //  - events that already had a reminder before
            if (e != sourceEvent && !eventsWithReminder.contains(e)) {
                // This event never had a reminder, and it's not the one user is editing now.
                // Do NOT auto-add reminder for it.
                continue;
            }

            boolean sameMinutes =
                    e.getReminderMinutesBefore() != null
                            && e.getReminderMinutesBefore() == minutesBefore;

            boolean sameAlert =
                    (e.getAlertType() == null && alertType == null)
                            || (e.getAlertType() != null && e.getAlertType().equals(alertType));

            boolean sameUrgency =
                    (e.getUrgencyLevel() == null && urgencyLevel == null)
                            || (e.getUrgencyLevel() != null && e.getUrgencyLevel().equals(urgencyLevel));

            boolean sameChannels =
                    e.isSendMessage() == sendMessage
                            && e.isSendEmail() == sendEmail
                            && e.isPlaySound() == playSound;

            if (e.isImportant() && sameMinutes && sameAlert && sameUrgency && sameChannels) {
                // This event already has an identical reminder; do NOT re-schedule it.
                continue;
            }

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


    // =========================================================
    //                  Cancel timers / cancel reminders
    // =========================================================

    /**
     * User deselects "Mark as Important": clear reminder metadata
     * and let the SetReminder use case cancel the reminder.
     */
    public void cancelImportantReminder(Event event) {
        if (event == null) return;

        if (!event.isImportant()) {
            JOptionPane.showMessageDialog(
                    null,
                    "This event has no reminder. It cannot remove reminder.",
                    "No reminder to remove",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Clear view-layer metadata
        event.setImportant(false);
        event.setReminderMinutesBefore(null);
        event.setAlertType(null);
        event.setPlaySound(false);
        event.setSendEmail(false);
        event.setSendMessage(false);

        // Tell the use case to cancel persistence / timers, etc.
        sendReminderToUseCase(
                event,
                0,
                null,
                null,
                false,
                false,
                false,
                false   // isImportant = false -> cancel
        );
        // NEW: this event no longer has an active reminder
        eventsWithReminder.remove(event);
    }
}