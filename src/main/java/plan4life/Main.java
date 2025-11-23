package plan4life;

import javax.swing.SwingUtilities;

// 核心架构 Imports
import plan4life.data_access.InMemoryScheduleDAO;
import plan4life.data_access.ScheduleDataAccessInterface;
import plan4life.data_access.InMemoryUserPreferencesDAO;
import plan4life.data_access.UserPreferencesDataAccessInterface;

import plan4life.entities.Schedule;

// Presenters
import plan4life.presenter.CalendarPresenter;
import plan4life.presenter.SettingsPresenter;

// Use Cases
import plan4life.use_case.block_off_time.BlockOffTimeController;
import plan4life.use_case.block_off_time.BlockOffTimeInputBoundary;
import plan4life.use_case.block_off_time.BlockOffTimeInteractor;
import plan4life.use_case.block_off_time.BlockOffTimeOutputBoundary;

import plan4life.use_case.generate_schedule.GenerateScheduleInputBoundary;
import plan4life.use_case.generate_schedule.GenerateScheduleInteractor;
import plan4life.use_case.generate_schedule.GenerateScheduleOutputBoundary;

import plan4life.use_case.lock_activity.LockActivityInputBoundary;
import plan4life.use_case.lock_activity.LockActivityInteractor;
import plan4life.use_case.lock_activity.LockActivityOutputBoundary;

import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;
import plan4life.use_case.set_preferences.SetPreferencesInteractor;
import plan4life.use_case.set_preferences.SetPreferencesOutputBoundary;

// View & Controllers
import plan4life.view.CalendarFrame; // <--- 关键！必须有这一行
import plan4life.controller.CalendarController;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1. Schedule DAO
            ScheduleDataAccessInterface scheduleDAO = new InMemoryScheduleDAO();
            Schedule daySchedule = new Schedule(1, "day");
            Schedule weekSchedule = new Schedule(2, "week");
            scheduleDAO.saveSchedule(daySchedule);
            scheduleDAO.saveSchedule(weekSchedule);

            // 2. Settings 后端组件 (DAO, Presenter, Interactor)
            UserPreferencesDataAccessInterface userPrefsDAO = new InMemoryUserPreferencesDAO();
            SettingsPresenter settingsPresenter = new SettingsPresenter(); // 必须是具体的 SettingsPresenter 类
            SetPreferencesInputBoundary settingsInteractor = new SetPreferencesInteractor(settingsPresenter, userPrefsDAO);

            // 3. 创建 View (注入 settingsInteractor)
            CalendarFrame view = new CalendarFrame(settingsInteractor);

            // [关键] 把 View 塞回给 SettingsPresenter (为了变色和改语言)
            settingsPresenter.setView(view);

            // 4. BlockOffTime 组件
            BlockOffTimeOutputBoundary presenter = new CalendarPresenter(view);
            BlockOffTimeInputBoundary interactor = new BlockOffTimeInteractor(scheduleDAO, presenter);
            BlockOffTimeController controller = new BlockOffTimeController(interactor);

            // 5. GenerateSchedule & LockActivity 组件
            GenerateScheduleOutputBoundary schedulePresenter = new CalendarPresenter(view);
            GenerateScheduleInputBoundary scheduleInput = new GenerateScheduleInteractor(schedulePresenter);

            LockActivityOutputBoundary lock_presenter = new CalendarPresenter(view);
            LockActivityInputBoundary lock_interactor = new LockActivityInteractor(lock_presenter, scheduleDAO);
            CalendarController calendarController = new CalendarController(scheduleInput, lock_interactor);

            // 6. 设置 View 的控制器
            view.setCalendarController(calendarController);
            view.setBlockOffTimeController(controller);

            view.setVisible(true);
        });
    }
}