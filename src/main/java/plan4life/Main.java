package plan4life;

import plan4life.controller.CalendarController;
import plan4life.data_access.*;
import plan4life.entities.Schedule;
import plan4life.presenter.CalendarPresenter;
import plan4life.use_case.block_off_time.*;
import plan4life.use_case.generate_schedule.GenerateScheduleInputBoundary;
import plan4life.use_case.generate_schedule.GenerateScheduleInteractor;
import plan4life.use_case.generate_schedule.GenerateScheduleOutputBoundary;
import plan4life.view.CalendarFrame;
import plan4life.use_case.lock_activity.*;

import plan4life.controller.CalendarController;
import plan4life.view.CalendarFrame;

import plan4life.use_case.generate_schedule.GenerateScheduleInputBoundary;
import plan4life.use_case.generate_schedule.GenerateScheduleRequestModel;
import plan4life.use_case.lock_activity.LockActivityInputBoundary;
import plan4life.use_case.lock_activity.LockActivityRequestModel;

import javax.swing.SwingUtilities;

import java.time.LocalDateTime;

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


            // Added GenerateSchedule and LockActivity to main. GenerateSchedule still needs work, needs to take 1 more input
            GenerateScheduleOutputBoundary schedulePresenter = new CalendarPresenter(view);
            GenerateScheduleInputBoundary scheduleInput = new GenerateScheduleInteractor(schedulePresenter);

            LockActivityOutputBoundary lock_presenter = new CalendarPresenter(view);
            LockActivityInputBoundary lock_interactor = new LockActivityInteractor(lock_presenter, scheduleDAO);
            CalendarController calendarController = new CalendarController(scheduleInput, lock_interactor);

            view.setCalendarController(calendarController);
            view.setBlockOffTimeController(controller);
            view.setVisible(true);
        });

        GenerateScheduleInputBoundary dummyGenerate = new GenerateScheduleInputBoundary() {
            @Override
            public void execute(GenerateScheduleRequestModel requestModel) {
                System.out.println("[DummyGenerate] generateSchedule called.");
            }
        };

        LockActivityInputBoundary dummyLock = new LockActivityInputBoundary() {
            @Override
            public void execute(LockActivityRequestModel requestModel) {
                System.out.println("[DummyLock] lockAndRegenerate called.");
            }
        };

        CalendarController controller = new CalendarController(dummyGenerate, dummyLock);

        SwingUtilities.invokeLater(() -> {
            CalendarFrame frame = new CalendarFrame();
            frame.setCalendarController(controller);
            frame.setVisible(true);
        });
    }
}
