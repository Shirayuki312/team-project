package plan4life.use_case.set_reminder;

import java.time.LocalDateTime;

/**
 * Input data for the SetReminder use case.
 */
public class SetReminderRequestModel {

    private final String title;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final int minutesBefore;
    private final String alertType;
    private final String urgencyLevel;
    private final boolean sendMessage;
    private final boolean sendEmail;
    private final boolean playSound;
    private final boolean important;

    public SetReminderRequestModel(String title,
                                   LocalDateTime start,
                                   LocalDateTime end,
                                   int minutesBefore,
                                   String alertType,
                                   String urgencyLevel,
                                   boolean sendMessage,
                                   boolean sendEmail,
                                   boolean playSound,
                                   boolean important) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.minutesBefore = minutesBefore;
        this.alertType = alertType;
        this.urgencyLevel = urgencyLevel;
        this.sendMessage = sendMessage;
        this.sendEmail = sendEmail;
        this.playSound = playSound;
        this.important = important;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
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

    public boolean isImportant() {
        return important;
    }
}
