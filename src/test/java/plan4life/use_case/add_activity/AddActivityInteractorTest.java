package plan4life.use_case.add_activity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.entities.Activity;
import plan4life.entities.Schedule;
import plan4life.data_access.ScheduleDataAccessInterface;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddActivityInteractorTest {

    private Schedule testSchedule;
    private AddActivityInteractor interactor;
    private MockAddActivityPresenter presenter;
    private ScheduleDataAccessInterface scheduleDAO;

    @BeforeEach
    void setUp() {
        // Simple in-memory DAO for testing
        testSchedule = new Schedule(1, "day");
        Map<Integer, Schedule> schedules = new HashMap<>();
        schedules.put(testSchedule.getScheduleId(), testSchedule);

        scheduleDAO = new ScheduleDataAccessInterface() {
            @Override
            public Schedule getSchedule(int scheduleId) {
                return schedules.get(scheduleId);
            }

            @Override
            public void saveSchedule(Schedule schedule) {
                schedules.put(schedule.getScheduleId(), schedule);
            }
        };

        presenter = new MockAddActivityPresenter();
        interactor = new AddActivityInteractor(scheduleDAO, presenter);
    }

    @Test
    void testAddValidActivity() {
        AddActivityRequestModel request = new AddActivityRequestModel(1, "Workout", 1.5f);
        AddActivityResponseModel response = interactor.execute(request);

        // Interactor response
        assertTrue(response.isSuccess());
        assertEquals("Activity successfully added.", response.getMessage());

        // Presenter was called with the same response
        assertEquals(response, presenter.getLastResponse());

        // Schedule state
        assertEquals(1, testSchedule.getTasks().size());
        Activity added = testSchedule.getTasks().get(0);
        assertEquals("Workout", added.getDescription());
        assertEquals(1.5f, added.getDuration());
    }

    @Test
    void testDeleteActivity() {
        Activity activity = new Activity("Workout", 1.5f);
        testSchedule.addTask(activity);
        assertEquals(1, testSchedule.getTasks().size());

        testSchedule.removeTask(activity);

        assertTrue(testSchedule.getTasks().isEmpty());
    }

    @Test
    void testInvalidDurationZero() {
        AddActivityRequestModel request = new AddActivityRequestModel(1, "Workout", 0f);
        AddActivityResponseModel response = interactor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid activity duration.", response.getMessage());
        assertEquals(response, presenter.getLastResponse());
        assertTrue(testSchedule.getTasks().isEmpty());
    }

    @Test
    void testInvalidDurationNegative() {
        AddActivityRequestModel request = new AddActivityRequestModel(1, "Workout", -2f);
        AddActivityResponseModel response = interactor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid activity duration.", response.getMessage());
        assertEquals(response, presenter.getLastResponse());
        assertTrue(testSchedule.getTasks().isEmpty());
    }

    @Test
    void testScheduleNotFound() {
        AddActivityRequestModel request = new AddActivityRequestModel(999, "Workout", 1f);
        AddActivityResponseModel response = interactor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("Schedule not found.", response.getMessage());
        assertEquals(response, presenter.getLastResponse());
    }

    @Test
    void testMultipleActivities() {
        AddActivityRequestModel req1 = new AddActivityRequestModel(1, "Workout", 1f);
        AddActivityRequestModel req2 = new AddActivityRequestModel(1, "Study", 2f);

        interactor.execute(req1);
        AddActivityResponseModel response2 = interactor.execute(req2);

        // Ensure both activities are added
        assertEquals(2, testSchedule.getTasks().size());
        assertEquals("Workout", testSchedule.getTasks().get(0).getDescription());
        assertEquals("Study", testSchedule.getTasks().get(1).getDescription());

        // Presenter was called with last response
        assertEquals(response2, presenter.getLastResponse());
    }
}