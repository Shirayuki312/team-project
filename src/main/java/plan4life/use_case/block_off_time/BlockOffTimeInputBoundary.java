package plan4life.use_case.block_off_time;

/**
 * Input boundary for the block-off-time use case.
 * Defines the method required for interactor invocation.
 */
public interface BlockOffTimeInputBoundary {
    /**
     * Executes the block-off-time use case using the provided request model.
     *
     * @param requestModel the input data for the operation
     * @return the response model containing results of the operation
     */
    BlockOffTimeResponseModel execute(BlockOffTimeRequestModel requestModel);
}
