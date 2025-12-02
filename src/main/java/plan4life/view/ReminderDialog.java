package plan4life.view;

import plan4life.controller.CalendarController;
import plan4life.view.Event;

import javax.swing.*;
import java.awt.*;

public class ReminderDialog extends JDialog{
    private final JSpinner minutesSpinner;
    private final JComboBox<String> alertTypeBox;

    public ReminderDialog(Frame owner,
                          CalendarController controller,
                          Event event) {
        super(owner, "Set Important Reminder", true); // modal dialog

        JLabel titleLabel = new JLabel(
                "This event is marked as Important. Set a reminder time:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 13f));

        alertTypeBox = new JComboBox<>(new String[]{
                "Message",
                "Message with sound"
        });

        minutesSpinner = new JSpinner(
                new SpinnerNumberModel(15, 0, 1440, 5)); // default 15 mins

        JLabel minutesLabel = new JLabel("minutes before");

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            int minutesBefore = (Integer) minutesSpinner.getValue();
            String alertType = (String) alertTypeBox.getSelectedItem();

            controller.setImportantReminder(event, minutesBefore, alertType);

            dispose();
        });

        cancelButton.addActionListener(e -> { dispose();
        });

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 10, 8, 10);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 3;
        gc.anchor = GridBagConstraints.WEST;
        content.add(titleLabel, gc);

        gc.gridy++;
        gc.gridwidth = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        content.add(new JLabel("Alert:"), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        content.add(alertTypeBox, gc);

        gc.gridx = 0;
        gc.gridy++;
        gc.weightx = 0;
        content.add(new JLabel("Remind me:"), gc);

        gc.gridx = 1;
        content.add(minutesSpinner, gc);

        gc.gridx = 2;
        content.add(minutesLabel, gc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(cancelButton);
        btnPanel.add(okButton);

        gc.gridx = 0;
        gc.gridy++;
        gc.gridwidth = 3;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.EAST;
        gc.fill = GridBagConstraints.NONE;
        content.add(btnPanel, gc);

        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }
}
