package plan4life.entities;

import java.time.LocalDateTime;

/**
 * Domain entity representing a reminder associated with an event.
 */
public class Reminder {

    private final String id;
    private final String title;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final LocalDateTime reminderTime;
    private final int minutesBefore;
    private final String alertType;
    private final String urgencyLevel;
    private final boolean sendMessage;
    private final boolean sendEmail;
    private final boolean playSound;
    private final boolean important;

    public Reminder(String id,
                    String title,
                    LocalDateTime start,
                    LocalDateTime end,
                    LocalDateTime reminderTime,
                    int minutesBefore,
                    String alertType,
                    String urgencyLevel,
                    boolean sendMessage,
                    boolean sendEmail,
                    boolean playSound,
                    boolean important) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.reminderTime = reminderTime;
        this.minutesBefore = minutesBefore;
        this.alertType = alertType;
        this.urgencyLevel = urgencyLevel;
        this.sendMessage = sendMessage;
        this.sendEmail = sendEmail;
        this.playSound = playSound;
        this.important = important;
    }

    public String getId() {
        return id;
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

    public LocalDateTime getReminderTime() {
        return reminderTime;
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
