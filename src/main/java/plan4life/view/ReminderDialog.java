package plan4life.view;

import plan4life.controller.CalendarController;

import javax.swing.*;
import java.awt.*;

public class ReminderDialog extends JDialog {

    private final JSpinner minutesSpinner;
    private final JComboBox<String> alertTypeBox;
    private final JComboBox<Event.UrgencyLevel> urgencyBox;
    private final JPanel colorPreview;

    private final JCheckBox sendMessageCheck;
    private final JCheckBox sendEmailCheck;
    private final JCheckBox soundCheck;

    private final JRadioButton applyThisEventRadio;
    private final JRadioButton applyAllEventsRadio;

    private final CalendarController controller;
    private final Event event;

    /**
     * @param owner
     * @param controller    calendar controller
     * @param event         event needed to be set reminder；if from Setting to set reminder，can be null
     * @param allowApplyAll
     */
    public ReminderDialog(Frame owner,
                          CalendarController controller,
                          Event event,
                          boolean allowApplyAll) {
        super(owner, "Set Important Reminder", true); // modal dialog
        this.controller = controller;
        this.event = event;

        JLabel titleLabel = new JLabel(
                "Set reminder time and options for important events:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.PLAIN, 13f));

        alertTypeBox = new JComboBox<>(new String[]{
                "Message",
                "Message with sound"
        });

        minutesSpinner = new JSpinner(
                new SpinnerNumberModel(15, 0, 1440, 5));
        JLabel minutesLabel = new JLabel("minutes before");

        urgencyBox = new JComboBox<>(Event.UrgencyLevel.values());
        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(30, 18));
        updateColorPreview((Event.UrgencyLevel) urgencyBox.getSelectedItem());

        urgencyBox.addActionListener(e ->
                updateColorPreview((Event.UrgencyLevel) urgencyBox.getSelectedItem()));

        sendMessageCheck = new JCheckBox("Send message");
        sendEmailCheck = new JCheckBox("Send email");
        soundCheck = new JCheckBox("Play sound");
        soundCheck.setSelected(true);

        applyThisEventRadio = new JRadioButton("Apply to this event only");
        applyAllEventsRadio = new JRadioButton("Apply to all events");

        ButtonGroup scopeGroup = new ButtonGroup();
        scopeGroup.add(applyThisEventRadio);
        scopeGroup.add(applyAllEventsRadio);

        if (event != null) {
            applyThisEventRadio.setSelected(true);
        } else {
            // If no event，use settings open reminder，default apply for all events”
            applyAllEventsRadio.setSelected(true);
            applyThisEventRadio.setEnabled(false);
        }

        applyAllEventsRadio.setEnabled(allowApplyAll);

        // ---- Buttons ----
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeBtn = new JButton("Remove Reminder");
        JButton cancelBtn = new JButton("Cancel");
        JButton okBtn = new JButton("OK");

        buttonPanel.add(removeBtn);
        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        removeBtn.addActionListener(e -> onRemove());
        cancelBtn.addActionListener(e -> dispose());
        okBtn.addActionListener(e -> onOk());


        // ---- Layout ----
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 10, 8, 10);

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 4;
        gc.anchor = GridBagConstraints.WEST;
        content.add(titleLabel, gc);

        gc.gridy++;
        gc.gridwidth = 1;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        content.add(new JLabel("Alert:"), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.gridwidth = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        content.add(alertTypeBox, gc);

        gc.gridx = 0;
        gc.gridy++;
        gc.gridwidth = 1;
        gc.weightx = 0;
        content.add(new JLabel("Remind me:"), gc);

        gc.gridx = 1;
        content.add(minutesSpinner, gc);

        gc.gridx = 2;
        content.add(minutesLabel, gc);

        gc.gridx = 0;
        gc.gridy++;
        content.add(new JLabel("Urgency:"), gc);

        gc.gridx = 1;
        gc.gridwidth = 2;
        content.add(urgencyBox, gc);

        gc.gridx = 3;
        gc.gridwidth = 1;
        content.add(colorPreview, gc);

        gc.gridx = 0;
        gc.gridy++;
        gc.gridwidth = 4;
        gc.anchor = GridBagConstraints.WEST;
        JPanel channelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        channelPanel.add(sendMessageCheck);
        channelPanel.add(sendEmailCheck);
        channelPanel.add(soundCheck);
        content.add(channelPanel, gc);

        gc.gridy++;
        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        scopePanel.add(applyThisEventRadio);
        scopePanel.add(applyAllEventsRadio);
        content.add(scopePanel, gc);

        gc.gridy++;
        gc.gridwidth = 4;
        gc.anchor = GridBagConstraints.EAST;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(cancelBtn);
        btnPanel.add(okBtn);
        content.add(btnPanel, gc);

        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    private void updateColorPreview(Event.UrgencyLevel level) {
        if (level == null) return;
        Event temp = new Event("temp", java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        temp.setUrgencyLevel(level);
        colorPreview.setBackground(temp.getColor());
    }

    private void onOk() {
        int minutesBefore = (Integer) minutesSpinner.getValue();
        String alertType = (String) alertTypeBox.getSelectedItem();
        Event.UrgencyLevel urgencyLevel =
                (Event.UrgencyLevel) urgencyBox.getSelectedItem();

        boolean sendMsg = sendMessageCheck.isSelected();
        boolean sendEmail = sendEmailCheck.isSelected();
        boolean playSound = soundCheck.isSelected();

        if (applyThisEventRadio.isSelected() && event == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "There is no event selected. Please create or select an event first.",
                    "No event selected",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (applyAllEventsRadio.isSelected()) {
            controller.setImportantReminderForAllEvents(
                    minutesBefore,
                    alertType,
                    urgencyLevel,
                    sendMsg,
                    sendEmail,
                    playSound
            );
        } else {
            controller.setImportantReminderForEvent(
                    event,
                    minutesBefore,
                    alertType,
                    urgencyLevel,
                    sendMsg,
                    sendEmail,
                    playSound
            );

            controller.registerEvent(event);
        }

        dispose();
    }
    private void onRemove() {
        if (event != null) {
            controller.cancelImportantReminder(event);
        }
        dispose();
    }

}

