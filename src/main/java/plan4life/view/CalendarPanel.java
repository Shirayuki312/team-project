package plan4life.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

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

    public CalendarPanel() {
        setBorder(BorderFactory.createTitledBorder("Weekly Calendar"));
        buildGrid(7);
        revalidate();
    }

    // --- [修复报错] 添加变色逻辑 ---
    public void setTheme(String themeName) {
        boolean isDark = "Dark Mode".equals(themeName);

        // 设置颜色
        Color bgColor = isDark ? Color.DARK_GRAY : Color.WHITE;
        Color gridColor = isDark ? Color.GRAY : Color.LIGHT_GRAY;

        // 遍历格子修改颜色
        if (cells != null) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < currentColumns; c++) {
                    if (cells[r][c] != null) {
                        // 保持原有逻辑：如果不是被占用的(有颜色的)，才改变背景
                        if (cells[r][c].getBackground().equals(Color.WHITE) ||
                                cells[r][c].getBackground().equals(Color.DARK_GRAY)) {
                            cells[r][c].setBackground(bgColor);
                        }
                        cells[r][c].setBorder(BorderFactory.createLineBorder(gridColor));
                    }
                }
            }
        }

        this.setBackground(bgColor);
        this.repaint();
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

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                JPanel cell = new JPanel();
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

                // Add click listener for locking (Teammate's logic)
                int finalR = r;
                int finalC = c;
                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e) && lockListener != null) {
                            // Assuming format "day:hour"
                            String key = (finalC + 1) + ":" + finalR;
                            lockListener.onLockToggle(key);
                        }
                    }
                });

                add(cell);
                cells[r][c] = cell;
            }
        }

        // Drag listener logic
        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                startCol = getColumnFromX(e.getX());
                startRow = getRowFromY(e.getY());
                endCol = startCol;
                endRow = startRow;
                updateSelection();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
                if (startCol != -1 && startRow != -1 && endCol != -1 && endRow != -1) {
                    int minRow = Math.min(startRow, endRow);
                    int maxRow = Math.max(startRow, endRow);
                    int col = startCol; // Assume single column selection for now

                    if (timeSelectionListener != null) {
                        // Mocking date for simplicity. Real app needs actual dates.
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime start = now.withHour(minRow).withMinute(0);
                        LocalDateTime end = now.withHour(maxRow + 1).withMinute(0);
                        // Passing a fake scheduleID (1) for now
                        timeSelectionListener.onTimeSelected(start, end, 1, col);
                    }
                }
                startRow = startCol = endRow = endCol = -1;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) return;
                int newRow = getRowFromY(e.getY());
                int newCol = getColumnFromX(e.getX());
                if (newRow != -1 && newCol != -1 && (newRow != endRow || newCol != endCol)) {
                    endRow = newRow;
                    endCol = newCol;
                    updateSelection();
                }
            }
        };

        addMouseListener(dragHandler);
        addMouseMotionListener(dragHandler);
    }

    public void setDayView() {
        setBorder(BorderFactory.createTitledBorder("Daily Calendar"));
        buildGrid(1);
        revalidate();
        repaint();
    }

    public void setWeekView() {
        setBorder(BorderFactory.createTitledBorder("Weekly Calendar"));
        buildGrid(7);
        revalidate();
        repaint();
    }

    public void clear() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < currentColumns; c++) {
                cells[r][c].removeAll();
                cells[r][c].setBackground(Color.WHITE);
                cells[r][c].revalidate();
                cells[r][c].repaint();
            }
        }
    }

    public void colorCell(String timeKey, Color color, String text, boolean isLocked) {
        // Parsing logic "day:hour"
        try {
            String[] parts = timeKey.split(":");
            int day = Integer.parseInt(parts[0]) - 1;
            int hour = Integer.parseInt(parts[1]);

            if (day >= 0 && day < currentColumns && hour >= 0 && hour < rows) {
                cells[hour][day].setBackground(color);
                if (text != null) {
                    cells[hour][day].add(new JLabel(text));
                }
                if (isLocked) {
                    cells[hour][day].setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }

    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int colIndex) {
        int startH = start.getHour();
        int endH = end.getHour();

        for (int r = startH; r < endH && r < rows; r++) {
            if (colIndex < currentColumns) {
                cells[r][colIndex].setBackground(Color.LIGHT_GRAY);
                cells[r][colIndex].add(new JLabel("Blocked"));
            }
        }
    }

    private int getColumnFromX(int x) {
        if (currentColumns <= 0) return -1;
        int cellWidth = getWidth() / currentColumns;
        if (cellWidth == 0) return -1;
        int col = x / cellWidth;
        return Math.min(col, currentColumns - 1);
    }

    private int getRowFromY(int y) {
        if (rows <= 0) return -1;
        int cellHeight = getHeight() / rows;
        if (cellHeight == 0) return -1;
        int row = y / cellHeight;
        return Math.min(row, rows - 1);
    }

    private void updateSelection() {
        if (startRow == -1 || startCol == -1) return;
        int minRow = Math.min(startRow, endRow);
        int maxRow = Math.max(startRow, endRow);
        int col = startCol;

        for (int r = 0; r < rows; r++) {
            // Reset current column highlight temporarily
            if (r < minRow || r > maxRow) {
                // Restore original color logic if needed, here simplified
            } else {
                cells[r][col].setBackground(new Color(173, 216, 230));
            }
        }
    }
}