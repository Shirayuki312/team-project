package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.ResourceBundle;

// Import Controllers and Entities
import plan4life.controller.CalendarController;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.use_case.block_off_time.BlockOffTimeController;
import plan4life.use_case.set_preferences.SetPreferencesInputBoundary;

import java.time.LocalDateTime;
import java.util.Random;

// Import Settings specific classes
import plan4life.controller.SettingsController;

/**
 * The main window of the application.
 * It acts as the "View" in the MVP/Clean Architecture pattern.
 * It implements CalendarViewInterface to allow the Presenter to update the UI (e.g., change language/theme).
 */
public class CalendarFrame extends JFrame implements CalendarViewInterface, TimeSelectionListener {

    // --- UI Components ---
    private final CalendarPanel calendarPanel;
    private final ActivityPanel activityPanel;

    // --- Controllers for other features ---
    private BlockOffTimeController blockOffTimeController;
    private CalendarController calendarController;

    // --- Settings Feature Components ---
    private SettingsView settingsView;
    private SettingsController settingsController;
    // The InputBoundary allows the View to send data to the Use Case (Interactor)
    private SetPreferencesInputBoundary settingsInteractor;

    // --- Internationalization (i18n) Variables ---
    private ResourceBundle bundle; // Holds the translated strings
    // Buttons are class-level variables so their text can be updated dynamically
    private JButton dayBtn;
    private JButton weekBtn;
    private JButton generateBtn;
    private JButton settingsBtn;


    /**
     * Default Constructor.
     * Used for backward compatibility or testing.
     * Calls the main constructor with null, casting it to avoid ambiguity errors.
     */
    public CalendarFrame() {
        this((SetPreferencesInputBoundary) null);
    }

    /**
     * Main Constructor.
     * This is called by Main.java to inject the Settings Interactor.
     *
     * @param settingsInteractor The backend logic for saving settings.
     */
    public CalendarFrame(SetPreferencesInputBoundary settingsInteractor) {
        super(); // Title is set later in updateLanguage()

        // 1. Dependency Injection: Save the interactor
        this.settingsInteractor = settingsInteractor;

        // 2. Initialize Settings Components
        // We pass 'this' (CalendarFrame) as the parent window for the dialog
        this.settingsView = new SettingsView(this);
        // We pass the interactor to the controller so the "Save" button works
        this.settingsController = new SettingsController(this.settingsView, this.settingsInteractor);

        // 3. Basic Frame Setup
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLayout(new BorderLayout(10, 10));

        // 4. UI Layout: Top Bar
        JPanel topBar = new JPanel(new BorderLayout());

        // Left side buttons
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dayBtn = new JButton(); // Text set in updateLanguage
        weekBtn = new JButton();
        leftPanel.add(dayBtn);
        leftPanel.add(weekBtn);

        // Right side buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generateBtn = new JButton();
        settingsBtn = new JButton();

        rightPanel.add(generateBtn);
        rightPanel.add(settingsBtn);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);

        // 5. UI Layout: Calendar Grid (Center)
        this.calendarPanel = new CalendarPanel();
        calendarPanel.setTimeSelectionListener(this);

        // Logic from teammates: Handle locking/unlocking time slots
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

        // 6. UI Layout: Activity Panel (Right)
        this.activityPanel = new ActivityPanel();

        // Add all panels to the main frame
        add(topBar, BorderLayout.NORTH);
        add(calendarPanel, BorderLayout.CENTER);
        add(activityPanel, BorderLayout.EAST);

        // --- Event Listeners ---

        dayBtn.addActionListener(e -> {
            calendarPanel.setDayView();
            displaySchedule(currentSchedule);
        });

        weekBtn.addActionListener(e -> {
            calendarPanel.setWeekView();
            displaySchedule(currentSchedule);
        });

        // When "Settings" is clicked, show the popup dialog
        settingsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsView.setVisible(true);
            }
        });

        // 7. Initialize default language (English) on startup
        updateLanguage("en");
    }

    /**
     * Auxiliary Constructor for BlockOffTimeController.
     * Kept for compatibility with existing teammate code.
     */
    public CalendarFrame(BlockOffTimeController blockOffTimeController) {
        this((SetPreferencesInputBoundary) null);
        this.blockOffTimeController = blockOffTimeController;
    }

    // --- Setters for Dependency Injection ---

    public void setBlockOffTimeController(BlockOffTimeController controller) {
        this.blockOffTimeController = controller;
    }

    public void setCalendarController(CalendarController controller) {
        this.calendarController = controller;
    }

    // --- Interface Methods (CalendarViewInterface) ---

    /**
     * Updates the language of the entire UI dynamically.
     * Called by SettingsPresenter when settings are saved.
     *
     * @param languageCode The code for the language (e.g., "en", "zh", "fr").
     */
    @Override
    public void updateLanguage(String languageCode) {
        Locale locale;

        // Determine the correct Locale based on the selection
        if ("zh".equalsIgnoreCase(languageCode) || "简体中文".equals(languageCode)) {
            locale = new Locale("zh", "CN");
        } else if ("fr".equalsIgnoreCase(languageCode) || "Français".equals(languageCode)) {
            locale = new Locale("fr", "FR");
        } else {
            locale = new Locale("en", "US"); // Default
        }

        try {
            // Load the resource bundle.
            // IMPORTANT: Use "messages" NOT "messages.properties"
            this.bundle = ResourceBundle.getBundle("messages", locale);

            // Update all text components from the bundle
            setTitle(bundle.getString("app.title"));
            dayBtn.setText(bundle.getString("btn.day"));
            weekBtn.setText(bundle.getString("btn.week"));
            generateBtn.setText(bundle.getString("btn.generate"));
            settingsBtn.setText(bundle.getString("btn.settings"));

            // Update TitledBorder for panels
            activityPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("label.activities")));

            // Refresh the UI to reflect changes
            revalidate();
            repaint();

        } catch (Exception e) {
            // Fallback in case resource file is missing
            System.err.println("Could not load language bundle: " + e.getMessage());
            dayBtn.setText("Day");
            weekBtn.setText("Week");
        }
    }

    /**
     * Updates the theme (Dark/Light mode) dynamically.
     * Called by SettingsPresenter when settings are saved.
     *
     * @param themeName The name of the theme (e.g., "Dark Mode", "Light Mode").
     */
    @Override
    public void updateTheme(String themeName) {
        boolean isDark = "Dark Mode".equals(themeName);

        // 1. Set the main window background
        Color frameBg = isDark ? new Color(40, 40, 40) : null; // null restores default
        getContentPane().setBackground(frameBg);

        // 2. Delegate the grid coloring logic to the CalendarPanel
        calendarPanel.setTheme(themeName);

        // 3. Force the Swing UI tree to update immediately
        SwingUtilities.updateComponentTreeUI(this);
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

        // Render Activities
        schedule.getActivities().forEach((time, activityName) -> {
            boolean isLocked = currentSchedule.isLockedKey(time);
            Color color = new Color(random.nextInt(156) + 100,
                    random.nextInt(156) + 100,
                    random.nextInt(156) + 100);
            calendarPanel.colorCell(time, color, activityName, isLocked);
        });

        // Render Blocked Times
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