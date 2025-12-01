package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

// Import Controllers and Entities
import plan4life.controller.CalendarController;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.use_case.block_off_time.BlockOffTimeController;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.List;

// Import Settings specific classes
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

    private Schedule currentSchedule;

    // Track current view state
    private String currentView = "week";


    public CalendarFrame() {
        this((SetPreferencesInputBoundary) null);
    }

    public CalendarFrame(SetPreferencesInputBoundary settingsInteractor) {
        super();

        this.currentSchedule = new Schedule(1, "week");
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

        this.activityPanel = new ActivityPanel(currentSchedule);

        add(topBar, BorderLayout.NORTH);
        add(calendarPanel, BorderLayout.CENTER);
        add(activityPanel, BorderLayout.EAST);

        generateBtn.addActionListener(e -> {
            if (calendarController != null) {

                String routineDescription = getRoutineDescription();
                Map<String, String> fixedActivities = getFixedActivities();
                List<String> freeActivities = getFreeActivities();

                if (routineDescription == null) {
                    showMessage("Schedule generation cancelled.");
                    return;
                }

                calendarController.generateSchedule(routineDescription, fixedActivities, freeActivities);
            }
        });

        dayBtn.addActionListener(e -> {
            calendarPanel.setDayView();
            currentView = "day";
            updateCalendarTitle();
            displaySchedule(currentSchedule);
        });

        weekBtn.addActionListener(e -> {
            calendarPanel.setWeekView();
            currentView = "week";
            updateCalendarTitle();
            displaySchedule(currentSchedule);
        });

        settingsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsView.setVisible(true);
            }
        });

        updateLanguage("en");
    }

    public String getRoutineDescription() {
        return JOptionPane.showInputDialog(this, "Describe your routine:");
    }

    public Map<String, String> getFixedActivities() {
        Map<String, String> fixed = new HashMap<>();

        for (plan4life.entities.Activity a : currentSchedule.getTasks()) {
            if (a.isFixed()) {
                fixed.put(a.getDescription(), a.getStartTime() + ":" + a.getDuration());
            }
        }
        return fixed;
    }

    public List<String> getFreeActivities() {
        return activityPanel.getFreeActivities();
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

    private void updateCalendarTitle() {
        if (bundle == null) return;

        String key = "calendar." + currentView + ".title";
        String fallbackTitle = currentView.equals("day") ? "Daily Calendar" : "Weekly Calendar";

        try {
            String title = bundle.containsKey(key) ? bundle.getString(key) : fallbackTitle;
            calendarPanel.updateTitle(title);
        } catch (Exception e) {
            calendarPanel.updateTitle(fallbackTitle);
        }
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

            updateCalendarTitle();

            revalidate();
            repaint();
        } catch (Exception e) {
            System.err.println("Could not load language bundle: " + e.getMessage());
            dayBtn.setText("Day");
            weekBtn.setText("Week");
            generateBtn.setText("Generate Schedule");
            settingsBtn.setText("Settings");
            updateCalendarTitle();
        }
    }

    @Override
    public void updateTheme(String themeName) {
        SwingUtilities.updateComponentTreeUI(this);

        boolean isDark = "Dark Mode".equals(themeName);
        Color bgColor = isDark ? new Color(40, 40, 40) : Color.WHITE;

        this.getContentPane().setBackground(bgColor);
        if (this.getContentPane() instanceof JComponent) {
            ((JComponent) this.getContentPane()).setOpaque(true);
        }

        calendarPanel.setTheme(themeName);
        activityPanel.setBackground(bgColor);

        this.revalidate();
        this.repaint();
    }

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
                calendarPanel.colorBlockedRange(block);
            }
        }
        calendarPanel.repaint();
    }

    // [Changed] Now handles a list of columns
    @Override
    public void onTimeSelected(LocalDateTime start, LocalDateTime end, int scheduleId, List<Integer> columnIndices) {
        String description = JOptionPane.showInputDialog(this,
                "Optional description for this blocked time:");

        if (description == null) {
            calendarPanel.resetDragSelection();
            return;
        }

        String title = description.isBlank() ? "Activity" : description;

        // 1. Reminder Logic: Only add ONE reminder for the entire block (or per column, here we do once for simplicity or first column)
        // If you want a reminder per day, move this inside the loop.
        // Assuming user wants 1 generic reminder for this multi-day event:
        if (calendarController != null) {
            Event event = new Event(title, start, end);
            calendarController.registerEvent(event);
            ReminderDialog dialog = new ReminderDialog(this, calendarController, event);
            dialog.setVisible(true);
        }

        // 2. Block-Off Logic: Apply to ALL selected columns
        if (blockOffTimeController != null) {
            for (int col : columnIndices) {
                blockOffTimeController.blockTime(
                        scheduleId, start, end, description, col
                );
                calendarPanel.colorBlockedRange(start, end, col, description);
            }
        }
    }
}