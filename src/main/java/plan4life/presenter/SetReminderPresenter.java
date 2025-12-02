package plan4life.presenter;

import plan4life.use_case.set_reminder.SetReminderOutputBoundary;
import plan4life.use_case.set_reminder.SetReminderResponseModel;

import javax.swing.*;
import java.awt.*;
import javax.swing.SwingUtilities;

/**
 * Presenter for the SetReminder use case.
 * Uses Swing dialogs and system beep to notify the user.
 */
public class SetReminderPresenter implements SetReminderOutputBoundary {

    @Override
    public void presentReminderScheduled(SetReminderResponseModel responseModel) {
     JOptionPane.showMessageDialog(null, "Reminder set successfully.");
    }

    @Override
    public void presentReminderFired(SetReminderResponseModel responseModel) {
        SwingUtilities.invokeLater(() -> {
            // Sound alert if requested
            if (responseModel.isPlaySound()) {
                for (int i = 0; i < 2; i++) {
                    Toolkit.getDefaultToolkit().beep();
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException ignored) {}
                }
            }

            // Base reminder popup
            StringBuilder msg = new StringBuilder();
            msg.append("Reminder: ").append(responseModel.getTitle());
            if (responseModel.getUrgencyLevel() != null) {
                msg.append(" (").append(responseModel.getUrgencyLevel()).append(")");
            }

            JOptionPane.showMessageDialog(
                    null,
                    msg.toString(),
                    "Important Reminder",
                    JOptionPane.INFORMATION_MESSAGE
            );

            // Simulate "message" notification
            if (responseModel.isSendMessage()) {
                JOptionPane.showMessageDialog(
                        null,
                        "A message notification for this event has been sent to your messaging inbox.",
                        "Message Sent",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            // Simulate "email" notification
            if (responseModel.isSendEmail()) {
                JOptionPane.showMessageDialog(
                        null,
                        "An email reminder for this event has been generated.",
                        "Email Sent",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });
    }

    @Override
    public void presentReminderCancelled(SetReminderResponseModel responseModel) {
        JOptionPane.showMessageDialog(null, "Reminder cancelled.");
    }
}

