package plan4life.presenter;

import plan4life.use_case.block_off_time.BlockOffTimeOutputBoundary;
import plan4life.use_case.block_off_time.BlockOffTimeResponseModel;
import plan4life.use_case.generate_schedule.*;
import plan4life.use_case.lock_activity.*;
import plan4life.entities.Schedule;
import plan4life.view.CalendarViewInterface;
import plan4life.use_case.add_activity.AddActivityOutputBoundary;
import plan4life.use_case.add_activity.AddActivityResponseModel;

public class CalendarPresenter implements
        GenerateScheduleOutputBoundary,
        LockActivityOutputBoundary,
        BlockOffTimeOutputBoundary,
        AddActivityOutputBoundary {

    private final CalendarViewInterface view;

    public CalendarPresenter(CalendarViewInterface view) {
        this.view = view;
    }

    @Override
    public void present(GenerateScheduleResponseModel response) {
        Schedule schedule = response.getSchedule();
        view.displaySchedule(schedule);
    }

    @Override
    public void present(LockActivityResponseModel response) {
        Schedule updatedSchedule = response.getUpdatedSchedule();
        view.displaySchedule(updatedSchedule);
    }

    @Override
    public void present(BlockOffTimeResponseModel response) {
        if (response.getUpdatedSchedule() != null) {
            view.displaySchedule(response.getUpdatedSchedule());
        }
        view.showMessage(response.getMessage());
    }
    
    @Override
    public void present(AddActivityResponseModel responseModel) {
        if (responseModel.getPendingActivities() != null) {
            view.updateActivityList(responseModel.getPendingActivities());
        }
        if (responseModel.getMessage() != null && !responseModel.getMessage().isEmpty()) {
            view.showMessage(responseModel.getMessage());
        }
    }
}