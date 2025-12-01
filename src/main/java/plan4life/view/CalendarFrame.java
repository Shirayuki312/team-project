// 文件路径: src/main/java/plan4life/view/CalendarFrame.java
package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import plan4life.controller.CalendarController; // imported controller
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.use_case.block_off_time.BlockOffTimeController;

import java.time.LocalDateTime;
import java.util.Random; //Temp till we get langchain/langgraph working

// --- 1. IMPORT SETTINGS CLASSES ---
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
    private Schedule currentSchedule;
    private CalendarController calendarController; // added controller

    // --- 2. ADD SETTINGS MEMBER VARIABLES ---
    private SettingsView settingsView;
    private SettingsController settingsController;


    public CalendarFrame() {
        super("Plan4Life - Scheduler");

        // --- 3. INITIALIZE SETTINGS CLASSES (at the top) ---
        // (This MUST be done before they are used)
        this.settingsView = new SettingsView(this);
        this.settingsController = new SettingsController(this.settingsView);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLayout(new BorderLayout(10, 10));

        // <--- Top part with Day/Week and Generate Schedule buttons --->
        JPanel topBar = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton dayBtn = new JButton("Day");
        JButton weekBtn = new JButton("Week");
        leftPanel.add(dayBtn);
        leftPanel.add(weekBtn);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton generateBtn = new JButton("Generate Schedule");

        // --- 4. ADD SETTINGS BUTTON TO THE UI ---
        JButton settingsBtn = new JButton("Settings");

        rightPanel.add(generateBtn);
        rightPanel.add(settingsBtn); // Add the button to the panel

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
        generatorInputPanel.setBorder(BorderFactory.createTitledBorder("Schedule Inputs"));
        generatorInputPanel.add(inputGrid, BorderLayout.CENTER);

        // <--- Calendar Panel --->
        this.calendarPanel = new CalendarPanel();
        calendarPanel.setTimeSelectionListener(this);

        calendarPanel.setLockListener(timeKey -> {
            if (currentSchedule == null) return;

            // Toggle lock state
            if (currentSchedule.isLockedKey(timeKey)) {
                currentSchedule.unlockSlotKey(timeKey);
            } else {
                currentSchedule.lockSlotKey(timeKey);
            }

            // Call your controller (must exist in the Frame)
            if (blockOffTimeController != null) {
                // If your controller regenerates based on block-offs,
                // call it here (optional depending on final design)
            }

            // Call your schedule-locking controller
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
        add(generatorInputPanel, BorderLayout.SOUTH);

        // --- Button Logic ---
        dayBtn.addActionListener(e -> {
            calendarPanel.setDayView();
            displaySchedule(currentSchedule);
        });

        weekBtn.addActionListener(e -> {
            calendarPanel.setWeekView();
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
    }

    public CalendarFrame(BlockOffTimeController blockOffTimeController) {
        this();
        this.blockOffTimeController = blockOffTimeController;
    }

    public void setBlockOffTimeController(BlockOffTimeController controller) {
        this.blockOffTimeController = controller;
    }

    public void setCalendarController(CalendarController controller) {
        this.calendarController = controller;
    }

    private String getFixedActivitiesAsText() {
        List<String> items = Collections.list(fixedActivitiesModel.elements());
        return items.stream()
                .map(String::trim)
                .filter(entry -> !entry.isBlank())
                .collect(Collectors.joining("\n"));
    }


    @Override
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    @Override
    public void displaySchedule(Schedule schedule) {
        this.currentSchedule = schedule;
        if (schedule == null) return;

        // This is where you’ll color each entry in the schedule
        // For now, until Activities exist, let’s simulate visually:
        calendarPanel.clear();

        Random random = new Random(); //Temp till we get langchain/langgraph working

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
            return; // user pressed Cancel
        }

        blockOffTimeController.blockTime(scheduleId, start, end, description, columnIndex);
    }
}