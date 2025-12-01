package plan4life.use_case.block_off_time;

import java.time.LocalDateTime;

/**
 * Controller responsible for creating and dispatching block-off-time
 * requests to the interactor.
 */
public class BlockOffTimeController {
    /** The interactor that processes block-off-time requests. */
    private final BlockOffTimeInputBoundary interactor;

    /**
     * Constructs a new controller for block-off-time operations.
     *
     * @param interactorInput the interactor to delegate requests to
     */
    public BlockOffTimeController(
            final BlockOffTimeInputBoundary interactorInput) {
        this.interactor = interactorInput;
    }

    /**
     * Sends a block-off-time request to the interactor.
     *
     * @param scheduleId the schedule to modify
     * @param start the start time of the blocked interval
     * @param end the end time of the blocked interval
     * @param description a human-readable description of the block
     * @param columnIndex the layout column index
     */
    public void blockTime(
            final int scheduleId,
            final LocalDateTime start,
            final LocalDateTime end,
            final String description,
            final int columnIndex) {
        BlockOffTimeRequestModel request = new BlockOffTimeRequestModel(
            scheduleId,
            start,
            end,
            description,
            columnIndex);
        interactor.execute(request);
    }
}
