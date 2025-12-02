package plan4life.entities;

import java.time.LocalDateTime;

/**
 * Domain entity that represents a persisted reminder for an event.
 *
 * Note: we keep this entity independent from the UI Event class
 * (plan4life.view.Event) to respect Clean Architecture layering.
 */
public class Reminder {

    /**
     * Simple string ID. In this project we use:
     *   id = eventTitle + "|" + eventStart.toString()
     */
    private final String id;

    private final String eventTitle;
    private final LocalDateTime eventStart;
    private final LocalDateTime eventEnd;

    private final int minutesBefore;
    private final String alertType;   // e.g. "Message with sound"
    private final String urgency;     // "LOW", "MEDIUM", "HIGH"

    private final boolean sendMessage;
    private final boolean sendEmail;
    private final boolean playSound;

    private final boolean active;

    public Reminder(String id,
                    String eventTitle,
                    LocalDateTime eventStart,
                    LocalDateTime eventEnd,
                    int minutesBefore,
                    String alertType,
                    String urgency,
                    boolean sendMessage,
                    boolean sendEmail,
                    boolean playSound,
                    boolean active) {
        this.id = id;
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

    public String getId() {
        return id;
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
}
