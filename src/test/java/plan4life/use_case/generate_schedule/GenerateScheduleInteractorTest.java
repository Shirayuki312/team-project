package plan4life.use_case.generate_schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.entities.Schedule;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenerateScheduleInteractorTest {

    private MockPresenter presenter;
    private MockGenerationService mockService;
    private GenerateScheduleInteractor interactor;

    @BeforeEach
    void setup() {
        presenter = new MockPresenter();
        mockService = new MockGenerationService();
        interactor = new GenerateScheduleInteractor(presenter, mockService);
    }

    // ------------------------------------------------------------
    // 1. Interactor Tests
    // ------------------------------------------------------------
    @Test
    void testGenerateSchedule_basic() {
        Map<String, String> fixed = new HashMap<>();
        fixed.put("7:00-8:00", "Breakfast");
        fixed.put("12:00-13:00", "Lunch");

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

        // Mock generator always adds this
        assertEquals("MOCK_ACTIVITY", schedule.getActivities().get("09:00"));

        // Fixed preserved
        assertEquals("Breakfast", schedule.getActivities().get("7:00-8:00"));
        assertEquals("Lunch", schedule.getActivities().get("12:00-13:00"));

        // Free activities passed correctly
        assertTrue(mockService.assigned.contains("Jogging"));
        assertTrue(mockService.assigned.contains("Study"));

        assertEquals("Study, Gym, Relax", mockService.lastDescription);
        assertEquals(fixed, mockService.lastFixed);
    }

    @Test
    void testGenerateSchedule_nullRequest() {
        interactor.execute(null);

        assertTrue(presenter.wasCalled);
        assertNotNull(presenter.lastResponse);

        Schedule s = presenter.lastResponse.getSchedule();
        assertNotNull(s);
        assertTrue(s.getActivities().isEmpty());
    }

    // ------------------------------------------------------------
    // 2. Mock Service Tests
    // ------------------------------------------------------------
    @Test
    void testGenerateCreatesSchedule() {
        Map<String, String> fixed = Map.of("10:00", "Meeting");

        Schedule s = mockService.generate("Routine text", fixed);

        assertNotNull(s);
        assertEquals("Meeting", s.getActivities().get("10:00"));
        assertEquals("Routine text", mockService.lastDescription);
        assertEquals(fixed, mockService.lastFixed);
    }

    @Test
    void testFindFreeSlotDoesNotThrow() {
        Schedule schedule = new Schedule();
        assertDoesNotThrow(() -> mockService.findFreeSlot(schedule));
    }

    @Test
    void testAssignActivityDoesNotCrash() {
        Schedule s = new Schedule();
        assertDoesNotThrow(() ->
                mockService.assignActivityToSlot(s, "Study", 1.0f)
        );
    }

    @Test
    void testAssignActivityActuallyAddsSomething() {
        Schedule s = new Schedule();

        mockService.assignActivityToSlot(s, "Study", 1.0f);

        assertTrue(
                s.getActivities().values().stream().anyMatch(v -> v.contains("Study"))
        );
    }

    // ------------------------------------------------------------
    // Mock Classes
    // ------------------------------------------------------------

    private static class MockPresenter implements GenerateScheduleOutputBoundary {
        boolean wasCalled = false;
        GenerateScheduleResponseModel lastResponse;

        @Override
        public void present(GenerateScheduleResponseModel responseModel) {
            wasCalled = true;
            lastResponse = responseModel;
        }
    }

    private static class MockGenerationService implements ScheduleGenerationService {

        String lastDescription;
        Map<String,String> lastFixed;
        List<String> assigned = new ArrayList<>();

        @Override
        public Schedule generate(String routineDescription,
                                 Map<String, String> fixedActivities) {

            lastDescription = routineDescription;
            lastFixed = fixedActivities;

            Schedule schedule = new Schedule();

            // MUST use addActivity — cannot modify getActivities()
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
            return "FREE_SLOT_TEST";
        }

        @Override
        public void assignActivityToSlot(Schedule schedule,
                                         String activityDescription,
                                         float durationHours) {
            assigned.add(activityDescription);

            // Simulate adding to schedule
            schedule.addActivity("FREE_" + assigned.size(), activityDescription);
        }
    }

}
