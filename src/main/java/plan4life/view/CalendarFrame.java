package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import plan4life.controller.CalendarController;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.use_case.block_off_time.BlockOffTimeController;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Main application frame that shows the calendar and activities.
 * It also reacts to time selections (for blocking time) and
 * integrates with the reminder feature (Use Case 7).
 */
public class CalendarFrame extends JFrame implements CalendarViewInterface, TimeSelectionListener {
    private final CalendarPanel calendarPanel;
    private final ActivityPanel activityPanel;
    private BlockOffTimeController blockOffTimeController;
    private Schedule currentSchedule;
    private CalendarController calendarController; // controller used for reminders & locking

    public CalendarFrame() {
        super("Plan4Life - Scheduler");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLayout(new BorderLayout(10, 10));

        JPanel topBar = new JPanel(new BorderLayout());

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton dayBtn = new JButton("Day");
        JButton weekBtn = new JButton("Week");
        leftPanel.add(dayBtn);
        leftPanel.add(weekBtn);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton generateBtn = new JButton("Generate Schedule");
        rightPanel.add(generateBtn);

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
    }

    public CalendarFrame(BlockOffTimeController blockOffTimeController) {
        this();
        this.blockOffTimeController = blockOffTimeController;
    }

    public void setBlockOffTimeController(BlockOffTimeController controller) {
        this.blockOffTimeController = controller;
    }

    /**
     * Injects the CalendarController so this frame can:
     *  - lock & regenerate schedules
     *  - register events and open the reminder dialog
     */
    public void setCalendarController(CalendarController controller) {
        this.calendarController = controller;
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
                calendarPanel.colorBlockedRange(
                        block.getStart(),
                        block.getEnd(),
                        block.getColumnIndex()
                );
            }
        }

        calendarPanel.repaint();
    }

    /**
     * Called when the user selects a time range on the calendar.
     * We use this both to:
     *  - block off time (original behavior), and
     *  - create an Event and open the reminder dialog (Use Case 7).
     */
    @Override
    public void onTimeSelected(LocalDateTime start, LocalDateTime end,
                               int scheduleId, int columnIndex) {
        String description = JOptionPane.showInputDialog(this,
                "Optional description for this blocked time:");

        if (description == null) {

            return;
        }

        if (calendarController != null) {
            String title = description.isBlank()
                    ? "Activity"
                    : description;

            Event event = new Event(title, start, end);

            calendarController.registerEvent(event);

            ReminderDialog dialog =
                    new ReminderDialog(this, calendarController, event, true);
            dialog.setVisible(true);
        }

        if (blockOffTimeController != null) {
            blockOffTimeController.blockTime(
                    scheduleId, start, end, description, columnIndex);
        }
    }
}
