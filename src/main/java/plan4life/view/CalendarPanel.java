package plan4life.view;

import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Locale;

public class CalendarPanel extends JPanel {
    private JPanel[][] cells;
    private JPanel gridPanel;
    private JPanel dayHeaderPanel;
    private JPanel timeLabelPanel;
    private int currentColumns = 7;

    private int startRow = -1;
    private int endRow = -1;
    private int column = -1;
    private boolean dragging = false;

    private TimeSelectionListener listener;

    public CalendarPanel() {
        setBorder(BorderFactory.createTitledBorder("Weekly Calendar"));
        buildGrid(7);
    }

    private void buildGrid(int columns) {
        removeAll();
        currentColumns = columns;
        int rows = 24;

        // Wrap grid with headers so users can see day labels across the top and hour labels on the left.
        setLayout(new BorderLayout(4, 4));

        dayHeaderPanel = new JPanel(new GridLayout(1, columns + 1));
        dayHeaderPanel.add(new JLabel("")); // empty corner so headers align with hour labels
        for (int c = 0; c < columns; c++) {
            JLabel dayLabel = new JLabel(getDayLabel(c), SwingConstants.CENTER);
            dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
            dayHeaderPanel.add(dayLabel);
        }

        timeLabelPanel = new JPanel(new GridLayout(rows, 1));
        for (int hour = 0; hour < rows; hour++) {
            JLabel hourLabel = new JLabel(String.format("%02d:00", hour), SwingConstants.RIGHT);
            hourLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            timeLabelPanel.add(hourLabel);
        }

        gridPanel = new JPanel(new GridLayout(rows, columns, 2, 2));
        cells = new JPanel[rows][columns];

        for (int hour = 0; hour < rows; hour++) {
            for (int day = 0; day < columns; day++) {
                JPanel cell = new JPanel();
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                cells[hour][day] = cell;
                gridPanel.add(cell);
            }
        }

        gridPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                column = getColumnFromX(e.getX());
                startRow = getRowFromY(e.getY());
                endRow = startRow;
                updateSelection();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragging) return;
                dragging = false;

                if (column != -1 && startRow != -1 && endRow != -1) {
                    int min = Math.min(startRow, endRow);
                    int max = Math.max(startRow, endRow);
                    LocalDateTime now = LocalDateTime.now();

                    if (listener == null) {
                        return;
                    }

                    // TODO: Using today's date as a placeholder. Replace with actual selected calendar date once implemented.
                    LocalDateTime start = now
                            .withHour(min)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    LocalDateTime end = now
                            .withHour(max + 1)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);

