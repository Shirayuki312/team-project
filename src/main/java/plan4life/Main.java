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
import plan4life.use_case.generate_schedule.MockScheduleGenerationService;

// --- Use Cases: Lock Activity ---
import plan4life.use_case.lock_activity.LockActivityInputBoundary;
import plan4life.use_case.lock_activity.LockActivityInteractor;
import plan4life.use_case.lock_activity.LockActivityOutputBoundary;

// --- Use Cases: Set Preferences ---
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesInteractor;
import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;

// --- View & Controllers ---
import plan4life.view.CalendarFrame;
import plan4life.controller.CalendarController;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // ============================================================
            // 1. SCHEDULE STORAGE
            // ============================================================
            ScheduleDataAccessInterface scheduleDAO = new InMemoryScheduleDAO();

            // IMPORTANT:
            // Must create schedules using new constructor (id + type)
            scheduleDAO.saveSchedule(new Schedule(1, "day"));
            scheduleDAO.saveSchedule(new Schedule(2, "week"));

            // ============================================================
            // 2. SETTINGS FEATURE
            // ============================================================
            UserPreferencesDataAccessInterface userPrefsDAO = new InMemoryUserPreferencesDAO();
            SettingsPresenter settingsPresenter = new SettingsPresenter();
            SetPreferencesInputBoundary settingsInteractor =
                    new SetPreferencesInteractor(settingsPresenter, userPrefsDAO);

            // ============================================================
            // 3. VIEW
            // ============================================================
            CalendarFrame view = new CalendarFrame(settingsInteractor);
            settingsPresenter.setView(view);

            // ============================================================
            // 4. BLOCK-OFF-TIME FEATURE
            // ============================================================
            BlockOffTimeOutputBoundary blockPresenter = new CalendarPresenter(view);
            BlockOffTimeInputBoundary blockInteractor =
                    new BlockOffTimeInteractor(scheduleDAO, blockPresenter);
            BlockOffTimeController blockController =
                    new BlockOffTimeController(blockInteractor);

            // ============================================================
            // 5. GENERATE-SCHEDULE + LOCK LOGIC
            // ============================================================
            GenerateScheduleOutputBoundary schedulePresenter = new CalendarPresenter(view);

            // NEW generator supporting duration and dayIndex keys
            ScheduleGenerationService generator =
                    new MockScheduleGenerationService();

            GenerateScheduleInputBoundary scheduleInput =
                    new GenerateScheduleInteractor(schedulePresenter, generator);

            LockActivityOutputBoundary lockPresenter = new CalendarPresenter(view);
            LockActivityInputBoundary lockInteractor =
                    new LockActivityInteractor(lockPresenter, scheduleDAO);

            CalendarController calendarController =
                    new CalendarController(scheduleInput, lockInteractor);

            // ============================================================
            // 6. CONNECT VIEW → CONTROLLERS
            // ============================================================
            view.setCalendarController(calendarController);
            view.setBlockOffTimeController(blockController);

            // ============================================================
            // 7. SHOW UI
            // ============================================================
            view.setVisible(true);
        });
    }
}