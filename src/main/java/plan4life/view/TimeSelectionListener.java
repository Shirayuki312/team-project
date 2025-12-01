package plan4life.view;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeSelectionListener {
    // [Changed] Now accepts a list of columns instead of a single column
    void onTimeSelected(LocalDateTime start, LocalDateTime end, int scheduleId, List<Integer> columnIndices);
}