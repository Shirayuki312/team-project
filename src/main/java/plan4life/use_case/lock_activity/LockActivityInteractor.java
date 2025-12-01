package plan4life.use_case.lock_activity;

import plan4life.entities.Schedule;
import plan4life.data_access.ScheduleDataAccessInterface;

public class LockActivityInteractor implements LockActivityInputBoundary {
    private final LockActivityOutputBoundary presenter;
    private final ScheduleDataAccessInterface scheduleDAO;

    public LockActivityInteractor(LockActivityOutputBoundary presenter, ScheduleDataAccessInterface scheduleDAO) {
        this.presenter = presenter;
        this.scheduleDAO = scheduleDAO;
    }

    @Override
    public void execute(LockActivityRequestModel requestModel) {
        int scheduleId = requestModel.getScheduleId();
        Schedule existing = scheduleDAO.getSchedule(scheduleId);
        if (existing == null) {
            // If not found, create a fresh schedule (or handle error via presenter)
            Schedule created = new Schedule(scheduleId, "week");
            created.populateRandomly();
            scheduleDAO.saveSchedule(created);
            presenter.present(new LockActivityResponseModel(created));
            return;
        }

        // Build new schedule preserving locked slots
        Schedule updatedSchedule = existing;

        // Replace locked set with the user selection while keeping all activities/blocks intact
        updatedSchedule.replaceLockedSlotKeys(requestModel.getLockedSlots());

        // Save and present the same schedule so visual blocks remain visible
        scheduleDAO.saveSchedule(updatedSchedule);
        presenter.present(new LockActivityResponseModel(updatedSchedule));
    }
}