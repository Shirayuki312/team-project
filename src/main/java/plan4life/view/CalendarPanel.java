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

    public CalendarPanel() {
        setBorder(BorderFactory.createTitledBorder("Weekly Calendar"));
        buildGrid(7);
        revalidate();
    }

    // Deep Mode Logic
    public void setTheme(String themeName) {
        boolean isDark = "Dark Mode".equals(themeName);
        Color bgColor = isDark ? Color.DARK_GRAY : Color.WHITE;
        Color gridColor = isDark ? Color.GRAY : Color.LIGHT_GRAY;

        this.setBackground(bgColor);

        if (cells != null) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < currentColumns; c++) {
                    if (cells[r][c] != null) {
                        Color current = cells[r][c].getBackground();
                        if (current.equals(Color.WHITE) || current.equals(Color.DARK_GRAY)) {
                            cells[r][c].setBackground(bgColor);
                        }
                        cells[r][c].setBorder(BorderFactory.createLineBorder(gridColor));
                    }
                }
            }
        }
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

                int finalR = r;
                int finalC = c;
                cell.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e) && lockListener != null) {
                            String key = (finalC + 1) + ":" + finalR;
                            lockListener.onLockToggle(key);
                        }
                    }
                });

                add(cell);
                cells[r][c] = cell;
            }
        }

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

            @Override
            public void mouseReleased(MouseEvent e) {
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
                startRow = startCol = endRow = endCol = -1;
                resetDragSelection();
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
        this.blockedTimes.clear();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < currentColumns; c++) {
                resetCell(r, c);
            }
        }
    }

    private void resetCell(int r, int c) {
        cells[r][c].removeAll();
        cells[r][c].setBackground(Color.WHITE);
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

                if (text != null) {
                    cell.add(new JLabel(text));
                }
                if (isLocked) {
                    cell.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                } else {
                    cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                }

                cell.revalidate();
                cell.repaint();
            }
        } catch (Exception ignored) {}
    }

    public void colorBlockedRange(BlockedTime bt) {
        this.blockedTimes.add(bt);
        renderBlockedTime(bt);
    }

    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int colIndex) {
        int startH = start.getHour();
        int endH = end.getHour();
        for (int r = startH; r <= endH && r < rows; r++) {
            if (colIndex < currentColumns) {
                cells[r][colIndex].setBackground(Color.LIGHT_GRAY);
                cells[r][colIndex].add(new JLabel("Blocked"));
            }
        }
    }

    private void renderBlockedTime(BlockedTime bt) {
        int col = bt.getColumnIndex();
        int start = bt.getStart().getHour();
        int end = bt.getEnd().getHour();

        for (int r = start; r <= end && r < rows; r++) {
            if (col < currentColumns) {
                JPanel cell = cells[r][col];
                cell.setBackground(Color.LIGHT_GRAY);
                cell.removeAll();
                if (r == start) {
                    cell.add(new JLabel(bt.getDescription()));
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
                boolean isBlocked = false;
                for(BlockedTime bt : blockedTimes) {
                    if(bt.getColumnIndex() == col && r >= bt.getStart().getHour() && r <= bt.getEnd().getHour()) {
                        renderBlockedTime(bt);
                        isBlocked = true;
                        break;
                    }
                }
                if(!isBlocked) {
                    cells[r][col].setBackground(Color.WHITE);
                }
            }
        }
    }

    public void resetDragSelection() {
        for(BlockedTime bt : blockedTimes) {
            renderBlockedTime(bt);
        }
    }
}