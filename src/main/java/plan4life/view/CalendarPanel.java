package plan4life.view;

import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CalendarPanel extends JPanel {
    private JPanel[][] cells;
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
            }
        }

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
            }
        };

        // Attach panel-level listeners
        addMouseListener(ma);
        addMouseMotionListener(ma);

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
        setTheme(currentThemeName);
        revalidate();
        repaint();
    }

    public void clear() {
        this.blockedTimes.clear();
        this.manualBlocks.clear();
        for (int r = 0; r < rows; r++) {
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
            String[] parts = timeKey.split(":");
            int day = Integer.parseInt(parts[0]);    // 0-based already
            int hour = Integer.parseInt(parts[1]);   // 0–23

            if (day >= 0 && day < currentColumns && hour >= 0 && hour < rows) {
                JPanel cell = cells[hour][day];
                cell.setBackground(color);
                cell.removeAll();
                cell.setLayout(new BorderLayout());
                String lockText = isLocked ? "\uD83D\uDD12" : "\uD83D\uDD13"; // 🔒 vs 🔓 Unicode (had to google it, might be wrong)
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