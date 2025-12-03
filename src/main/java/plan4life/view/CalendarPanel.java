package plan4life.view;

import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarPanel extends JPanel {
    private static final int START_HOUR = 6;
    private static final int ROWS = 24 - START_HOUR; // show from 6:00 to 23:00 inclusive

    private JPanel[][] cells;
    private JPanel gridPanel;
    private JPanel dayHeaderPanel;
    private JPanel timeLabelPanel;
    private int currentColumns = 7;

    private int startRow = -1;
    private int endRow = -1;
    private int column = -1;
    private boolean dragging = false;

    private final List<BlockedTime> blockedTimes = new ArrayList<>();
    private final List<ManualBlock> manualBlocks = new ArrayList<>();

    private TimeSelectionListener listener;
    private LockListener lockListener;

    private String currentThemeName = "Light Mode";

    public CalendarPanel() {
        buildGrid(7);
    }

    public void updateTitle(String title) {
        this.setBorder(BorderFactory.createTitledBorder(title));
        this.repaint();
    }

    private void buildGrid(int columns) {
        removeAll();
        currentColumns = columns;

        setLayout(new BorderLayout(4, 4));

        dayHeaderPanel = new JPanel(new GridLayout(1, columns + 1));
        dayHeaderPanel.add(new JLabel(""));
        for (int c = 0; c < columns; c++) {
            JLabel dayLabel = new JLabel(getDayLabel(c), SwingConstants.CENTER);
            dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
            dayHeaderPanel.add(dayLabel);
        }

        timeLabelPanel = new JPanel(new GridLayout(ROWS, 1));
        for (int hour = 0; hour < ROWS; hour++) {
            JLabel hourLabel = new JLabel(String.format("%02d:00", START_HOUR + hour), SwingConstants.RIGHT);
            hourLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            timeLabelPanel.add(hourLabel);
        }

        gridPanel = new JPanel(new GridLayout(ROWS, columns, 2, 2));
        cells = new JPanel[ROWS][columns];

        boolean isDark = "Dark Mode".equals(currentThemeName);
        Color bgColor = isDark ? Color.DARK_GRAY : Color.WHITE;
        Color gridColor = isDark ? Color.GRAY : Color.LIGHT_GRAY;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < columns; c++) {
                JPanel cell = new JPanel();
                cell.setBackground(bgColor);
                cell.setBorder(BorderFactory.createLineBorder(gridColor));
                cells[r][c] = cell;
                gridPanel.add(cell);
            }
        }

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e);
                    return;
                }

                dragging = true;
                Point p = e.getPoint();
                column = getColumnFromX(p.x);
                startRow = getRowFromY(p.y);
                endRow = startRow;
                updateSelectionVisual();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) {
                    return;
                }
                Point p = e.getPoint();
                int newRow = getRowFromY(p.y);
                int newCol = getColumnFromX(p.x);
                if (newRow != -1) {
                    endRow = newRow;
                }
                if (newCol != -1) {
                    column = newCol;
                }
                updateSelectionVisual();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragging) {
                    return;
                }
                dragging = false;

                if (listener != null && column != -1 && startRow != -1 && endRow != -1) {
                    int minRow = Math.min(startRow, endRow);
                    int maxRow = Math.max(startRow, endRow);
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime start = now
                            .withHour(toHour(minRow))
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    LocalDateTime end = now
                            .withHour(toHour(maxRow + 1))
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    int scheduleId = (currentColumns == 1) ? 1 : 2;
                    listener.onTimeSelected(start, end, scheduleId, column);
                }

                clearDragSelection();
            }
        };

        gridPanel.addMouseListener(mouseHandler);
        gridPanel.addMouseMotionListener(mouseHandler);

        add(dayHeaderPanel, BorderLayout.NORTH);
        add(timeLabelPanel, BorderLayout.WEST);
        add(gridPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public void setTheme(String themeName) {
        this.currentThemeName = themeName;
        boolean isDark = "Dark Mode".equals(themeName);

        Color bgColor = isDark ? Color.DARK_GRAY : Color.WHITE;
        Color gridColor = isDark ? Color.GRAY : Color.LIGHT_GRAY;

        setBackground(bgColor);

        if (cells != null) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < currentColumns; c++) {
                    JPanel cell = cells[r][c];
                    if (!isBlockedCell(r, c)) {
                        cell.setBackground(bgColor);
                        cell.setBorder(BorderFactory.createLineBorder(gridColor));
                        cell.removeAll();
                    }
                }
            }
        }
        repaintAllBlocks();
        repaint();
    }

    private void handleRightClick(MouseEvent e) {
        if (lockListener == null) {
            return;
        }

        Point p = e.getPoint();
        int c = getColumnFromX(p.x);
        int r = getRowFromY(p.y);
        if (r >= 0 && c >= 0) {
            String key = toTimeKey(c, toHour(r));
            lockListener.onLockToggle(key);
        }
    }

    private boolean isBlockedCell(int r, int c) {
        for (BlockedTime bt : blockedTimes) {
            if (bt.getColumnIndex() == c && r >= bt.getStart().getHour() && r <= bt.getEnd().getHour()) {
                return true;
            }
        }
        for (ManualBlock mb : manualBlocks) {
            if (mb.col == c && r >= mb.start.getHour() && r <= mb.end.getHour()) {
                return true;
            }
        }
        return false;
    }

    public void setTimeSelectionListener(TimeSelectionListener listener) {
        this.listener = listener;
    }

    public interface LockListener {
        void onLockToggle(String timeKey);
    }

    public void setLockListener(LockListener listener) {
        this.lockListener = listener;
    }

    public void setDayView() {
        buildGrid(1);
        setTheme(currentThemeName);
    }

    public void setWeekView() {
        buildGrid(7);
        setTheme(currentThemeName);
    }

    private int getColumnFromX(int x) {
        if (currentColumns <= 0 || gridPanel.getWidth() == 0) {
            return -1;
        }
        int cellWidth = gridPanel.getWidth() / currentColumns;
        int col = x / Math.max(1, cellWidth);
        return Math.max(0, Math.min(currentColumns - 1, col));
    }

    private int getRowFromY(int y) {
        if (cells == null || cells.length == 0 || gridPanel.getHeight() == 0) {
            return -1;
        }
        int cellHeight = gridPanel.getHeight() / ROWS;
        int row = y / Math.max(1, cellHeight);
        return Math.max(0, Math.min(ROWS - 1, row));
    }

    private void updateSelectionVisual() {
        repaintAllBlocks();

        if (column == -1 || startRow == -1) {
            return;
        }

        int min = Math.min(startRow, endRow);
        int max = Math.max(startRow, endRow);

        for (int r = min; r <= max; r++) {
            JPanel cell = cells[r][column];
            boolean overlap = isBlockedCell(r, column);
            if (overlap) {
                cell.setBackground(new Color(230, 173, 187));
            } else {
                cell.setBackground(new Color(173, 216, 230));
            }
            cell.revalidate();
            cell.repaint();
        }
    }

    public void resetDragSelection() {
        clearDragSelection();
    }

    private void clearDragSelection() {
        repaintAllBlocks();
        startRow = -1;
        endRow = -1;
        column = -1;
    }

    public void updateSchedule(Schedule schedule) {
        // placeholder for future updates
        setTheme(currentThemeName);
    }

    public void clear() {
        blockedTimes.clear();
        manualBlocks.clear();
        repaintAllBlocks();
    }

    private void resetCellVisual(int r, int c) {
        JPanel cell = cells[r][c];
        cell.removeAll();
        boolean isDark = "Dark Mode".equals(currentThemeName);
        cell.setBackground(isDark ? Color.DARK_GRAY : Color.WHITE);
        cell.setBorder(BorderFactory.createLineBorder(isDark ? Color.GRAY : Color.LIGHT_GRAY));
        cell.revalidate();
        cell.repaint();
    }

    public void colorCell(String timeKey, Color color, String text, boolean isLocked) {
        try {
            ParsedTimeKey parsed = parseTimeKey(timeKey);
            if (parsed == null) {
                System.out.printf("[CalendarPanel] Unable to parse time key: %s%n", timeKey);
                return;
            }

            String normalizedTimeKey = toTimeKey(parsed.columnIndex, parsed.hour);

            int rowIndex = toRow(parsed.hour);
            int columnIndex = currentColumns == 1 ? 0 : parsed.columnIndex;
            if (columnIndex < 0 || columnIndex >= currentColumns) {
                System.out.printf("[CalendarPanel] Column index out of bounds for %s -> %d%n", timeKey, columnIndex);
                return;
            }

            JPanel cell = cells[rowIndex][columnIndex];
            cell.setBackground(color);
            cell.removeAll();
            cell.setLayout(new BorderLayout());

            JLabel label = new JLabel(text != null ? text : "", SwingConstants.CENTER);
            cell.add(label, BorderLayout.CENTER);

            String lockText = isLocked ? "\uD83D\uDD12" : "\uD83D\uDD13";
            JLabel lockLabel = new JLabel(lockText);
            lockLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lockLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            lockLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (lockListener != null) {
                        lockListener.onLockToggle(normalizedTimeKey);
                    }
                }
            });
            cell.add(lockLabel, BorderLayout.EAST);

            if (isLocked) {
                cell.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else {
                boolean isDark = "Dark Mode".equals(currentThemeName);
                cell.setBorder(BorderFactory.createLineBorder(isDark ? Color.GRAY : Color.LIGHT_GRAY));
            }

            cell.revalidate();
            cell.repaint();

            System.out.printf("[CalendarPanel] colorCell %s -> row %d, col %d, locked=%s%n", timeKey, rowIndex, columnIndex, isLocked);
        } catch (Exception ex) {
            System.out.printf("[CalendarPanel] Failed to color cell for %s due to %s%n", timeKey, ex.getMessage());
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

    private int toRow(int hour) {
        int row = hour - START_HOUR;
        return Math.min(Math.max(0, row), ROWS - 1);
    }

    private int toHour(int row) {
        int safeRow = Math.max(0, Math.min(row, ROWS - 1));
        return Math.min(23, START_HOUR + safeRow);
    }

    private String toTimeKey(int columnIndex, int hour) {
        DayOfWeek day = DayOfWeek.of((columnIndex % 7) + 1);
        String dayLabel = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        int safeHour = Math.max(0, Math.min(hour, 23));
        return String.format("%s %02d:00", dayLabel, safeHour);
    }

    public void colorBlockedRange(BlockedTime bt) {
        if (!blockedTimes.contains(bt)) {
            blockedTimes.add(bt);
        }
        renderBlockedTime(bt);
    }

    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int colIndex, String description) {
        manualBlocks.add(new ManualBlock(start, end, colIndex, description));
        int startH = start.getHour();
        int endH = end.getHour();
        renderRange(startH, endH, colIndex, description);
    }

    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int colIndex) {
        colorBlockedRange(start, end, colIndex, "Blocked");
    }

    private void renderBlockedTime(BlockedTime bt) {
        int col = bt.getColumnIndex();
        int start = bt.getStart().getHour();
        int end = bt.getEnd().getHour();
        renderRange(start, end, col, bt.getDescription());
    }

    private void renderRange(int startH, int endH, int col, String description) {
        if (description == null) {
            description = "Blocked";
        }
        for (int r = startH; r <= endH && r < START_HOUR + ROWS; r++) {
            int rowIndex = toRow(r);
            if (col < currentColumns) {
                JPanel cell = cells[rowIndex][col];

                cell.setBackground(Color.GRAY);
                cell.setBorder(BorderFactory.createLineBorder(Color.WHITE));
                cell.removeAll();

                if (r == startH) {
                    cell.setLayout(new BorderLayout());
                    JLabel label = new JLabel(description, SwingConstants.CENTER);
                    label.setForeground(Color.WHITE);
                    cell.add(label, BorderLayout.CENTER);
                }
                cell.revalidate();
                cell.repaint();
            }
        }
    }

    private void repaintAllBlocks() {
        if (cells == null) {
            return;
        }
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < currentColumns; c++) {
                resetCellVisual(r, c);
            }
        }
        for (BlockedTime bt : blockedTimes) {
            renderBlockedTime(bt);
        }
        for (ManualBlock mb : manualBlocks) {
            renderRange(mb.start.getHour(), mb.end.getHour(), mb.col, mb.description);
        }
    }

    private static class ManualBlock {
        LocalDateTime start;
        LocalDateTime end;
        int col;
        String description;

        ManualBlock(LocalDateTime start, LocalDateTime end, int col, String description) {
            this.start = start;
            this.end = end;
            this.col = col;
            this.description = description != null ? description : "Blocked";
        }
    }
}