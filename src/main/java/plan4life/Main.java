package plan4life;

import javax.swing.SwingUtilities;

// --- Core Architecture Imports ---
import plan4life.data_access.*;

import plan4life.entities.Schedule;
import plan4life.ai.LlmScheduleService;
import plan4life.ai.PromptBuilder;
import plan4life.ai.RagRetriever;
import plan4life.solver.ConstraintSolver;

// --- Presenters ---
import plan4life.presenter.CalendarPresenter;
import plan4life.presenter.SetReminderPresenter;
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

// --- Use Cases: Set Preferences ---
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesInteractor;
import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;

// --- Use Cases: Set Reminder ---
import plan4life.data_access.InMemoryReminderDAO;
import plan4life.data_access.ReminderDataAccessInterface;
import plan4life.presenter.SetReminderPresenter;
import plan4life.use_case.set_reminder.SetReminderInputBoundary;
import plan4life.use_case.set_reminder.SetReminderInteractor;
import plan4life.use_case.set_reminder.SetReminderOutputBoundary;

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
            RagRetriever ragRetriever = new RagRetriever();
            PromptBuilder promptBuilder = new PromptBuilder(ragRetriever);
            LlmScheduleService llmScheduleService = new LlmScheduleService(promptBuilder);
            String configuredModel = LlmScheduleService.resolveModelId();
            boolean hasApiKey = System.getenv("HUGGINGFACE_API_KEY") != null
                    && !System.getenv("HUGGINGFACE_API_KEY").isBlank();
            System.out.printf("[Main] LLM configured model: %s (API key present: %b)%n", configuredModel, hasApiKey);
            ConstraintSolver constraintSolver = new ConstraintSolver();
            GenerateScheduleInputBoundary scheduleInput = new GenerateScheduleInteractor(
                    schedulePresenter,
                    ragRetriever,
                    llmScheduleService,
                    constraintSolver,
                    scheduleDAO);

            // ============================================================
            // 6. set reminder
            // ============================================================
            ReminderDataAccessInterface reminderDAO = new InMemoryReminderDAO();
            SetReminderOutputBoundary reminderPresenter = new SetReminderPresenter();
            SetReminderInputBoundary setReminderInteractor =
                    new SetReminderInteractor(reminderDAO, reminderPresenter);

            // NEW generator supporting duration and dayIndex keys
            ScheduleGenerationService generator =
                    new MockScheduleGenerationService();

            GenerateScheduleInputBoundary scheduleInput =
                    new GenerateScheduleInteractor(schedulePresenter, generator);

            LockActivityOutputBoundary lockPresenter = new CalendarPresenter(view);
            LockActivityInputBoundary lockInteractor =
                    new LockActivityInteractor(lockPresenter, scheduleDAO);

            CalendarController calendarController =
                    new CalendarController(scheduleInput, lockInteractor, setReminderInteractor);

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
