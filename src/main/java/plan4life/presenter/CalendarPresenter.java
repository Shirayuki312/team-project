package plan4life.presenter;

import plan4life.use_case.block_off_time.BlockOffTimeOutputBoundary;
import plan4life.use_case.block_off_time.BlockOffTimeResponseModel;
import plan4life.use_case.generate_schedule.*;
import plan4life.use_case.lock_activity.*;
import plan4life.entities.Schedule;
import plan4life.view.CalendarViewInterface;

public class CalendarPresenter implements
        GenerateScheduleOutputBoundary, LockActivityOutputBoundary, BlockOffTimeOutputBoundary {

    private final CalendarViewInterface view;

    public CalendarPresenter(CalendarViewInterface view) {
        this.view = view;
    }

    @Override
    public void present(GenerateScheduleResponseModel response) {
        Schedule schedule = response.getSchedule();
        view.displaySchedule(schedule);

        if (response.getMessage() != null && !response.getMessage().isBlank()) {
            view.showMessage(response.getMessage());
            return;
        }

        if (schedule == null) {
            view.showMessage("No schedule available to display.");
        } else if (schedule.getActivities().isEmpty()
                && schedule.getLockedBlocks().isEmpty()
                && schedule.getUnlockedBlocks().isEmpty()) {
            view.showMessage("No plan could be created. Please adjust your inputs and try again.");
        }
    }

    @Override
    public void present(LockActivityResponseModel response) {
        Schedule updatedSchedule = response.getUpdatedSchedule();
        view.displaySchedule(updatedSchedule);
    }

    @Override
    public void present(BlockOffTimeResponseModel response) {
        if (response.getUpdatedSchedule() != null) {
            view.applyBlockedTimeUpdate(response.getUpdatedSchedule(), response.getUpdatedBlockedTimes());
        }
        view.showMessage(response.getMessage());
    }
}