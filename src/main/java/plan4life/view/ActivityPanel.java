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

        JScrollPane listScroll = new JScrollPane(activityList);

        add(inputPanel, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);
    }
}