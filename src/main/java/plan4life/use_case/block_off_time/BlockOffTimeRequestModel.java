package plan4life.use_case.block_off_time;

import java.time.LocalDateTime;

public class BlockOffTimeRequestModel {
    /** The ID of the schedule to modify. */
    private final int scheduleId;
    /** The start time of the blocked interval. */
    private final LocalDateTime startTime;
    /** The end time of the blocked interval. */
    private final LocalDateTime endTime;
    /** A human-readable description of the blocked interval. */
    private final String description;
    /** The layout column index for the blocked interval. */
    private final int columnIndex;

    /**
     * Creates a new {@code BlockOffTimeRequestModel}.
     *
     * @param scheduleIdInput the schedule ID
     * @param startTimeInput the start time of the interval
     * @param endTimeInput the end time of the interval
     * @param descriptionInput the interval description
     * @param columnIndexInput the layout column index
     */
    public BlockOffTimeRequestModel(final int scheduleIdInput,
                                    final LocalDateTime startTimeInput,
                                    final LocalDateTime endTimeInput,
                                    final String descriptionInput,
                                    final int columnIndexInput) {
        this.scheduleId = scheduleIdInput;
        this.startTime = startTimeInput;
        this.endTime = endTimeInput;
        this.description = descriptionInput;
        this.columnIndex = columnIndexInput;
    }

    /**
     * Returns the schedule ID associated with this request.
     *
     * @return the schedule ID
     */
    public int getScheduleId() {
        return scheduleId;
    }

    /**
     * Returns the start time of the interval.
     *
     * @return the start time
     */
    public LocalDateTime getStart() {
        return startTime;
    }

    /**
     * Returns the end time of the interval.
     *
     * @return the end time
     */
    public LocalDateTime getEnd() {
        return endTime;
    }

    /**
     * Returns the description of the interval.
     *
     * @return the description text
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the layout column index for this interval.
     *
     * @return the column index
     */
    public int getColumnIndex() {
        return columnIndex;
    }
}
