package plan4life.use_case.block_off_time;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BlockOffTimeControllerTest {
    static class MockInteractor implements BlockOffTimeInputBoundary {
        boolean called = false;
        BlockOffTimeRequestModel lastRequest = null;

        @Override
        public BlockOffTimeResponseModel execute(BlockOffTimeRequestModel requestModel) {
            called = true;
            lastRequest = requestModel;
            return null;
        }
    }

    @Test
    void testControllerCallsInteractor() {
        MockInteractor mock = new MockInteractor();
        BlockOffTimeController controller = new BlockOffTimeController(mock);

        LocalDateTime start = LocalDateTime.of(2025, 12, 2, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 2, 11, 0);

        controller.blockTime(5, start, end, "Break", 2);

        assertTrue(mock.called, "Controller should call interactor.execute()");
        assertNotNull(mock.lastRequest);

        assertEquals(5, mock.lastRequest.getScheduleId());
        assertEquals(start, mock.lastRequest.getStart());
        assertEquals(end, mock.lastRequest.getEnd());
        assertEquals("Break", mock.lastRequest.getDescription());
        assertEquals(2, mock.lastRequest.getColumnIndex());
    }
}