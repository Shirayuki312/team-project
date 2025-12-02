package plan4life.use_case.set_reminder;

public class SetReminderResponseModel {

    private final String message;

    public SetReminderResponseModel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

