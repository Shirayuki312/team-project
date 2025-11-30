package plan4life.view;

import javax.swing.*;
import java.awt.*;

public class ActivityPanel extends JPanel {
    private final DefaultListModel<String> activityListModel = new DefaultListModel<>();
    private final JList<String> activityList = new JList<>(activityListModel);
    private final JTextField inputField = new JTextField(12);
    private final JButton addButton = new JButton("Add");

    public ActivityPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Activities"));
        setPreferredSize(new Dimension(250, 0));

        JPanel inputPanel = new JPanel();
        inputPanel.add(inputField);
        inputPanel.add(addButton);

        addButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                activityListModel.addElement(text);
                inputField.setText("");
            }
        });

        JScrollPane listScroll = new JScrollPane(activityList);

        add(inputPanel, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);
    }
    public java.util.List<String> getFreeActivities() {
        return java.util.Collections.list(activityListModel.elements());
    }

}