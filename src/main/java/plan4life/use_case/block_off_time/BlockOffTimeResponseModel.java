package plan4life.use_case.block_off_time;

import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;

import java.util.List;

/**
 * Response model for the block-off-time use case.
 * Contains the results of the operation including
 * success flag, message, updated blocked times,
 * and updated schedule.
 */
public class BlockOffTimeResponseModel {
    /** Whether the operation completed successfully. */
    private final boolean success;
    /** A human-readable message describing the result. */
    private final String message;
    /** The updated list of blocked time intervals. */
    private final List<BlockedTime> updatedBlockedTimes;
    /** The updated schedule after applying changes. */
    private final Schedule updatedSchedule;

    /**
     * Creates a new {@code BlockOffTimeResponseModel}.
     *
     * @param successInput whether the operation succeeded
     * @param messageInput message describing the result
     * @param updatedBlockedTimesInput list of updated blocks
     * @param updatedScheduleInput the updated schedule
     */
    public BlockOffTimeResponseModel(
            final boolean successInput,
            final String messageInput,
            final List<BlockedTime> updatedBlockedTimesInput,
            final Schedule updatedScheduleInput) {
        this.success = successInput;
        this.message = messageInput;
        this.updatedBlockedTimes = updatedBlockedTimesInput;
        this.updatedSchedule = updatedScheduleInput;
    }

    /**
     * Returns whether the operation succeeded.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the message associated with the result.
     *
     * @return the result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the updated list of blocked time intervals.
     *
     * @return list of updated blocked times
     */
    public List<BlockedTime> getUpdatedBlockedTimes() {
        return updatedBlockedTimes;
    }

    /**
     * Returns the updated schedule.
     *
     * @return the updated schedule object
     */
    public Schedule getUpdatedSchedule() {
        return updatedSchedule;
    }
}
