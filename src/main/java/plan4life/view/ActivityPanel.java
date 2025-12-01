package plan4life.view;

import plan4life.entities.Activity;
import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class ActivityPanel extends JPanel {
    private Schedule schedule;
    private final DefaultListModel<String> activityListModel = new DefaultListModel<>();
    private final JList<String> activityList = new JList<>(activityListModel);
    private final JButton addButton = new JButton("Add Activity");
    private final JButton deleteButton = new JButton("Delete Activity"); // New button

    public ActivityPanel(Schedule schedule) {
        this.schedule = schedule;

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


        startTimeField.setEnabled(false);

        typeSelector.addActionListener(e -> {
            boolean isFixed = typeSelector.getSelectedItem().equals("Fixed Activity");
            startTimeField.setEnabled(isFixed);
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
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(this, "Please select an activity to delete.");
            return;
        }

        Activity activityToRemove = schedule.getTasks().get(selectedIndex);
        schedule.removeTask(activityToRemove); // Assumes Schedule has a removeTask method
        refreshActivityList();
    }

    /** Refresh the activity list */
    public void refreshActivityList() {
        activityListModel.clear();
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
    public List<String> getFreeActivities() {
        List<String> free = new java.util.ArrayList<>();
        for (Activity a : schedule.getTasks()) {
            if (!a.isFixed()) {
                free.add(a.getDescription() + ":" + a.getDuration());
            }
        }
        return free;
    }
}