                    int scheduleId = (currentColumns == 1) ? 1 : 2;
                    listener.onTimeSelected(start, end, scheduleId, column);
                }
            }
        });

        gridPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int newRow = getRowFromY(e.getY());
                    if (newRow != endRow) {
                        endRow = newRow;
                        updateSelection();
                    }
                }
            }
        });

        add(dayHeaderPanel, BorderLayout.NORTH);
        add(timeLabelPanel, BorderLayout.WEST);
        add(gridPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public void setDayView() {
        setBorder(BorderFactory.createTitledBorder("Daily Calendar"));
        buildGrid(1);
    }

    public void setWeekView() {
        setBorder(BorderFactory.createTitledBorder("Weekly Calendar"));
        buildGrid(7);
    }

    public void setTimeSelectionListener(TimeSelectionListener listener) {
        this.listener = listener;
    }

    private int getColumnFromX(int x) {
        int cellWidth = gridPanel.getWidth() / currentColumns;
        return Math.max(0, Math.min(currentColumns - 1, x / cellWidth));
    }

    private int getRowFromY(int y) {
        for (int r = 0; r < 24; r++) {
            Rectangle bounds = cells[r][0].getBounds();
            if (y >= bounds.y && y < bounds.y + bounds.height) {
                return r;
            }
        }

        Rectangle last = cells[23][0].getBounds();
        if (y > last.y + last.height + 3) {
            return 23;
        }

        return Math.max(0, Math.min(23, 23 * y / getHeight()));  // Approximate row if cursor is between cells
    }

    private void updateSelection() {
        for (int r = 0; r < 24; r++) {
            for (int c = 0; c < currentColumns; c++) {
                cells[r][c].setBackground(Color.WHITE);
            }
        }

        if (column != -1 && startRow != -1) {
            int min = Math.min(startRow, endRow);
            int max = Math.max(startRow, endRow);
            for (int r = min; r <= max; r++) {
                cells[r][column].setBackground(new Color(173, 216, 230)); // light blue
            }
        }
    }

    public void updateSchedule(Schedule schedule) {
    }

    public void clear() {
        for (int r = 0; r < 24; r++) {
            for (int c = 0; c < currentColumns; c++) {
                cells[r][c].setBackground(Color.WHITE);
                cells[r][c].removeAll();
            }
        }
    }

    public void colorCell(String time, Color color, String label, boolean locked) {
        try {
            ParsedTimeKey parsed = parseTimeKey(time);
            if (parsed == null) {
                System.out.printf("[CalendarPanel] Unable to parse time key: %s%n", time);
                return;
            }

            int columnIndex = currentColumns == 1 ? 0 : parsed.columnIndex;
            if (columnIndex < 0 || columnIndex >= currentColumns) {
                System.out.printf("[CalendarPanel] Column index out of bounds for %s -> %d%n", time, columnIndex);
                return;
            }

            JPanel cell = cells[parsed.hour][columnIndex];
            cell.removeAll();
            cell.setBackground(color);
            cell.setLayout(new BorderLayout());

            String displayLabel = label != null && label.length() > 18
                    ? label.substring(0, 17) + "…"
                    : label;
            JLabel title = new JLabel(displayLabel, SwingConstants.CENTER);
            title.setOpaque(false);
            title.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            if (label != null) {
                title.setToolTipText(label);
            }
            cell.add(title, BorderLayout.CENTER);

            String lockText = locked ? "\uD83D\uDD12" : "\uD83D\uDD13";
            JLabel lockLabel = new JLabel(lockText);
            lockLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lockLabel.setBorder(BorderFactory.createEmptyBorder(2,6,2,6));
            lockLabel.setToolTipText(locked ? "Fixed: cannot be moved" : "Click to lock this slot");
            lockLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (lockListener != null) lockListener.onLockToggle(time);
                }
            });

            cell.add(lockLabel, BorderLayout.EAST);
            cell.revalidate();
            cell.repaint();

            System.out.printf("[CalendarPanel] colorCell %s -> row %d, col %d, locked=%s%n", time, parsed.hour, columnIndex, locked);
        } catch (Exception ex) {
            System.out.printf("[CalendarPanel] Failed to color cell for %s due to %s%n", time, ex.getMessage());
        }
    }

    private ParsedTimeKey parseTimeKey(String time) {
        if (time == null || !time.contains(" ")) {
            return null;
        }
        String[] parts = time.split(" ");
        if (parts.length < 2) {
            return null;
        }
        String dayToken = parts[0].trim().toUpperCase(Locale.ROOT);
        String timeToken = parts[1].trim();

        DayOfWeek day = switch (dayToken) {
            case "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> null;
        };
        if (day == null) {
            return null;
        }

        String[] timeParts = timeToken.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int columnIndex = day.getValue() - 1;
        return new ParsedTimeKey(hour, columnIndex);
    }

    private static class ParsedTimeKey {
        final int hour;
        final int columnIndex;

        ParsedTimeKey(int hour, int columnIndex) {
            this.hour = hour;
            this.columnIndex = columnIndex;
        }
    }

    private String getDayLabel(int columnIndex) {
        if (currentColumns == 1) {
            return "Day";
        }

        DayOfWeek day = DayOfWeek.of((columnIndex % 7) + 1);
        return day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault());
    }


    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int columnIndex) {
        int startHour = start.getHour();
        int endHour = end.getHour();

        int min = Math.max(0, startHour);
        int max = Math.min(23, endHour - 1);

        if (columnIndex < 0 || columnIndex >= currentColumns) {
            return;
        }

        for (int r = min; r <= max; r++) {
            cells[r][columnIndex].setBackground(Color.GRAY);
            cells[r][columnIndex].removeAll();
            cells[r][columnIndex].add(new JLabel("Blocked"));
        }
    }
    // new listener interface inside or external:
    public interface LockListener {
        void onLockToggle(String timeKey);
    }

    // field
    private LockListener lockListener;

    public void setLockListener(LockListener listener) {
        this.lockListener = listener;
    }

}