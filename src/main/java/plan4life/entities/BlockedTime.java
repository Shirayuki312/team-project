package plan4life.entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class BlockedTime {
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final String description;
    private final int columnIndex;

    public BlockedTime(LocalDateTime start, LocalDateTime end, String description, int columnIndex) {
        this.start = start;
        this.end = end;
        this.columnIndex = columnIndex;
        if (Objects.equals(description, "")) {
            this.description = "Blocked";
        } else {
            this.description = description;
        }
    }

    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public String getDescription() { return description; }
    public int getColumnIndex() { return columnIndex; }

    // Domain rule: check if two intervals overlap
    public boolean overlaps(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return !(otherEnd.isBefore(start) || otherStart.isAfter(end));
    }
}