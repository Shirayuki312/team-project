package plan4life;

import plan4life.controller.CalendarController;
import plan4life.data_access.*;
import plan4life.entities.Schedule;
import plan4life.presenter.CalendarPresenter;
import plan4life.use_case.block_off_time.*;
import plan4life.use_case.generate_schedule.GenerateScheduleInputBoundary;
import plan4life.use_case.generate_schedule.GenerateScheduleInteractor;
import plan4life.use_case.generate_schedule.GenerateScheduleOutputBoundary;
import plan4life.use_case.generate_schedule.ScheduleGenerationService;
import plan4life.use_case.generate_schedule.MockScheduleGenerationService;
// Mock Schedule Generator temp till fully implemented LLM.

import plan4life.view.CalendarFrame;
import plan4life.use_case.lock_activity.*;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ScheduleDataAccessInterface scheduleDAO = new InMemoryScheduleDAO();
            Schedule daySchedule = new Schedule(1, "day");
            Schedule weekSchedule = new Schedule(2, "week");
            scheduleDAO.saveSchedule(daySchedule);
            scheduleDAO.saveSchedule(weekSchedule);

            CalendarFrame view = new CalendarFrame(); // temp
            BlockOffTimeOutputBoundary presenter = new CalendarPresenter(view);
            BlockOffTimeInputBoundary interactor = new BlockOffTimeInteractor(scheduleDAO, presenter);
            BlockOffTimeController controller = new BlockOffTimeController(interactor);


            // Added GenerateSchedule and LockActivity to main.
            // GenerateSchedule still needs work, needs to take 1 more input.
            CalendarController calendarController = getCalendarController(view, scheduleDAO);

            view.setCalendarController(calendarController);
            view.setBlockOffTimeController(controller);
            view.setVisible(true);
        });
    }

    private static CalendarController
    getCalendarController(CalendarFrame view, ScheduleDataAccessInterface scheduleDAO) {
        GenerateScheduleOutputBoundary schedulePresenter = new CalendarPresenter(view);

        // mock schedule generator here
        ScheduleGenerationService generationService = new MockScheduleGenerationService();

        GenerateScheduleInputBoundary scheduleInput =
                new GenerateScheduleInteractor(schedulePresenter, generationService);

        LockActivityOutputBoundary lockPresenter = new CalendarPresenter(view);
        LockActivityInputBoundary lockInteractor = new LockActivityInteractor(lockPresenter, scheduleDAO);
        return new CalendarController(scheduleInput, lockInteractor);
    }
}