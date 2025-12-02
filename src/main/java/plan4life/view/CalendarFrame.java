// 文件路径: src/main/java/plan4life/view/CalendarFrame.java
package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.event.ActionListener;
import java.util.*;

// Import Controllers and Entities
import plan4life.controller.CalendarController;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.use_case.block_off_time.BlockOffTimeController;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;

import java.time.LocalDateTime;
import java.util.Random; // Temp till we get langchain/langgraph working
import java.util.List;

// Import Settings specific classes
import plan4life.controller.SettingsController;
// (ActionEvent is already imported by your teammate)

public class CalendarFrame extends JFrame implements CalendarViewInterface, TimeSelectionListener {

    private final CalendarPanel calendarPanel;
    private final ActivityPanel activityPanel;
    private final JTextArea routineDescriptionArea = new JTextArea(5, 30);
    private final DefaultListModel<String> fixedActivitiesModel = new DefaultListModel<>();
    private final JList<String> fixedActivitiesList = new JList<>(fixedActivitiesModel);
    private final JTextField fixedActivityInput = new JTextField(25);

    private BlockOffTimeController blockOffTimeController;
    private CalendarController calendarController;

    // --- 2. ADD SETTINGS MEMBER VARIABLES ---
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

        // <--- AI Inputs Panel --->
        routineDescriptionArea.setLineWrap(true);
        routineDescriptionArea.setWrapStyleWord(true);
        fixedActivitiesList.setVisibleRowCount(4);
        fixedActivitiesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel routinePanel = new JPanel(new BorderLayout(5, 5));
        routinePanel.add(new JLabel("Routine Description"), BorderLayout.NORTH);
        routinePanel.add(new JScrollPane(routineDescriptionArea), BorderLayout.CENTER);

        JPanel fixedPanel = new JPanel(new BorderLayout(5, 5));
        fixedPanel.add(new JLabel("Fixed activities (e.g., Mon 09:00-10:00 Team Sync)"), BorderLayout.NORTH);
        fixedPanel.add(new JScrollPane(fixedActivitiesList), BorderLayout.CENTER);

        JPanel fixedInputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fixedInputRow.add(fixedActivityInput);
        JButton addFixedBtn = new JButton("Add");
        JButton removeFixedBtn = new JButton("Remove");
        fixedInputRow.add(addFixedBtn);
        fixedInputRow.add(removeFixedBtn);
        fixedPanel.add(fixedInputRow, BorderLayout.SOUTH);

        JPanel inputGrid = new JPanel(new GridLayout(1, 2, 10, 10));
        inputGrid.add(routinePanel);
        inputGrid.add(fixedPanel);

        JPanel generatorInputPanel = new JPanel(new BorderLayout(10, 10));
        // Add a bit of padding so the inputs feel less cramped.
        generatorInputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Schedule Inputs"),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        generatorInputPanel.add(inputGrid, BorderLayout.CENTER);

        // <--- Calendar Panel --->
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
        add(generatorInputPanel, BorderLayout.SOUTH);

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

        addFixedBtn.addActionListener(e -> {
            String input = fixedActivityInput.getText().trim();
            if (input.isEmpty()) {
                showMessage("Enter a fixed activity before adding it to the list.");
                return;
            }
            fixedActivitiesModel.addElement(input);
            fixedActivityInput.setText("");
        });

        removeFixedBtn.addActionListener(e -> {
            int selectedIndex = fixedActivitiesList.getSelectedIndex();
            if (selectedIndex >= 0) {
                fixedActivitiesModel.remove(selectedIndex);
            }
        });

        generateBtn.addActionListener(e -> {
            if (this.calendarController == null) {
                showMessage("Calendar controller is not configured.");
                return;
            }

            String routineText = routineDescriptionArea.getText().trim();
            String fixedActivities = getFixedActivitiesAsText();

            if (routineText.isBlank() && fixedActivities.isBlank()) {
                showMessage("Please enter a routine description or add at least one fixed activity.");
                return;
            }

            calendarController.generateSchedule(routineText, fixedActivities);
        });

        // --- 5. ADD SETTINGS BUTTON LOGIC ---
        settingsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // When the button is clicked, show the SettingsView dialog
                settingsView.setVisible(true);
            }
        });

        updateLanguage("en");
    }

    // --- GETTER FOR ROUTINE DESCRIPTION ---
    public String getRoutineDescription() {
        // TODO: Replace with your actual text field / textarea
        // For now, we pop up a simple input (temporary)
        return JOptionPane.showInputDialog(this, "Describe your routine:");
    }

    // --- GETTER FOR FIXED ACTIVITIES ---
    public Map<String, String> getFixedActivities() {
        Map<String, String> fixed = new HashMap<>();

        for (plan4life.entities.Activity a : currentSchedule.getTasks()) {
            if (a.isFixed()) {
                // key example: "Gym"
                // value example: "14:00:1.5"
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

    /**
     * Injects the CalendarController so this frame can:
     * - lock & regenerate schedules
     * - register events and open the reminder dialog
     */
    public void setCalendarController(CalendarController controller) {

        this.calendarController = controller;
        // NEW: also let ActivityPanel use it for its Set Reminder button
        if (this.activityPanel != null) {
            this.activityPanel.setCalendarController(controller);
        }
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

    private String getFixedActivitiesAsText() {
        List<String> items = Collections.list(fixedActivitiesModel.elements());
        return items.stream()
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .collect(Collectors.joining("\n"));
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

        activityPanel.setActivities(schedule.getActivities().values());

        if (schedule.getBlockedTimes() != null) {
            for (BlockedTime block : schedule.getBlockedTimes()) {
                calendarPanel.colorBlockedRange(block);
            }
        }
        calendarPanel.repaint();
    }

    /**
     * Called when the user selects a time range on the calendar.
     * We use this both to:
     * - block off time (original behavior), and
     * - create an Event and open the reminder dialog (Use Case 7).
     */
    @Override
    public void onTimeSelected(LocalDateTime start, LocalDateTime end, int scheduleId, int columnIndex) {
        String description = JOptionPane.showInputDialog(this,
                "Optional description for this blocked time:");

        if (description == null) {
            calendarPanel.resetDragSelection();
            return;
        }

        // ---------- Reminder flow (Use Case 7) ----------
        if (calendarController != null) {
            // Use the description as the event title (fallback to "Activity")
            String title = description.isBlank() ? "Activity" : description;

            // Create an Event for this time range
            Event event = new Event(title, start, end);

            // 1) Let the controller know this event exists
            calendarController.registerEvent(event);

            // 2) Open the ReminderDialog; when user clicks OK,
            //    ReminderDialog.onOk() will call:
            //    - setImportantReminderForEvent(...)
            //    - or setImportantReminderForAllEvents(...)
            ReminderDialog dialog =
                    new ReminderDialog(this, calendarController, event);
            dialog.setVisible(true);
        }

        blockOffTimeController.blockTime(scheduleId, start, end, description, columnIndex);
    }
}