package plan4life.view;

import plan4life.entities.BlockedTime;

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
    private int rows = 24;

    private int startRow = -1;
    private int startCol = -1;
    private int endRow = -1;
    private int endCol = -1;
    private boolean dragging = false;

    private TimeSelectionListener timeSelectionListener;
    private LockListener lockListener;

    private final List<BlockedTime> blockedTimes = new ArrayList<>();
    private final List<ManualBlock> manualBlocks = new ArrayList<>();

    private String currentThemeName = "Light Mode";

    public CalendarPanel() {
        // [修复 1] 移除构造函数中的硬编码
        buildGrid(7);
        revalidate();
    }

    // [新增] 允许 CalendarFrame 修改边框标题
    public void updateTitle(String title) {
        this.setBorder(BorderFactory.createTitledBorder(title));
        this.repaint();
    }

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
                        Color current = cells[r][c].getBackground();
                        if (!isBlockedCell(r, c)) {
                            cells[r][c].setBackground(bgColor);
                            cells[r][c].setBorder(BorderFactory.createLineBorder(gridColor));
                        }
                    }
                }
            }
        }
        repaintAllBlocks();
        this.repaint();
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
        this.timeSelectionListener = listener;
    }

    public interface LockListener {
        void onLockToggle(String timeKey);
    }

    public void setLockListener(LockListener listener) {
        this.lockListener = listener;
    }

    private void buildGrid(int columns) {
        this.currentColumns = columns;
        removeAll();
        setLayout(new GridLayout(rows, columns, 2, 2));

        cells = new JPanel[rows][columns];

        MouseAdapter interactionHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e);
                    return;
                }
                dragging = true;
                Point p = convertPointToPanel(e);
                startCol = getColumnFromX(p.x);
                startRow = getRowFromY(p.y);
                endCol = startCol;
                endRow = startRow;
                updateSelection();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) return;
                Point p = convertPointToPanel(e);
                int newRow = getRowFromY(p.y);
                int newCol = getColumnFromX(p.x);

                if (newRow != -1 && newCol != -1 && (newRow != endRow || newCol != endCol)) {
                    endRow = newRow;
                    endCol = newCol;
                    updateSelection();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragging) return;
                dragging = false;

                if (startCol != -1 && startRow != -1 && endCol != -1 && endRow != -1) {
                    int minRow = Math.min(startRow, endRow);
                    int maxRow = Math.max(startRow, endRow);
                    int col = startCol;

                    if (timeSelectionListener != null) {
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime start = now.withHour(minRow).withMinute(0).withSecond(0).withNano(0);
                        LocalDateTime end = now.withHour(maxRow).withMinute(59).withSecond(59).withNano(999999999);
                        int scheduleId = (currentColumns == 1) ? 1 : 2;
                        timeSelectionListener.onTimeSelected(start, end, scheduleId, col);
                    }
                }

                int tempCol = startCol;
                int tempStart = Math.min(startRow, endRow);
                int tempEnd = Math.max(startRow, endRow);

                startRow = startCol = endRow = endCol = -1;

                if (tempCol != -1) {
                    for (int r = tempStart; r <= tempEnd; r++) resetCellColor(r, tempCol);
                }

                repaintAllBlocks();
            }
        };

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                JPanel cell = new JPanel();
                boolean isDark = "Dark Mode".equals(currentThemeName);
                cell.setBackground(isDark ? Color.DARK_GRAY : Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(isDark ? Color.GRAY : Color.LIGHT_GRAY));

                cell.addMouseListener(interactionHandler);
                cell.addMouseMotionListener(interactionHandler);
                cell.putClientProperty("row", r);
                cell.putClientProperty("col", c);

                add(cell);
                cells[r][c] = cell;
            }
        }
    }

    private void handleRightClick(MouseEvent e) {
        if (lockListener != null) {
            JComponent c = (JComponent) e.getComponent();
            Object rObj = c.getClientProperty("row");
            Object cObj = c.getClientProperty("col");
            if (rObj != null && cObj != null) {
                int r = (int) rObj;
                int cIdx = (int) cObj;
                String key = (cIdx + 1) + ":" + r;
                lockListener.onLockToggle(key);
            }
        }
    }

    private Point convertPointToPanel(MouseEvent e) {
        return SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);
    }

    public void setDayView() {
        // [关键修复] 移除硬编码
        buildGrid(1);
        setTheme(currentThemeName);
        revalidate();
        repaint();
    }

    public void setWeekView() {
        // [关键修复] 移除硬编码
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
                resetCellColor(r, c);
            }
        }
    }

    private void resetCellColor(int r, int c) {
        cells[r][c].removeAll();
        boolean isDark = "Dark Mode".equals(currentThemeName);
        cells[r][c].setBackground(isDark ? Color.DARK_GRAY : Color.WHITE);
        cells[r][c].setBorder(BorderFactory.createLineBorder(isDark ? Color.GRAY : Color.LIGHT_GRAY));
        cells[r][c].revalidate();
        cells[r][c].repaint();
    }

    public void colorCell(String timeKey, Color color, String text, boolean isLocked) {
        try {
            String[] parts = timeKey.split(":");
            int day = Integer.parseInt(parts[0]) - 1;
            int hour = Integer.parseInt(parts[1]);

            if (day >= 0 && day < currentColumns && hour >= 0 && hour < rows) {
                JPanel cell = cells[hour][day];
                cell.setBackground(color);
                cell.removeAll();
                cell.setLayout(new BorderLayout());
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
                cell.revalidate();
                cell.repaint();
            }
        } catch (Exception ignored) {}
    }

    public void colorBlockedRange(BlockedTime bt) {
        if (!this.blockedTimes.contains(bt)) {
            this.blockedTimes.add(bt);
        }
        renderBlockedTime(bt);
    }

    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int colIndex, String description) {
        manualBlocks.add(new ManualBlock(start, end, colIndex, description));
        int startH = start.getHour();
        int endH = end.getHour();
        renderRange(startH, endH, colIndex, description);
    }

    private void renderBlockedTime(BlockedTime bt) {
        int col = bt.getColumnIndex();
        int start = bt.getStart().getHour();
        int end = bt.getEnd().getHour();
        renderRange(start, end, col, bt.getDescription());
    }

    private void renderRange(int startH, int endH, int col, String description) {
        for (int r = startH; r <= endH && r < rows; r++) {
            if (col < currentColumns) {
                JPanel cell = cells[r][col];

                cell.setBackground(Color.GRAY);

                cell.setBorder(BorderFactory.createLineBorder(Color.WHITE));

                cell.removeAll();

                if (r == startH) {
                    cell.setLayout(new BorderLayout());
                    JLabel label = new JLabel(description, SwingConstants.CENTER);
                    label.setForeground(Color.WHITE); // White Text
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
        if (rows <= 0) return -1;
        int cellHeight = getHeight() / rows;
        if (cellHeight == 0) return -1;
        int row = y / cellHeight;
        return Math.max(0, Math.min(rows - 1, row));
    }

    private void updateSelection() {
        if (startRow == -1 || startCol == -1) return;
        int minRow = Math.min(startRow, endRow);
        int maxRow = Math.max(startRow, endRow);
        int col = startCol;

        for (int r = 0; r < rows; r++) {
            if (r >= minRow && r <= maxRow) {
                cells[r][col].setBackground(new Color(173, 216, 230));
            } else {
                boolean isBlocked = isBlockedCell(r, col);
                if (!isBlocked) {
                    resetCellColor(r, col);
                } else {
                    repaintAllBlocks();
                }
            }
        }
    }

    public void resetDragSelection() {
        repaintAllBlocks();
    }

    private void repaintAllBlocks() {
        for(BlockedTime bt : blockedTimes) renderBlockedTime(bt);
        for(ManualBlock mb : manualBlocks) renderRange(mb.start.getHour(), mb.end.getHour(), mb.col, mb.description);
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
            this.description = description;
        }
    }
}