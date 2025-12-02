package plan4life.use_case.set_reminder;

import java.time.LocalDateTime;

/**
 * Data passed from controller to interactor for creating / canceling a reminder.
 */
public class SetReminderRequestModel {

    private final String eventTitle;
    private final LocalDateTime eventStart;
    private final LocalDateTime eventEnd;
    private final int minutesBefore;
    private final String alertType;
    private final String urgency; // "LOW", "MEDIUM", "HIGH"
    private final boolean sendMessage;
    private final boolean sendEmail;
    private final boolean playSound;
    private final boolean active;

    public SetReminderRequestModel(String eventTitle,
                                   LocalDateTime eventStart,
                                   LocalDateTime eventEnd,
                                   int minutesBefore,
                                   String alertType,
                                   String urgency,
                                   boolean sendMessage,
                                   boolean sendEmail,
                                   boolean playSound,
                                   boolean active) {
        this.eventTitle = eventTitle;
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.minutesBefore = minutesBefore;
        this.alertType = alertType;
        this.urgency = urgency;
        this.sendMessage = sendMessage;
        this.sendEmail = sendEmail;
        this.playSound = playSound;
        this.active = active;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public LocalDateTime getEventStart() {
        return eventStart;
    }

    public LocalDateTime getEventEnd() {
        return eventEnd;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getUrgency() {
        return urgency;
    }

    public boolean isSendMessage() {
        return sendMessage;
    }

    public boolean isSendEmail() {
        return sendEmail;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Utility: build the reminder ID (same rule as entities.Reminder).
     */
    public String buildReminderId() {
        return eventTitle + "|" + eventStart;
    }
}

