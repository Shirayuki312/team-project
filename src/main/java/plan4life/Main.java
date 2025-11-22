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
import plan4life.data_access.InMemoryUserPreferencesDAO;
import plan4life.data_access.UserPreferencesDataAccessInterface;
import plan4life.presenter.SettingsPresenter;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesInteractor;
import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ScheduleDataAccessInterface scheduleDAO = new InMemoryScheduleDAO();
            Schedule daySchedule = new Schedule(1, "day");
            Schedule weekSchedule = new Schedule(2, "week");
            scheduleDAO.saveSchedule(daySchedule);
            scheduleDAO.saveSchedule(weekSchedule);

            UserPreferencesDataAccessInterface userPrefsDAO = new InMemoryUserPreferencesDAO();

            // B. 创建 Presenter (负责显示成功/失败弹窗)
            SetPreferencesOutputBoundary settingsPresenter = new SettingsPresenter();

            // C. 创建 Interactor (核心逻辑，连接 DAO 和 Presenter)
            SetPreferencesInputBoundary settingsInteractor = new SetPreferencesInteractor(settingsPresenter, userPrefsDAO);


            // ==========================================
            // 3. [修改] 创建 View (CalendarFrame)
            // ==========================================
            // 注意：我们这里把 settingsInteractor 传进去了！
            // (这就要求你必须修改 CalendarFrame 的构造函数，见下文提示)
            CalendarFrame view = new CalendarFrame(settingsInteractor);

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
    }
}