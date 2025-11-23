package plan4life.view;

import java.awt.Color;
import java.time.LocalDateTime;

/**
 * Simple event model used by the calendar and reminder system.
 */
public class Event {

    /** Urgency level of an event; can be shown with different colors in the UI. */
    public enum UrgencyLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    private String title;
    private LocalDateTime start;
    private LocalDateTime end;

    private boolean important;
    /**
     * Minutes before {@code start} when the reminder should fire.
     * If null, no reminder is scheduled.
     */
    private Integer reminderMinutesBefore;
    /** e.g. "Message" or "Message with sound". */
    private String alertType;

    private UrgencyLevel urgencyLevel = UrgencyLevel.MEDIUM;
    private boolean sendMessage;
    private boolean sendEmail;
    private boolean playSound;

    private Color color = new Color(65, 243, 6); // default: light green

    public Event(String title, LocalDateTime start, LocalDateTime end) {
        this.title = title;
        this.start = start;
        this.end = end;
        updateColorFromUrgency();
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }


    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public Integer getReminderMinutesBefore() {
        return reminderMinutesBefore;
    }

    public void setReminderMinutesBefore(Integer reminderMinutesBefore) {
        this.reminderMinutesBefore = reminderMinutesBefore;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }


    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        if (urgencyLevel == null) {
            urgencyLevel = UrgencyLevel.MEDIUM;
        }
        this.urgencyLevel = urgencyLevel;
        updateColorFromUrgency();
    }

    public Color getColor() {
        return color;
    }

    private void updateColorFromUrgency() {
        switch (urgencyLevel) {
            case LOW:
                color = new Color(8, 239, 204);  // light blue
                break;
            case MEDIUM:
                color = new Color(65, 243, 6);  // light green
                break;
            case HIGH:
                color = new Color(246, 194, 5);  // light yellow
                break;
        }
    }


    public boolean isSendMessage() {
        return sendMessage;
    }

    public void setSendMessage(boolean sendMessage) {
        this.sendMessage = sendMessage;
    }

    public boolean isSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }
}

