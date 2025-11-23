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
    private int rows = 24;

    private int startRow = -1;
    private int startCol = -1;
    private int endRow = -1;
    private int endCol = -1;
    private boolean dragging = false;
    private final List<BlockedTime> blockedTimes = new ArrayList<>();

    private TimeSelectionListener timeSelectionListener;
    private LockListener lockListener;

    public CalendarPanel() {
        setBorder(BorderFactory.createTitledBorder("Weekly Calendar"));
        buildGrid(7);
        revalidate();
    }

    public void setTheme(String themeName) {
        boolean isDark = "Dark Mode".equals(themeName);

        // set color
        Color bgColor = isDark ? Color.DARK_GRAY : Color.WHITE;
        Color gridColor = isDark ? Color.GRAY : Color.LIGHT_GRAY;

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

                if (column != -1 && startRow != -1 && endRow != -1) {
                    int min = Math.min(startRow, endRow);
                    int max = Math.max(startRow, endRow);
                    LocalDateTime now = LocalDateTime.now();

                    if (listener == null) return;

                    // TODO: Using today's date as a placeholder. Replace with actual selected calendar date once implemented.
                    LocalDateTime start = now
                            .withHour(min)
                            .withMinute(0)
                            .withSecond(0)
                            .withNano(0);
                    LocalDateTime end = now
                            .withHour(max)
                            .withMinute(59)
                            .withSecond(59)
                            .withNano(999_999_999);

                    int scheduleId = (currentColumns == 1) ? 1 : 2;
                    listener.onTimeSelected(start, end, scheduleId, column);
                }

                clearDragSelection();
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

    public void setTimeSelectionListener(TimeSelectionListener listener) {
        this.listener = listener;
    }

    private int getColumnFromX(int x) {
        int cellWidth = getWidth() / currentColumns;
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
        for (BlockedTime bt : blockedTimes) {
            int col = bt.getColumnIndex();
            int start = bt.getStart().getHour();
            int end = bt.getEnd().getHour();

            for (int r = start; r <= end; r++) {
                JPanel cell = cells[r][col];
                cell.setBackground(Color.GRAY);
                cell.removeAll();
                if (r == start) { // description goes on the first cell only
                    JLabel label = new JLabel(bt.getDescription(), SwingConstants.CENTER);
                    cell.setLayout(new BorderLayout());
                    cell.add(label, BorderLayout.CENTER);
                }
                cell.revalidate();
                cell.repaint();
            }
        }
    }

        if (column != -1 && startRow != -1) {
            int min = Math.min(startRow, endRow);
            int max = Math.max(startRow, endRow);

            for (int r = min; r <= max; r++) {
                JPanel cell = cells[r][column];

                // checks if this cell overlpas an existing block
                boolean overlap = false;
                for (BlockedTime bt : blockedTimes) {
                    int col = bt.getColumnIndex();
                    int btStart = bt.getStart().getHour();
                    int btEnd = bt.getEnd().getHour();
                    if (column == col && r >= btStart && r <= btEnd) {
                        overlap = true;
                        break;
                    }
                }

                if (overlap) {
                    cell.setBackground(new Color(230, 173, 187)); // redish for overlap
                } else {
                    cell.setBackground(new Color(173, 216, 230)); // blueish for selection
                }

                cell.revalidate();
                cell.repaint();
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

    private void clearDragSelection() {
        if (column == -1 || startRow == -1) return;

        int min = Math.min(startRow, endRow);
        int max = Math.max(startRow, endRow);

        for (int r = min; r <= max; r++) {
            // redraw underlying blocked range or empty
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

            JPanel cell = cells[r][column];
            if (blocked) {
                cell.setBackground(Color.GRAY);
                cell.removeAll();
                if (description != null) {
                    JLabel label = new JLabel(description, SwingConstants.CENTER);
                    cell.setLayout(new BorderLayout());
                    cell.add(label, BorderLayout.CENTER);
                }
            } else {
                cell.setBackground(Color.WHITE);
                cell.removeAll();
            }

            cell.revalidate();
            cell.repaint();
        }

        // reset drag state
        startRow = -1;
        endRow = -1;
        column = -1;
    }

    public void resetDragSelection() {
        clearDragSelection();
    }

    public void colorCell(String time, Color color, String label, boolean locked) {
        try {
            // Assuming time corresponds to the first column's row index mapping logic you already have
            int hour = Integer.parseInt(time.split(" ")[1].split(":")[0]); // crude, adapt to your keys
            int columnIndex = 0; // if using daily view otherwise compute from day name

            JPanel cell = cells[hour][columnIndex];
            cell.removeAll();
            cell.setBackground(color);
            cell.setLayout(new BorderLayout());

            JLabel title = new JLabel(label, SwingConstants.CENTER);
            title.setOpaque(false);
            cell.add(title, BorderLayout.CENTER);

            // Create a small lock label on EAST
            String lockText = locked ? "\uD83D\uDD12" : "\uD83D\uDD13"; // 🔒 vs 🔓 unicode (had to google it, might be wrong)
            JLabel lockLabel = new JLabel(lockText);
            lockLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lockLabel.setBorder(BorderFactory.createEmptyBorder(2,6,2,6));
            lockLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (lockListener != null) lockListener.onLockToggle(time);
                }
            });

            cell.add(lockLabel, BorderLayout.EAST);
            cell.revalidate();
            cell.repaint();
        } catch (Exception ignored) {}
    }


    public void colorBlockedRange(BlockedTime blockedTime) {
        int columnIndex = blockedTime.getColumnIndex();
        int startHour = blockedTime.getStart().getHour();
        int endHour = blockedTime.getEnd().getHour();

        for (int r = startHour; r <= endHour; r++) {
            JPanel cell = cells[r][columnIndex];
            cell.setBackground(Color.GRAY);
            cell.removeAll();

            if (r == startHour) { // only first cell gets description
                JLabel label = new JLabel(blockedTime.getDescription(), SwingConstants.CENTER);
                cell.setLayout(new BorderLayout());
                cell.add(label, BorderLayout.CENTER);
            }
        }

        blockedTimes.add(blockedTime); // track for redrawing later
    }

    private void renderBlockedCell(int r, int c) {
        JPanel cell = cells[r][c];
        cell.setBackground(Color.GRAY);
        cell.removeAll();
    }

    // new listener interface inside or external:
    public interface LockListener {
        void onLockToggle(String timeKey);
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