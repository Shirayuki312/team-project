package plan4life.use_case.generate_schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.entities.Schedule;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenerateScheduleInteractorTest {

    private MockPresenter presenter;
    private MockGenerationService generationService;
    private GenerateScheduleInteractor interactor;

    @BeforeEach
    void setup() {
        presenter = new MockPresenter();
        generationService = new MockGenerationService();
        interactor = new GenerateScheduleInteractor(presenter, generationService);
    }

    @Test
    void testGenerateSchedule_basic() {
        Map<String, String> fixed = new HashMap<>();
        fixed.put("7:00-8:00", "Breakfast");
        fixed.put("12:00-13:00", "Lunch");

        GenerateScheduleRequestModel request =
                new GenerateScheduleRequestModel("Study, Gym, Relax", fixed);

        interactor.execute(request);

        // Presenter should be called
        assertTrue(presenter.wasCalled);
        assertNotNull(presenter.lastResponse);

        // Schedule should be the one produced by MockGenerationService
        Schedule schedule = presenter.lastResponse.getSchedule();
        assertNotNull(schedule);

        // The mock service inserts deterministic test values
        assertEquals("MOCK_ACTIVITY", schedule.getActivities().get("09:00-10:00"));
        assertEquals("Breakfast", schedule.getActivities().get("7:00-8:00"));
        assertEquals("Lunch", schedule.getActivities().get("12:00-13:00"));

        assertEquals("Study, Gym, Relax", generationService.lastDescription);
        assertEquals(fixed, generationService.lastFixed);
    }

    @Test
    void testGenerateSchedule_nullRequest() {
        interactor.execute(null);

        assertTrue(presenter.wasCalled);
        assertNotNull(presenter.lastResponse);

        Schedule schedule = presenter.lastResponse.getSchedule();
        assertNotNull(schedule);

        // Null request should produce an empty schedule
        assertTrue(schedule.getActivities().isEmpty());
    }

    // --------------------------------------------------------
    // Mock Presenter
    // --------------------------------------------------------
    private static class MockPresenter implements GenerateScheduleOutputBoundary {
        boolean wasCalled = false;
        GenerateScheduleResponseModel lastResponse;

        @Override
        public void present(GenerateScheduleResponseModel responseModel) {
            wasCalled = true;
            lastResponse = responseModel;
        }
    }

    // --------------------------------------------------------
    // Mock ScheduleGenerationService
    // --------------------------------------------------------
    private static class MockGenerationService implements ScheduleGenerationService {

        String lastDescription;
        Map<String,String> lastFixed;

        @Override
        public Schedule generate(String routineDescription, Map<String, String> fixed) {
            this.lastDescription = routineDescription;
            this.lastFixed = fixed;

            // Deterministic mock output
            Schedule schedule = new Schedule();
            schedule.getActivities().put("09:00-10:00", "MOCK_ACTIVITY");

            // Include fixed activities as the real service would
            if (fixed != null) {
                schedule.getActivities().putAll(fixed);
            }

            return schedule;
        }
    }
}



