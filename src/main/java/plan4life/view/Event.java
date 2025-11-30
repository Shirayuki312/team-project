package plan4life.view;

import java.time.LocalDateTime;

public class Event {
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean important;
    private Integer reminderMinutesBefore;
    private String alertType;

    public Event(String title, LocalDateTime start, LocalDateTime end) {
        this.title = title;
        this.start = start;
        this.end = end;
    }

    public String getTitle() { return title; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    public boolean isImportant() { return important; }
    public void setImportant(boolean important) { this.important = important; }

    public Integer getReminderMinutesBefore() { return reminderMinutesBefore; }
    public void setReminderMinutesBefore(Integer reminderMinutesBefore) {
        this.reminderMinutesBefore = reminderMinutesBefore;
    }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
}
