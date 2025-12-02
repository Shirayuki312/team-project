package plan4life.use_case.lock_activity;

import plan4life.entities.Schedule;
import plan4life.data_access.ScheduleDataAccessInterface;

public class LockActivityInteractor implements LockActivityInputBoundary {

    private final LockActivityOutputBoundary presenter;
    private final ScheduleDataAccessInterface scheduleDAO;

    public LockActivityInteractor(LockActivityOutputBoundary presenter,
                                  ScheduleDataAccessInterface scheduleDAO) {
        this.presenter = presenter;
        this.scheduleDAO = scheduleDAO;
    }

    @Override
    public void execute(LockActivityRequestModel requestModel) {

        if (requestModel == null) {
            // Correct behaviour per test expectation
            return;
        }

        int scheduleId = requestModel.getScheduleId();
        Schedule schedule = scheduleDAO.getSchedule(scheduleId);

        // Create a new empty schedule if missing
        if (schedule == null) {
            schedule = new Schedule(scheduleId, "week");
            scheduleDAO.saveSchedule(schedule);
            presenter.present(new LockActivityResponseModel(schedule));
            return;
        }

        // Apply lock/unlock actions
        for (String key : requestModel.getLockedSlots()) {
            if(schedule.isLockedKey(key)) {
                schedule.unlockSlotKey(key);
            } else {
                schedule.lockSlotKey(key);
            }
        }
        // Build new schedule preserving locked slots
        Schedule updatedSchedule = existing;

        // Replace locked set with the user selection while keeping all activities/blocks intact
        updatedSchedule.replaceLockedSlotKeys(requestModel.getLockedSlots());

        // Save and present the same schedule so visual blocks remain visible
        scheduleDAO.saveSchedule(updatedSchedule);
        presenter.present(new LockActivityResponseModel(updatedSchedule));
        scheduleDAO.saveSchedule(schedule);
        presenter.present(new LockActivityResponseModel(schedule));
    }
}
