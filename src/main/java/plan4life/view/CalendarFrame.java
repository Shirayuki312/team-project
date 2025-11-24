package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.ResourceBundle;

import plan4life.controller.CalendarController;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.use_case.block_off_time.BlockOffTimeController;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;

import java.time.LocalDateTime;
import java.util.Random;

import plan4life.controller.SettingsController;

public class CalendarFrame extends JFrame implements CalendarViewInterface, TimeSelectionListener {

    private final CalendarPanel calendarPanel;
    private final ActivityPanel activityPanel;

    private BlockOffTimeController blockOffTimeController;
    private CalendarController calendarController;

    private SettingsView settingsView;
    private SettingsController settingsController;
    private SetPreferencesInputBoundary settingsInteractor;

    private ResourceBundle bundle;
    private JButton dayBtn;
    private JButton weekBtn;
    private JButton generateBtn;
    private JButton settingsBtn;

    // 1. Default Constructor
    public CalendarFrame() {
        this((SetPreferencesInputBoundary) null);
    }

    // 2. Main Constructor
    public CalendarFrame(SetPreferencesInputBoundary settingsInteractor) {
        super();

        this.settingsInteractor = settingsInteractor;
        this.settingsView = new SettingsView(this);
        this.settingsController = new SettingsController(this.settingsView, this.settingsInteractor);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLayout(new BorderLayout(10, 10));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dayBtn = new JButton();
        weekBtn = new JButton();
        leftPanel.add(dayBtn);
        leftPanel.add(weekBtn);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generateBtn = new JButton();
        settingsBtn = new JButton();
        rightPanel.add(generateBtn);
        rightPanel.add(settingsBtn);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        this.calendarPanel = new CalendarPanel();
        calendarPanel.setTimeSelectionListener(this);

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

        this.activityPanel = new ActivityPanel();

        add(topBar, BorderLayout.NORTH);
        add(calendarPanel, BorderLayout.CENTER);
        add(activityPanel, BorderLayout.EAST);

        dayBtn.addActionListener(e -> {
            calendarPanel.setDayView();
            displaySchedule(currentSchedule);
        });

        weekBtn.addActionListener(e -> {
            calendarPanel.setWeekView();
            displaySchedule(currentSchedule);
        });

        settingsBtn.addActionListener(e -> {
            settingsView.setVisible(true);
        });

        updateLanguage("en");
    }

    public CalendarFrame(BlockOffTimeController blockOffTimeController) {
        this((SetPreferencesInputBoundary) null);
        this.blockOffTimeController = blockOffTimeController;
    }

    public void setBlockOffTimeController(BlockOffTimeController controller) {
        this.blockOffTimeController = controller;
    }

    public void setCalendarController(CalendarController controller) {
        this.calendarController = controller;
    }

    @Override
    public void updateLanguage(String languageCode) {
        Locale locale;
        if ("zh".equalsIgnoreCase(languageCode) || "简体中文".equals(languageCode)) {
            locale = new Locale("zh", "CN");
        } else if ("fr".equalsIgnoreCase(languageCode) || "Français".equals(languageCode)) {
            locale = new Locale("fr", "FR");
        } else {
            locale = new Locale("en", "US");
        }

        try {
            this.bundle = ResourceBundle.getBundle("messages", locale);
            setTitle(bundle.getString("app.title"));
            dayBtn.setText(bundle.getString("btn.day"));
            weekBtn.setText(bundle.getString("btn.week"));
            generateBtn.setText(bundle.getString("btn.generate"));
            settingsBtn.setText(bundle.getString("btn.settings"));
            activityPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("label.activities")));
            revalidate();
            repaint();
        } catch (Exception e) {
            System.err.println("Could not load language bundle: " + e.getMessage());
            dayBtn.setText("Day");
            weekBtn.setText("Week");
        }
    }

    @Override
    public void updateTheme(String themeName) {
        // 1. Refresh Look and Feel
        SwingUtilities.updateComponentTreeUI(this);

        boolean isDark = "Dark Mode".equals(themeName);
        Color bgColor = isDark ? new Color(40, 40, 40) : Color.WHITE;

        // 2. Set Background
        this.getContentPane().setBackground(bgColor);
        if (this.getContentPane() instanceof JComponent) {
            ((JComponent) this.getContentPane()).setOpaque(true);
        }

        // 3. Update Panels
        calendarPanel.setTheme(themeName);
        activityPanel.setBackground(bgColor);

        // 4. Redraw
        this.revalidate();
        this.repaint();
    }

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

        // [关键修复] 使用 1 个参数的方法，而不是 3 个
        if (schedule.getBlockedTimes() != null) {
            for (BlockedTime block : schedule.getBlockedTimes()) {
                calendarPanel.colorBlockedRange(block);
            }
        }
        calendarPanel.repaint();
    }

    @Override
    public void onTimeSelected(LocalDateTime start, LocalDateTime end, int scheduleId, int columnIndex) {
        String description = JOptionPane.showInputDialog(this,
                "Optional description for this blocked time:");

        if (description == null) {
            // Cancelled -> Reset visual
            calendarPanel.resetDragSelection();
            return;
        }

        if (blockOffTimeController != null) {
            // 1. Backend Update
            blockOffTimeController.blockTime(scheduleId, start, end, description, columnIndex);

            // 2. Visual Update (Immediate)
            calendarPanel.colorBlockedRange(start, end, columnIndex, description);
        }
    }
}