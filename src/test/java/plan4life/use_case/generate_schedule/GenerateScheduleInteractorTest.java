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
        fixed.put("7:00", "Breakfast");
        fixed.put("12:00", "Lunch");

        List<String> free = Arrays.asList(
                "Jogging (1.0h)",
                "Study (2h)"
        );

        GenerateScheduleRequestModel request =
                new GenerateScheduleRequestModel(
                        "Study, Gym, Relax",
                        fixed,
                        free
                );

        interactor.execute(request);

        assertTrue(presenter.wasCalled);
        assertNotNull(presenter.lastResponse);

        Schedule schedule = presenter.lastResponse.getSchedule();
        assertNotNull(schedule);

        // From mock generator
        assertEquals("MOCK_ACTIVITY", schedule.getActivities().get("09:00"));

        // Fixed activities preserved
        assertEquals("Breakfast", schedule.getActivities().get("7:00"));
        assertEquals("Lunch", schedule.getActivities().get("12:00"));

        // Free activities assigned by Mock assignActivityToSlot
        assertTrue(generationService.assigned.contains("Jogging"));
        assertTrue(generationService.assigned.contains("Study"));

        // Did the interactor pass inputs correctly?
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

        // Null request → new Schedule() with empty activity map
        assertTrue(schedule.getActivities().isEmpty());
    }

    // ------------------------------
    // Mock Presenter
    // ------------------------------
    private static class MockPresenter implements GenerateScheduleOutputBoundary {
        boolean wasCalled = false;
        GenerateScheduleResponseModel lastResponse;

        @Override
        public void present(GenerateScheduleResponseModel responseModel) {
            wasCalled = true;
            lastResponse = responseModel;
        }
    }

    // ------------------------------
    // Mock ScheduleGenerationService
    // ------------------------------
    private static class MockGenerationService implements ScheduleGenerationService {

        String lastDescription;
        Map<String,String> lastFixed;
        List<String> assigned = new ArrayList<>();

        @Override
        public Schedule generate(String routineDescription,
                                 Map<String, String> fixedActivities) {

            this.lastDescription = routineDescription;
            this.lastFixed = fixedActivities;

            Schedule schedule = new Schedule();

            // Add mock data using real API
            schedule.addActivity("09:00", "MOCK_ACTIVITY");

            if (fixedActivities != null) {
                for (var e : fixedActivities.entrySet()) {
                    schedule.addActivity(e.getKey(), e.getValue());
                }
            }

            return schedule;
        }

        @Override
        public String findFreeSlot(Schedule schedule) {
            return "TEMP_SLOT"; // unused by the test
        }

        @Override
        public void assignActivityToSlot(Schedule schedule,
                                         String activityDescription,
                                         float durationHours) {
            // Track that interactor passed activity correctly
            assigned.add(activityDescription);

            // Put a test entry into the schedule
            schedule.addActivity("FREE_" + assigned.size(), activityDescription);
        }
    }
}
