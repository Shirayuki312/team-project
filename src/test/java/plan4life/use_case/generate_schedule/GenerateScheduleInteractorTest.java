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

        // NEW FORMAT:
        // key   = activity description
        // value = "dayIndex:startHour:duration"
        Map<String, String> fixed = new HashMap<>();
        fixed.put("Breakfast", "1:7:1");  // Tuesday at 7:00 for 1h
        fixed.put("Lunch", "3:12:1");     // Thursday at 12:00 for 1h

        // NEW free-activity format: "Desc:Duration"
        List<String> free = Arrays.asList(
                "Jogging:1.0",
                "Study:2"
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

        // The mock puts a predictable test value
        assertEquals("MOCK_ACTIVITY", schedule.getActivities().get("0:9"));

        // Fixed activities added properly
        assertEquals("Breakfast", schedule.getActivities().get("1:7"));
        assertEquals("Lunch", schedule.getActivities().get("3:12"));

        // Free activities recorded by mock assignActivityToSlot
        assertTrue(generationService.assigned.contains("Jogging"));
        assertTrue(generationService.assigned.contains("Study"));

        // Interactor passed through correct parameters
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

        // Null request → empty schedule
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

            // Add mock base entry using correct key format "day:hour"
            schedule.addActivity("0:9", "MOCK_ACTIVITY");

            // Add raw fixed activities WITH correct decoding
            if (fixedActivities != null) {
                for (var e : fixedActivities.entrySet()) {
                    String activity = e.getKey();
                    String encoded = e.getValue(); // "day:start:duration"

                    String[] p = encoded.split(":");
                    int day = Integer.parseInt(p[0].trim());
                    int hour = Integer.parseInt(p[1].trim());

                    schedule.addActivity(day + ":" + hour, activity);
                }
            }

            return schedule;
        }

        @Override
        public String findFreeSlot(Schedule schedule) {
            return "0:10"; // not used by test but valid format
        }

        @Override
        public void assignActivityToSlot(Schedule schedule,
                                         String activityDescription,
                                         float durationHours) {

            assigned.add(activityDescription);

            // Insert with auto-generated key just for testing
            String key = "FREE:" + assigned.size();
            schedule.addActivity(key, activityDescription);
        }
    }
}
