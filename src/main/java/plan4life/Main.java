package plan4life;

import javax.swing.SwingUtilities;

// --- Core Architecture Imports ---
import plan4life.data_access.InMemoryScheduleDAO;
import plan4life.data_access.ScheduleDataAccessInterface;
import plan4life.data_access.InMemoryUserPreferencesDAO;
import plan4life.data_access.UserPreferencesDataAccessInterface;

import plan4life.entities.Schedule;

// --- Presenters ---
import plan4life.presenter.CalendarPresenter;
import plan4life.presenter.SettingsPresenter;

// --- Use Cases: Block Off Time ---
import plan4life.use_case.block_off_time.BlockOffTimeController;
import plan4life.use_case.block_off_time.BlockOffTimeInputBoundary;
import plan4life.use_case.block_off_time.BlockOffTimeInteractor;
import plan4life.use_case.block_off_time.BlockOffTimeOutputBoundary;

// --- Use Cases: Generate Schedule ---
import plan4life.use_case.generate_schedule.GenerateScheduleInputBoundary;
import plan4life.use_case.generate_schedule.GenerateScheduleInteractor;
import plan4life.use_case.generate_schedule.GenerateScheduleOutputBoundary;
import plan4life.use_case.generate_schedule.ScheduleGenerationService;
// [关键修复] 必须导入这个 Mock 类
import plan4life.use_case.generate_schedule.MockScheduleGenerationService;

// --- Use Cases: Lock Activity ---
import plan4life.use_case.lock_activity.LockActivityInputBoundary;
import plan4life.use_case.lock_activity.LockActivityInteractor;
import plan4life.use_case.lock_activity.LockActivityOutputBoundary;

// --- Use Cases: Set Preferences (Your Feature) ---
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesInteractor;
import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;

// --- View & Controllers ---
import plan4life.view.CalendarFrame;
import plan4life.controller.CalendarController;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // 1. Initialize Schedule Data Access
            ScheduleDataAccessInterface scheduleDAO = new InMemoryScheduleDAO();
            // Pre-load some dummy schedules for testing
            Schedule daySchedule = new Schedule(1, "day");
            Schedule weekSchedule = new Schedule(2, "week");
            scheduleDAO.saveSchedule(daySchedule);
            scheduleDAO.saveSchedule(weekSchedule);

            // ============================================================
            // 2. Initialize Settings Feature (Your Backend)
            // ============================================================
            // A. Create Data Access for Preferences
            UserPreferencesDataAccessInterface userPrefsDAO = new InMemoryUserPreferencesDAO();

            // B. Create Presenter (Handles success/failure messages)
            SettingsPresenter settingsPresenter = new SettingsPresenter();

            // C. Create Interactor (Connects DAO and Presenter)
            SetPreferencesInputBoundary settingsInteractor = new SetPreferencesInteractor(settingsPresenter, userPrefsDAO);


            // ============================================================
            // 3. Initialize View (Injecting Settings Interactor)
            // ============================================================
            CalendarFrame view = new CalendarFrame(settingsInteractor);

            // [Critical] Pass the View back to the SettingsPresenter
            // This allows the Presenter to update the UI (Dark Mode/Language)
            settingsPresenter.setView(view);


            // ============================================================
            // 4. Initialize Block Off Time Feature
            // ============================================================
            BlockOffTimeOutputBoundary blockPresenter = new CalendarPresenter(view);
            BlockOffTimeInputBoundary blockInteractor = new BlockOffTimeInteractor(scheduleDAO, blockPresenter);
            BlockOffTimeController blockController = new BlockOffTimeController(blockInteractor);


            // ============================================================
            // 5. Initialize Generate Schedule & Lock Activity Features
            // ============================================================
            GenerateScheduleOutputBoundary schedulePresenter = new CalendarPresenter(view);

            // [关键修复] 使用 Mock 类，并且去掉了参数 (通常 Mock 不需要 DAO)
            ScheduleGenerationService scheduleGenerator = new MockScheduleGenerationService();

            GenerateScheduleInputBoundary scheduleInput = new GenerateScheduleInteractor(schedulePresenter, scheduleGenerator);

            LockActivityOutputBoundary lockPresenter = new CalendarPresenter(view);
            LockActivityInputBoundary lockInteractor = new LockActivityInteractor(lockPresenter, scheduleDAO);

            // Create the main Calendar Controller
            CalendarController calendarController = new CalendarController(scheduleInput, lockInteractor);


            // ============================================================
            // 6. Finalize View Setup
            // ============================================================
            view.setCalendarController(calendarController);
            view.setBlockOffTimeController(blockController);

            view.setVisible(true);
        });
    }
}