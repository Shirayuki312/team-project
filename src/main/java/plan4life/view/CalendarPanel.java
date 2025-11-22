package plan4life.view;

import plan4life.entities.Schedule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

public class CalendarPanel extends JPanel {
    private JPanel[][] cells;
    private int currentColumns = 7;

    private int startRow = -1;
    private int endRow = -1;
    private int column = -1;
    private boolean dragging = false;
    private boolean[][] isBlocked;

    private TimeSelectionListener listener;

    public CalendarPanel() {
        setBorder(BorderFactory.createTitledBorder("Weekly Calendar"));
        buildGrid(7);
    }

    private void buildGrid(int columns) {
        removeAll();
        currentColumns = columns;
        isBlocked = new boolean[24][columns];
        int rows = 24;

        // Each component added to a GridLayout fills the next cell in the grid (left to right, then top to bottom)
        setLayout(new GridLayout(rows, columns, 2, 2));
        cells = new JPanel[rows][columns];

        for (int hour = 0; hour < rows; hour++) {
            for (int day = 0; day < columns; day++) {
                JPanel cell = new JPanel();
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                cells[hour][day] = cell;
                add(cell);
            }
        }

        addMouseListener(new MouseAdapter() {
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
            }
        });

        addMouseMotionListener(new MouseAdapter() {
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
        for (int r = 0; r < 24; r++) {
            for (int c = 0; c < currentColumns; c++) {
                if (isBlocked[r][c]) {
                    renderBlockedCell(r, c);
                }
                else {
                    cells[r][c].setBackground(Color.WHITE);
                    cells[r][c].removeAll();
                }
            }
        }

        if (column != -1 && startRow != -1) {
            int min = Math.min(startRow, endRow);
            int max = Math.max(startRow, endRow);
            for (int r = min; r <= max; r++) {
                if (isBlocked[r][column]) {
                    cells[r][column].setBackground(new Color(230, 173, 187));
                }
                else {
                    cells[r][column].setBackground(new Color(173, 216, 230));
                }
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


    public void colorBlockedRange(LocalDateTime start, LocalDateTime end, int columnIndex) {
        int startHour = start.getHour();
        int endHour = end.getHour();

        int min = Math.max(0, startHour);
        int max = Math.min(23, endHour);

        if (columnIndex < 0 || columnIndex >= currentColumns) {
            return;
        }

        for (int r = min; r <= max; r++) {
            isBlocked[r][columnIndex] = true;
            renderBlockedCell(r, columnIndex);
//            cells[r][columnIndex].setBackground(Color.GRAY);
//            cells[r][columnIndex].removeAll();
//            cells[r][columnIndex].add(new JLabel("Blocked"));
        }
    }

    private void renderBlockedCell(int r, int c) {
        JPanel cell = cells[r][c];
        cell.setBackground(Color.GRAY);
        cell.removeAll();
        cell.add(new JLabel("Blocked"));
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