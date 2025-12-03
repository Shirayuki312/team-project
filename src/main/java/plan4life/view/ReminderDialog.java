package plan4life.view;

import plan4life.entities.Event;
import plan4life.controller.CalendarController;
import plan4life.entities.Event.UrgencyLevel;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

/**
 * Dialog for Use Case 7: Set Important Reminder.
 *
 * Allows the user to choose:
 *  - alert type (message only, message with sound, etc.)
 *  - minutes before the event
 *  - urgency level (with color)
 *  - whether to send message / email / play sound
 *  - apply to this event only or to all events
 */
public class ReminderDialog extends JDialog {

    private final CalendarController controller;
    private final Event event;
    private final String timeKey;

    private JComboBox<String> alertTypeBox;
    private JSpinner minutesSpinner;
    private JComboBox<UrgencyLevel> urgencyBox;

    private JCheckBox sendMessageCheck;
    private JCheckBox sendEmailCheck;
    private JCheckBox soundCheck;

    private JRadioButton applyThisEventRadio;
    private JRadioButton applyAllEventsRadio;

    public ReminderDialog(Frame owner,
                          CalendarController controller,
                          Event event, String timeKey) {
        super(owner, "Set Important Reminder", true);
        this.controller = controller;
        this.event = event;
        this.timeKey = timeKey;

        initUI();
        urgencyBox.setUI(new BasicComboBoxUI());
        configureUrgencyRenderer();

        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        // Title
        JLabel titleLabel = new JLabel(
                "Set reminder time and options for important events:");
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Alert type
        gbc.gridy++;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Alert:"), gbc);

        gbc.gridx = 1;
        alertTypeBox = new JComboBox<>(new String[]{
                "Message only",
                "Message with sound",
                "Sound only"
        });
        alertTypeBox.setSelectedIndex(1); // default: Message with sound
        mainPanel.add(alertTypeBox, gbc);

        // Minutes before
        gbc.gridx = 0;
        gbc.gridy++;
        mainPanel.add(new JLabel("Remind me:"), gbc);

        gbc.gridx = 1;
        minutesSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 1440, 5));
        JPanel minutesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        minutesPanel.add(minutesSpinner);
        minutesPanel.add(new JLabel("  minutes before"));
        mainPanel.add(minutesPanel, gbc);

        // Urgency
        gbc.gridx = 0;
        gbc.gridy++;
        mainPanel.add(new JLabel("Urgency:"), gbc);

        gbc.gridx = 1;
        urgencyBox = new JComboBox<>(UrgencyLevel.values());
        urgencyBox.setSelectedItem(UrgencyLevel.MEDIUM);
        mainPanel.add(urgencyBox, gbc);

        // Notification channels
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel channelsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sendMessageCheck = new JCheckBox("Send message", true);
        sendEmailCheck = new JCheckBox("Send email", true);
        soundCheck = new JCheckBox("Play sound", true);
        channelsPanel.add(sendMessageCheck);
        channelsPanel.add(sendEmailCheck);
        channelsPanel.add(soundCheck);
        mainPanel.add(channelsPanel, gbc);

        // Apply scope (this event / all events)
        gbc.gridy++;
        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        applyThisEventRadio = new JRadioButton("Apply to this event only", true);
        applyAllEventsRadio = new JRadioButton("Apply to all events");
        ButtonGroup scopeGroup = new ButtonGroup();
        scopeGroup.add(applyThisEventRadio);
        scopeGroup.add(applyAllEventsRadio);
        scopePanel.add(applyThisEventRadio);
        scopePanel.add(applyAllEventsRadio);
        mainPanel.add(scopePanel, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Buttons
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
    }

    /**
     * Install a colorful renderer for urgency levels.
     * This is called after urgencyBox is created.
     */
    private void configureUrgencyRenderer() {
        if (urgencyBox == null) {
            return;
        }

        urgencyBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel();
            label.setOpaque(true);

            if (value instanceof UrgencyLevel) {
                UrgencyLevel level = (UrgencyLevel) value;
                label.setText(level.name());

                Color levelColor;
                switch (level) {
                    case LOW:
                        levelColor = new Color(8, 239, 204);   // cyan-ish
                        break;
                    case MEDIUM:
                        levelColor = new Color(65, 243, 6);    // green
                        break;
                    case HIGH:
                        levelColor = new Color(200, 120, 0);   // orange
                        break;
                    default:
                        levelColor = list.getForeground();
                }

                if (isSelected) {
                    // For selected item (both dropdown and closed state)
                    label.setBackground(levelColor);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(list.getBackground());
                    label.setForeground(levelColor);
                }
            } else if (value != null) {
                label.setText(value.toString());
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
            }

            return label;
        });
    }

    private void onOk() {
        int minutesBefore = (Integer) minutesSpinner.getValue();
        String alertType = (String) alertTypeBox.getSelectedItem();
        UrgencyLevel urgencyLevel = (UrgencyLevel) urgencyBox.getSelectedItem();

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

            // Show a single success message regardless of how many events
            // are updated underneath.
            JOptionPane.showMessageDialog(
                    this,
                    "Set Reminder successfully.",
                    "Message",
                    JOptionPane.INFORMATION_MESSAGE
            );
            controller.setImportantReminderForAllEvents(
                    event,          // NEW: sourceEvent
                    minutesBefore,
                    alertType,
                    urgencyLevel,
                    sendMsg,
                    sendEmail,
                    playSound
            );
        } else {

            // Show a single success message regardless of how many events
            // are updated underneath.
            JOptionPane.showMessageDialog(
                    this,
                    "Set Reminder successfully.",
                    "Message",
                    JOptionPane.INFORMATION_MESSAGE
            );
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
            // NEW: highlight the event cell by urgency
            if (getOwner() instanceof CalendarFrame && timeKey != null) {
                CalendarFrame frame = (CalendarFrame) getOwner();
                frame.highlightReminderCell(timeKey, urgencyLevel);
            }

            dispose();
        }
    }

        private void onRemove () {
            if (event != null) {
                controller.cancelImportantReminder(event);

                JOptionPane.showMessageDialog(
                        this,
                        "Reminder cancelled.",
                        "Message",
                        JOptionPane.INFORMATION_MESSAGE);

                if (getOwner() instanceof CalendarFrame && timeKey != null) {
                    CalendarFrame frame = (CalendarFrame) getOwner();
                    frame.resetReminderCell(timeKey);
                }
                dispose();
            }
        }
    }
