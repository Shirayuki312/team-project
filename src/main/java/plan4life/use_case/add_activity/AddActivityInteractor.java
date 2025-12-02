package plan4life.use_case.add_activity;

import plan4life.entities.Activity;
import plan4life.entities.Schedule;
import plan4life.data_access.ScheduleDataAccessInterface;

import java.util.List;

public class AddActivityInteractor implements AddActivityInputBoundary {
    private final ScheduleDataAccessInterface scheduleDAO;
    private final AddActivityOutputBoundary presenter;

    public AddActivityInteractor(ScheduleDataAccessInterface scheduleDAO, AddActivityOutputBoundary presenter) {
        this.scheduleDAO = scheduleDAO;
        this.presenter = presenter;
    }

    @Override
    public AddActivityResponseModel execute(AddActivityRequestModel requestModel) {
        // Retrieve schedule using the scheduleId
        Schedule schedule = scheduleDAO.getSchedule(requestModel.getScheduleId());
        if (schedule == null) {
            return fail("Schedule not found.");
        }

        // Validate input
        if (!isValidDuration(requestModel.getDuration())) {
            return fail("Invalid activity duration.");
        }

        // Create and add new Activity to the schedule
        Activity newActivity = new Activity(
                requestModel.getDescription(),
                requestModel.getDuration()
        );

        schedule.addTask(newActivity);
        scheduleDAO.saveSchedule(schedule);

        // Successful response
        AddActivityResponseModel response = new AddActivityResponseModel(
                true,
                "Activity successfully added.",
                schedule.getTasks(),
                schedule
        );

        presenter.present(response);
        return response;
    }

    private AddActivityResponseModel fail(String message) {
        AddActivityResponseModel response = new AddActivityResponseModel(
                false, message, List.of(), null
        );
        presenter.present(response);
        return response;
    }

    private boolean isValidDuration(float duration) {
        return duration > 0;
    }
}