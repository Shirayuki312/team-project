package plan4life.view;

import plan4life.entities.Activity;
import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ActivityPanel extends JPanel {
    private Schedule schedule;
    private final DefaultListModel<String> activityListModel = new DefaultListModel<>();
    private final JList<String> activityList = new JList<>(activityListModel);
    private final JButton addButton = new JButton("Add Activity");

    public ActivityPanel(Schedule schedule) {
        this.schedule = schedule;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Activities"));
        setPreferredSize(new Dimension(250, 0));

        // Top panel with Add button
        JPanel topPanel = new JPanel();
        topPanel.add(addButton);

        // Scrollable list of activities
        JScrollPane listScroll = new JScrollPane(activityList);

        add(topPanel, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);

        // Add button opens the form
        addButton.addActionListener(e -> showAddActivityForm());

        // Initial refresh
        refreshActivityList();
    }

    /** Show a simple input form for a new activity */
    private void showAddActivityForm() {
        JTextField descriptionField = new JTextField(12);
        JTextField durationField = new JTextField(5);

        JPanel form = new JPanel(new GridLayout(2, 2, 5, 5));
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        form.add(new JLabel("Duration (hours):"));
        form.add(durationField);

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

            if (desc.isEmpty() || durText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both description and duration.");
                return;
            }

            try {
                float duration = Float.parseFloat(durText);
                Activity newActivity = new Activity(desc, duration);

                // Add to schedule
                schedule.addTask(newActivity);

                // Refresh UI
                refreshActivityList();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Duration must be a number.");
            }
        }
    }

    /** Refresh the activity list */
    public void refreshActivityList() {
        activityListModel.clear();
        if (schedule == null) return;

        List<Activity> tasks = schedule.getTasks();
        for (Activity a : tasks) {
            activityListModel.addElement(a.getDescription() + " (" + a.getDuration() + "h)");
        }
    }

    /** Allow updating the schedule reference if it changes */
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        refreshActivityList();
    }
}