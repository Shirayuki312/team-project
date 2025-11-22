package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import plan4life.controller.CalendarController;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.use_case.block_off_time.BlockOffTimeController;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary; // <--- 1. 导入 InputBoundary

import java.time.LocalDateTime;
import java.util.Random;

// Settings Classes
import plan4life.controller.SettingsController;

public class CalendarFrame extends JFrame implements CalendarViewInterface, TimeSelectionListener {

    private final CalendarPanel calendarPanel;
    private final ActivityPanel activityPanel;

    // Controllers for other features
    private BlockOffTimeController blockOffTimeController;
    private CalendarController calendarController;

    // --- Settings Variables ---
    private SettingsView settingsView;
    private SettingsController settingsController;
    private SetPreferencesInputBoundary settingsInteractor; // <--- 2. 添加 Interactor 字段


    // --- 3. 默认构造函数 (为了兼容性，但建议使用下面的带参数构造函数) ---
    public CalendarFrame() {
        this((SetPreferencesInputBoundary) null); // 正确：指定类型
    }

    // --- 4. [核心修改] 带 Interactor 的构造函数 ---
    // Main.java 会调用这个构造函数
    public CalendarFrame(SetPreferencesInputBoundary settingsInteractor) {
        super("Plan4Life - Scheduler");

        // 保存 Interactor
        this.settingsInteractor = settingsInteractor;

        // --- 初始化 Settings 组件 ---
        // 关键点：我们将 Interactor 传给了 Controller
        this.settingsView = new SettingsView(this);
        this.settingsController = new SettingsController(this.settingsView, this.settingsInteractor);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLayout(new BorderLayout(10, 10));

        // <--- Top part with Day/Week/Generate/Settings buttons --->
        JPanel topBar = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton dayBtn = new JButton("Day");
        JButton weekBtn = new JButton("Week");
        leftPanel.add(dayBtn);
        leftPanel.add(weekBtn);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton generateBtn = new JButton("Generate Schedule");

        // 添加 Settings 按钮
        JButton settingsBtn = new JButton("Settings");

        rightPanel.add(generateBtn);
        rightPanel.add(settingsBtn);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        // <--- Calendar Panel --->
        this.calendarPanel = new CalendarPanel();
        calendarPanel.setTimeSelectionListener(this);

        // 设置锁定监听器 (你队友的逻辑)
        calendarPanel.setLockListener(timeKey -> {
            if (currentSchedule == null) return;

            if (currentSchedule.isLockedKey(timeKey)) {
                currentSchedule.unlockSlotKey(timeKey);
            } else {
                currentSchedule.lockSlotKey(timeKey);
            }

            if (this.calendarController != null) {
                calendarController.lockAndRegenerate(
                        currentSchedule.getScheduleId(),
                        currentSchedule.getLockedSlotKeys()
                );
            }
        });

        // <--- Activities Panel --->
        this.activityPanel = new ActivityPanel();

        // --- Add to frame ---
        add(topBar, BorderLayout.NORTH);
        add(calendarPanel, BorderLayout.CENTER);
        add(activityPanel, BorderLayout.EAST);

        // --- Button Logic ---
        dayBtn.addActionListener(e -> {
            calendarPanel.setDayView();
            displaySchedule(currentSchedule);
        });

        weekBtn.addActionListener(e -> {
            calendarPanel.setWeekView();
            displaySchedule(currentSchedule);
        });

        // Settings 按钮逻辑
        settingsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsView.setVisible(true);
            }
        });
    }

    // --- 用于 BlockOffTimeController 的辅助构造函数 (你队友的旧逻辑) ---
    public CalendarFrame(BlockOffTimeController blockOffTimeController) {
        this((SetPreferencesInputBoundary) null); // 正确
        this.blockOffTimeController = blockOffTimeController;
    }

    // --- Setters ---
    public void setBlockOffTimeController(BlockOffTimeController controller) {
        this.blockOffTimeController = controller;
    }

    public void setCalendarController(CalendarController controller) {
        this.calendarController = controller;
    }

    // --- View Interface Methods ---
    private Schedule currentSchedule;

    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    @Override
    public void displaySchedule(Schedule schedule) {
        this.currentSchedule = schedule;
        if (schedule == null) return;

        calendarPanel.clear();
        Random random = new Random();

        schedule.getActivities().forEach((time, activityName) -> {
            boolean isLocked = currentSchedule.isLockedKey(time);
            Color color = new Color(random.nextInt(156) + 100,
                    random.nextInt(156) + 100,
                    random.nextInt(156) + 100);
            calendarPanel.colorCell(time, color, activityName, isLocked);
        });

        if (schedule.getBlockedTimes() != null) {
            for (BlockedTime block : schedule.getBlockedTimes()) {
                calendarPanel.colorBlockedRange(
                        block.getStart(),
                        block.getEnd(),
                        block.getColumnIndex()
                );
            }
        }
        calendarPanel.repaint();
    }

    @Override
    public void onTimeSelected(LocalDateTime start, LocalDateTime end, int scheduleId, int columnIndex) {
        String description = JOptionPane.showInputDialog(this,
                "Optional description for this blocked time:");

        if (description == null) {
            return;
        }
        if (blockOffTimeController != null) {
            blockOffTimeController.blockTime(scheduleId, start, end, description, columnIndex);
        }
    }
}