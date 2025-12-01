package plan4life.use_case.block_off_time;

/**
 * Output boundary for the block-off-time use case.
 * Responsible for presenting the response model.
 */
public interface BlockOffTimeOutputBoundary {
    /**
     * Presents the result of the block-off-time operation.
     *
     * @param responseModel the response data to present
     */
    void present(BlockOffTimeResponseModel responseModel);
}
