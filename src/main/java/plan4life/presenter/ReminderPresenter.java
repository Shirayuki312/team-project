package plan4life.presenter;

import plan4life.use_case.set_reminder.SetReminderOutputBoundary;
import plan4life.use_case.set_reminder.SetReminderResponseModel;
import plan4life.view.CalendarViewInterface;

/**
 * Simple presenter for reminder use case.
 * It just forwards a small message to the Calendar view.
 */
public class ReminderPresenter implements SetReminderOutputBoundary {

    private final CalendarViewInterface view;

    public ReminderPresenter(CalendarViewInterface view) {
        this.view = view;
    }

    @Override
    public void present(SetReminderResponseModel responseModel) {
        if (view != null && responseModel != null) {
            view.showMessage(responseModel.getMessage());
        }
    }
}
