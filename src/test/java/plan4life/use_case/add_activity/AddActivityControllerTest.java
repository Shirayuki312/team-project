package plan4life.use_case.add_activity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddActivityControllerTest {

    static class MockInteractor implements AddActivityInputBoundary {
        boolean called = false;
        AddActivityRequestModel lastRequest = null;

        @Override
        public AddActivityResponseModel execute(AddActivityRequestModel requestModel) {
            called = true;
            lastRequest = requestModel;
            return null; // controller doesn't use returned value
        }
    }

    @Test
    void testControllerCallsInteractor() {
        MockInteractor mock = new MockInteractor();
        AddActivityController controller = new AddActivityController(mock);

        controller.addActivity(5, "Read", 2.5f);

        assertTrue(mock.called);
        assertNotNull(mock.lastRequest);

        assertEquals(5, mock.lastRequest.getScheduleId());
        assertEquals("Read", mock.lastRequest.getDescription());
        assertEquals(2.5f, mock.lastRequest.getDuration());
    }
}