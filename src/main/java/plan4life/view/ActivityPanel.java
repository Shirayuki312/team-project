package plan4life.view;

import plan4life.entities.Activity;
import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import plan4life.controller.CalendarController;
import plan4life.view.Event;
import plan4life.view.ReminderDialog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public class ActivityPanel extends JPanel {
    private Schedule schedule;
    private final DefaultListModel<String> activityListModel = new DefaultListModel<>();
    private final JList<String> activityList = new JList<>(activityListModel);
    private final JButton addButton = new JButton("Add Activity");
    private final JButton deleteButton = new JButton("Delete Activity"); // New button
    private CalendarController calendarController;

    public ActivityPanel(Schedule schedule) {
        this.schedule = Objects.requireNonNull(schedule, "schedule must not be null");

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Activities"));
        setPreferredSize(new Dimension(275, 0));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.add(addButton);
        topPanel.add(Box.createRigidArea(new Dimension(2, 0))); // spacing
        topPanel.add(deleteButton);

        // Scrollable list of activities
        JScrollPane listScroll = new JScrollPane(activityList);

        add(topPanel, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);

        // Add button opens the form
        addButton.addActionListener(e -> showAddActivityForm());

        // Delete button removes the selected activity
        deleteButton.addActionListener(e -> deleteSelectedActivity());

        // Initial refresh
        refreshActivityList();
    }

    /** Show a simple input form for a new activity */
    private void showAddActivityForm() {
        JTextField descriptionField = new JTextField(12);
        JTextField durationField = new JTextField(5);
        JTextField startTimeField = new JTextField(5);  // NEW

        String[] dayOptions = {
                "Any", "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday", "Sunday"
        };
        JComboBox<String> daySelector = new JComboBox<>(dayOptions);

        String[] types = {"Free Activity", "Fixed Activity"};
        JComboBox<String> typeSelector = new JComboBox<>(types);

        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        form.add(new JLabel("Type:"));
        form.add(typeSelector);
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        form.add(new JLabel("Duration (hours):"));
        form.add(durationField);
        form.add(new JLabel("Start Time (e.g., 14:00):"));
        form.add(startTimeField);
        form.add(new JLabel("Day:"));
        form.add(daySelector);

        // ----- NEW: "Set Reminder" button inside the Add Activity dialog -----
        JButton setReminderButton = new JButton("Set Reminder");
        JPanel reminderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reminderPanel.add(setReminderButton);

        form.add(new JLabel(""));
        form.add(reminderPanel);



        startTimeField.setEnabled(false);

        typeSelector.addActionListener(e -> {
            boolean isFixed = typeSelector.getSelectedItem().equals("Fixed Activity");
            startTimeField.setEnabled(isFixed);
        });

        // ----- NEW: logic for "Set Reminder" button -----
        setReminderButton.addActionListener(e -> {
            // 1) 必须有 CalendarController 才能设置 reminder
            if (calendarController == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "Reminder feature is not available (no CalendarController attached).",
                        "Reminder not available",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            // 2) 描述，允许为空时使用默认
            String desc = descriptionField.getText().trim();
            if (desc.isEmpty()) {
                desc = "New activity";
            }

            // 3) 读取 duration，必须是数字
            String durText = durationField.getText().trim();
            double durationHours;
            try {
                durationHours = Double.parseDouble(durText);
            } catch (NumberFormatException ex1) {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter a valid duration (number of hours) before setting a reminder."
                );
                return;
            }

            // 4) 必须是 Fixed Activity 且有 start time
            boolean isFixedType = "Fixed Activity".equals(typeSelector.getSelectedItem());
            String start = startTimeField.getText().trim();

            if (!isFixedType || start.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "To set a reminder here, please choose \"Fixed Activity\" and provide a start time."
                );
                return;
            }

            // 5) 解析 start time: HH:mm
            java.time.LocalTime time;
            try {
                time = java.time.LocalTime.parse(start); // e.g., "14:00"
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(
                        this,
                        "Start time format must be HH:mm, e.g., 14:00."
                );
                return;
            }

            // 6) 先简单用今天日期；如果以后要按 day 下拉框映射具体日期，再扩展这里
            java.time.LocalDate date = java.time.LocalDate.now();
            java.time.LocalDateTime startDateTime = java.time.LocalDateTime.of(date, time);
            java.time.LocalDateTime endDateTime =
                    startDateTime.plusMinutes((long) (durationHours * 60));

            // 7) 构造一个 Event，让现有的 set reminder use case 复用
            Event event = new Event(desc, startDateTime, endDateTime);
            calendarController.registerEvent(event);

            // 8) Open the existing ReminderDialog (same behavior as in CalendarFrame)
            java.awt.Window window = SwingUtilities.getWindowAncestor(this);
            // Safely cast to Frame; if not a Frame, pass null as the owner
            java.awt.Frame owner = (window instanceof java.awt.Frame)
                    ? (java.awt.Frame) window
                    : null;

            ReminderDialog reminderDialog =
                    new ReminderDialog(owner, calendarController, event);
            reminderDialog.setVisible(true);

        });



        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "Add New Activity",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String desc = descriptionField.getText().trim();
            String durText = durationField.getText().trim();
            String start = startTimeField.getText().trim();
            int dayIndex = daySelector.getSelectedIndex() - 1;  // 0 = "No Day"

            if (desc.isEmpty() || durText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter description and duration.");
                return;
            }

            try {
                float duration = Float.parseFloat(durText);
                Activity newActivity;

                boolean isFixedType = typeSelector.getSelectedItem().equals("Fixed Activity");
                boolean hasStart = !start.isEmpty();
                boolean hasDay = dayIndex >= 0;

                // --------------- FOUR CASES ----------------

                // ① Free activity (no start, no day)
                if (!isFixedType || (!hasStart && !hasDay)) {
                    newActivity = new Activity(desc, duration);
                }

                // ② Fixed start time only
                else if (hasStart && !hasDay) {
                    newActivity = new Activity(desc, duration, start);
                }

                // ③ Fixed day only
                else if (!hasStart && hasDay) {
                    newActivity = Activity.withDayOnly(desc, duration, dayIndex);
                }

                // ④ Fully fixed: day + start time
                else {
                    newActivity = new Activity(desc, duration, dayIndex, start);
                }

                // -------------------------------------------

                schedule.addTask(newActivity);
                refreshActivityList();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Duration must be a number.");
            }
        }
    }


    /** Delete the currently selected activity */
    private void deleteSelectedActivity() {
        int selectedIndex = activityList.getSelectedIndex();
        List<Activity> tasks = schedule != null ? schedule.getTasks() : Collections.emptyList();

        if (selectedIndex < 0 || selectedIndex >= tasks.size()) {
            JOptionPane.showMessageDialog(this, "Select an activity to delete.");
            activityList.clearSelection();
            return;
        }

        Activity activityToRemove = tasks.get(selectedIndex);
        schedule.removeTask(activityToRemove); // Assumes Schedule has a removeTask method
        refreshActivityList();
    }

    /** Refresh the activity list */
    public void refreshActivityList() {
        activityListModel.clear();
        activityList.clearSelection();
        if (schedule == null) return;

        List<Activity> tasks = schedule.getTasks();
        for (Activity a : tasks) {
            if (a.isFixed()) {
                activityListModel.addElement("[Fixed] " + a.getDescription()
                        + " at " + a.getStartTime()
                        + " (" + a.getDuration() + "h)");
            } else {
                activityListModel.addElement("[Free] " + a.getDescription()
                        + " (" + a.getDuration() + "h)");
            }
        }
    }

    /** Allow updating the schedule reference if it changes */
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        refreshActivityList();
    }

    public void setCalendarController(CalendarController controller) {
        this.calendarController = controller;
    }

    public List<String> getFreeActivities() {
        List<String> free = new java.util.ArrayList<>();
        for (Activity a : schedule.getTasks()) {
            if (!a.isFixed()) {
                free.add(a.getDescription() + ":" + a.getDuration());
            }
        }
        return free;
    }

    public String getFixedActivitiesText() {
        List<String> fixedActivities = new ArrayList<>();
        for (Activity activity : schedule.getTasks()) {
            if (activity.isFixed()) {
                fixedActivities.add(formatFixedActivity(activity));
            }
        }
        return String.join("\n", fixedActivities);
    }

    private String formatFixedActivity(Activity activity) {
        String dayPart = activity.getDayIndex() == null
                ? ""
                : dayName(activity.getDayIndex()) + " ";
        String startPart = activity.getStartTime() == null ? "" : activity.getStartTime() + " ";

        if (activity.getDayIndex() != null && activity.getStartTime() != null) {
            int durationMinutes = Math.round(activity.getDuration() * 60);
            return String.format("%s%s%d %s", dayPart, startPart, durationMinutes, activity.getDescription()).trim();
        }

        StringBuilder builder = new StringBuilder(activity.getDescription());
        builder.append(" (")
                .append(activity.getDuration())
                .append("h");
        if (activity.getDayIndex() != null) {
            builder.append(", ").append(dayName(activity.getDayIndex()));
        }
        if (activity.getStartTime() != null) {
            builder.append(" @ ").append(activity.getStartTime());
        }
        builder.append(')');
        return builder.toString();
    }

    private String dayName(int dayIndex) {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        if (dayIndex < 0 || dayIndex >= days.length) {
            return "";
        }
        return days[dayIndex];
    }

    /**
     * Display the activities that were placed on the calendar so testers can scan them quickly.
     */
    public void setActivities(Iterable<String> activities) {
        activityListModel.clear();
        activityList.clearSelection();
        if (activities == null) {
            return;
        }
        for (String activity : activities) {
            activityListModel.addElement(activity);
        }
    }
}