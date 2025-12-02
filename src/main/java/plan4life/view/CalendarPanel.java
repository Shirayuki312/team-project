package plan4life.view;

import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class CalendarPanel extends JPanel {
    private static final int START_HOUR = 6;
    private JPanel[][] cells;
    private JPanel gridPanel;
    private JPanel dayHeaderPanel;
    private JPanel timeLabelPanel;
    private int currentColumns = 7;
    private final int rows = 24;

    private int startRow = -1;
    private int endRow = -1;
    private int column = -1;
    private boolean dragging = false;

    // Persistent blocked times from backend
    private final List<BlockedTime> blockedTimes = new ArrayList<>();

    // Temporary manual blocks to keep them visible immediately (optional)
    private final List<ManualBlock> manualBlocks = new ArrayList<>();

    private TimeSelectionListener listener;
    private LockListener lockListener;

    private String currentThemeName = "Light Mode";

    public CalendarPanel() {
        // [Fix] Remove hardcoded title
        buildGrid(7);
    }

    public void updateTitle(String title) {
        this.setBorder(BorderFactory.createTitledBorder(title));
        this.repaint();
    }

//    public void setTheme(String themeName) {
//        this.currentThemeName = themeName;
//        boolean isDark = "Dark Mode".equals(themeName);
    private void buildGrid(int columns) {
        removeAll();
        currentColumns = columns;
        int rows = 24 - START_HOUR;

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
            JLabel hourLabel = new JLabel(String.format("%02d:00", START_HOUR + hour), SwingConstants.RIGHT);
            hourLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
            timeLabelPanel.add(hourLabel);
        }

        gridPanel = new JPanel(new GridLayout(rows, columns, 2, 2));
        setLayout(new GridLayout(rows, columns, 2, 2));

        cells = new JPanel[rows][columns];

        boolean isDark = "Dark Mode".equals(currentThemeName);
        Color bgColor = isDark ? Color.DARK_GRAY : Color.WHITE;
        Color gridColor = isDark ? Color.GRAY : Color.LIGHT_GRAY;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                JPanel cell = new JPanel();
                cell.setBackground(bgColor);
                cell.setBorder(BorderFactory.createLineBorder(gridColor));
                cells[r][c] = cell;
                add(cell);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                cells[hour][day] = cell;
                gridPanel.add(cell);
            }
        }

        gridPanel.addMouseListener(new MouseAdapter() {
        // Panel-level mouse handling (avoid per-cell listeners — fixes offset/inconsistent events)
        MouseAdapter ma = new MouseAdapter() {
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
                if (!dragging) return;
                Point p = e.getPoint();
                int newRow = getRowFromY(p.y);
                int newCol = getColumnFromX(p.x);
                if (newRow != -1) endRow = newRow;
                if (newCol != -1) column = newCol;
                updateSelectionVisual();
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
                            .withHour(toHour(min))
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    LocalDateTime end = now
                            .withHour(toHour(max + 1))
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);

                    int scheduleId = (currentColumns == 1) ? 1 : 2;
                    listener.onTimeSelected(start, end, scheduleId, column);
                }
            }
        });

                    if (listener != null) {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime start = now.withHour(min).withMinute(0).withSecond(0).withNano(0);
                        LocalDateTime end = now.withHour(max).withMinute(59).withSecond(59).withNano(999_999_999);
                        int scheduleId = (currentColumns == 1) ? 1 : 2;
                        listener.onTimeSelected(start, end, scheduleId, column);
                    }
                }

                // Always clear the visual selection and restore underlying blocks
                clearDragSelection();
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
        };

        // Attach panel-level listeners
        addMouseListener(ma);
        addMouseMotionListener(ma);

        add(dayHeaderPanel, BorderLayout.NORTH);
        add(timeLabelPanel, BorderLayout.WEST);
        add(gridPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    // --- Theme Logic ---
    public void setTheme(String themeName) {
        this.currentThemeName = themeName;
        boolean isDark = "Dark Mode".equals(themeName);

        Color bgColor = isDark ? Color.DARK_GRAY : Color.WHITE;
        Color gridColor = isDark ? Color.GRAY : Color.LIGHT_GRAY;

        this.setBackground(bgColor);

        if (cells != null) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < currentColumns; c++) {
                    if (cells[r][c] != null) {
                        // Only reset background if the underlying data doesn't dictate otherwise
                        if (!isBlockedCell(r, c)) {
                            cells[r][c].setBackground(bgColor);
                            cells[r][c].setBorder(BorderFactory.createLineBorder(gridColor));
                            cells[r][c].removeAll();
                        }
                    }
                }
            }
        }
        repaintAllBlocks();
        this.repaint();
    }

    private void handleRightClick(MouseEvent e) {
        if (lockListener == null) return;

        // convert point to the cell under the cursor
        Point p = e.getPoint();
        int c = getColumnFromX(p.x);
        int r = getRowFromY(p.y);
        if (r >= 0 && c >= 0) {
            String key = c + ":" + r; // 0-based
            lockListener.onLockToggle(key);
        }
    }

    private boolean isBlockedCell(int r, int c) {
        for (BlockedTime bt : blockedTimes) {
            if (bt.getColumnIndex() == c && r >= bt.getStart().getHour() && r <= bt.getEnd().getHour()) return true;
        }
        for (ManualBlock mb : manualBlocks) {
            if (mb.col == c && r >= mb.start.getHour() && r <= mb.end.getHour()) return true;
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
        // [Fix] Removed hardcoded title
        buildGrid(1);
        setTheme(currentThemeName);
        revalidate();
        repaint();
    }

    public void setWeekView() {
        // [Fix] Removed hardcoded title
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
        for (int r = 0; r < cells.length; r++) {
            Rectangle bounds = cells[r][0].getBounds();
            if (y >= bounds.y && y < bounds.y + bounds.height) {
                return r;
            }
        }

        Rectangle last = cells[cells.length - 1][0].getBounds();
        if (y > last.y + last.height + 3) {
            return cells.length - 1;
        }

        return Math.max(0, Math.min(cells.length - 1, cells.length * y / getHeight()));  // Approximate row if cursor is between cells
    }

    private void updateSelection() {
        for (int r = 0; r < cells.length; r++) {
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
        setTheme(currentThemeName);
        revalidate();
        repaint();
    }

    public void clear() {
        this.blockedTimes.clear();
        this.manualBlocks.clear();
        for (int r = 0; r < rows; r++) {
        for (int r = 0; r < cells.length; r++) {
            for (int c = 0; c < currentColumns; c++) {
                resetCellVisual(r, c);
            }
        }
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

    /**
     * Paint a single cell with given color and optional centered text and lock border.
     * This is used by external presenters to force a given visual state.
     */
    public void colorCell(String timeKey, Color color, String text, boolean isLocked) {
        try {
            ParsedTimeKey parsed = parseTimeKey(time);
            if (parsed == null) {
                System.out.printf("[CalendarPanel] Unable to parse time key: %s%n", time);
                return;
            }
            String[] parts = timeKey.split(":");
            int day = Integer.parseInt(parts[0]);    // 0-based already
            int hour = Integer.parseInt(parts[1]);   // 0–23

            int columnIndex = currentColumns == 1 ? 0 : parsed.columnIndex;
            if (columnIndex < 0 || columnIndex >= currentColumns) {
                System.out.printf("[CalendarPanel] Column index out of bounds for %s -> %d%n", time, columnIndex);
                return;
            }

            int rowIndex = toRow(parsed.hour);
            JPanel cell = cells[rowIndex][columnIndex];
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

            System.out.printf("[CalendarPanel] colorCell %s -> row %d, col %d, locked=%s%n", time, rowIndex, columnIndex, locked);
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

    private int toRow(int hour) {
        int row = hour - START_HOUR;
        if (cells == null || cells.length == 0) {
            return Math.max(0, row);
        }
        int max = cells.length - 1;
        return Math.min(Math.max(0, row), max);
    }

    private int toHour(int row) {
        int safeRow = Math.max(0, row);
        if (cells != null && cells.length > 0) {
            safeRow = Math.min(safeRow, cells.length - 1);
        }
        int hour = START_HOUR + safeRow;
        return Math.min(23, hour);
            if (day >= 0 && day < currentColumns && hour >= 0 && hour < rows) {
                JPanel cell = cells[hour][day];
                cell.setBackground(color);
                cell.removeAll();
                cell.setLayout(new BorderLayout());
                String lockText = isLocked ? "\uD83D\uDD12" : "\uD83D\uDD13";
                // 🔒 and 🔓 Unicode (had to google it, might be wrong)
                JLabel lockLabel = new JLabel(lockText);
                lockLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lockLabel.setBorder(BorderFactory.createEmptyBorder(2,6,2,6));
                lockLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (lockListener != null) lockListener.onLockToggle(timeKey);
                    }
                });
                if (text != null) {
                    JLabel label = new JLabel(text, SwingConstants.CENTER);
                    cell.add(label, BorderLayout.CENTER);
                }
                if (isLocked) {
                    cell.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                } else {
                    boolean isDark = "Dark Mode".equals(currentThemeName);
                    cell.setBorder(BorderFactory.createLineBorder(isDark ? Color.GRAY : Color.LIGHT_GRAY));
                }
                cell.add(lockLabel, BorderLayout.EAST);
                cell.revalidate();
                cell.repaint();
            }
        } catch (Exception ignored) {}
    }

    public void colorBlockedRange(BlockedTime bt) {
        // ensure not duplicated
        if (!this.blockedTimes.contains(bt)) {
            this.blockedTimes.add(bt);
        }
        renderBlockedTime(bt);
    }

    /**
     * Optional immediate feedback API - if controller wants to show the block immediately
     * before backend confirmation (manualBlocks), use the method with description param.
     */
    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int colIndex, String description) {
        // Keep a manualBlock for immediate visibility; controller should remove it if creation fails.
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
        if (description == null) description = "Blocked"; // ensure default text always shows
        for (int r = startH; r <= endH && r < rows; r++) {
            if (col < currentColumns) {
                JPanel cell = cells[r][col];

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

    private int getColumnFromX(int x) {
        if (currentColumns <= 0) return -1;
        int cellWidth = getWidth() / currentColumns;
        if (cellWidth == 0) return -1;
        int col = x / cellWidth;
        return Math.max(0, Math.min(currentColumns - 1, col));
    }

    private int getRowFromY(int y) {
        // Prefer exact bounds-based hit test to avoid offset issues
        if (cells == null) return Math.max(0, Math.min(rows - 1, y * rows / Math.max(1, getHeight())));
        for (int r = 0; r < rows; r++) {
            Rectangle bounds = cells[r][0].getBounds();
            if (y >= bounds.y && y < bounds.y + bounds.height) {
                return r;
            }
        }
        // fallback
        return Math.max(0, Math.min(rows - 1, rows * y / Math.max(1, getHeight())));
    }

    private void updateSelectionVisual() {
        // Clear previous visuals in the column then draw current selection
        if (column == -1) return;

        // First restore column visuals from underlying data
        for (int r = 0; r < rows; r++) {
            if (!isBlockedCell(r, column)) {
                resetCellVisual(r, column);
            } else {
                // redraw blocked cell if underlying data says it's blocked
                boolean shown = false;
                for (BlockedTime bt : blockedTimes) {
                    if (bt.getColumnIndex() == column && r >= bt.getStart().getHour() && r <= bt.getEnd().getHour()) {
                        renderBlockedTime(bt);
                        shown = true;
                        break;
                    }
                }
                if (!shown) {
                    // manual blocks
                    for (ManualBlock mb : manualBlocks) {
                        if (mb.col == column && r >= mb.start.getHour() && r <= mb.end.getHour()) {
                            renderRange(mb.start.getHour(), mb.end.getHour(), mb.col, mb.description);
                            break;
                        }
                    }
                }
            }
        }

        if (startRow == -1) return;

        int min = Math.max(0, startHour);
        int max = Math.min(23, endHour - 1);
        int minRow = toRow(min);
        int maxRow = toRow(max);
        int min = Math.min(startRow, endRow);
        int max = Math.max(startRow, endRow);

        for (int r = min; r <= max; r++) {
            JPanel cell = cells[r][column];

            // Determine overlap against persistent blockedTimes
            boolean overlap = false;
            for (BlockedTime bt : blockedTimes) {
                if (bt.getColumnIndex() == column && r >= bt.getStart().getHour() && r <= bt.getEnd().getHour()) {
                    overlap = true;
                    break;
                }
            }
            // Also check manualBlocks (these are intended to be displayed immediately)
            for (ManualBlock mb : manualBlocks) {
                if (mb.col == column && r >= mb.start.getHour() && r <= mb.end.getHour()) {
                    overlap = true;
                    break;
                }
            }

            if (overlap) {
                cell.setBackground(new Color(230, 173, 187)); // reddish for overlap
            } else {
                cell.setBackground(new Color(173, 216, 230)); // blueish for selection
            }
            cell.revalidate();
            cell.repaint();
        }
    }

    // [Conflict Resolution] Use this simplified method
    public void resetDragSelection() {
        clearDragSelection();
    }

    private void clearDragSelection() {
        if (column == -1 || startRow == -1) {
            // reset state
            startRow = endRow = column = -1;
            return;
        }

        for (int r = minRow; r <= maxRow; r++) {
            cells[r][columnIndex].setBackground(Color.GRAY);
            cells[r][columnIndex].removeAll();
            cells[r][columnIndex].add(new JLabel("Blocked"));
        }
    }
    // new listener interface inside or external:
    public interface LockListener {
        void onLockToggle(String timeKey);
    }
        int min = Math.min(startRow, endRow);
        int max = Math.max(startRow, endRow);

        // For each cell in the selection column, restore underlying blocked/empty visual
        for (int r = min; r <= max; r++) {
            boolean blocked = false;
            String description = null;

            for (BlockedTime bt : blockedTimes) {
                int col = bt.getColumnIndex();
                int btStart = bt.getStart().getHour();
                int btEnd = bt.getEnd().getHour();
                if (r >= btStart && r <= btEnd && column == col) {
                    blocked = true;
                    if (r == btStart) description = bt.getDescription();
                    break;
                }
            }

            if (blocked) {
                JPanel cell = cells[r][column];
                cell.setBackground(Color.GRAY);
                cell.removeAll();
                if (description != null) {
                    JLabel label = new JLabel(description, SwingConstants.CENTER);
                    cell.setLayout(new BorderLayout());
                    cell.add(label, BorderLayout.CENTER);
                }
                cell.revalidate();
                cell.repaint();
            } else {
                resetCellVisual(r, column);
            }
        }

        // reset drag state
        startRow = -1;
        endRow = -1;
        column = -1;
    }

    private void repaintAllBlocks() {
        // clear all cells then re-render
        for (int r = 0; r < rows; r++) {
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

    public void updateSchedule(Schedule schedule) {
        // placeholder if schedule updates required
    }

    private static class ManualBlock {
        LocalDateTime start;
        LocalDateTime end;
        int col;
        String description;

        public ManualBlock(LocalDateTime start, LocalDateTime end, int col, String description) {
            this.start = start;
            this.end = end;
            this.col = col;
            this.description = description != null ? description : "Blocked";
        }
    }
}