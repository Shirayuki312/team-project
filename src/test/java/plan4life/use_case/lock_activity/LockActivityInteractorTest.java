package plan4life.use_case.lock_activity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.data_access.InMemoryScheduleDAO;
import plan4life.entities.Activity;
import plan4life.entities.Schedule;
import plan4life.use_case.generate_schedule.ScheduleGenerationService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LockActivityInteractorTest {

    private InMemoryScheduleDAO dao;
    private MockLockPresenter presenter;
    private MockGenerator generator;
    private LockActivityInteractor interactor;
    private Schedule schedule;

    @BeforeEach
    void setup() {

        dao = new InMemoryScheduleDAO();
        presenter = new MockLockPresenter();
        generator = new MockGenerator();

        interactor = new LockActivityInteractor(presenter, dao, generator);

        schedule = new Schedule(1, "week");
        schedule.addActivity("Mon 9:00", "Workout");
        schedule.addActivity("Mon 10:00", "Study");
        dao.saveSchedule(schedule);
    }

    @Test
    void testLockSingleKey() {
        LockActivityRequestModel req =
                new LockActivityRequestModel(1, Set.of("Mon 9:00"));

        interactor.execute(req);

        Schedule updated = presenter.lastResponse.getUpdatedSchedule();
        assertTrue(updated.getLockedSlotKeys().contains("Mon 9:00"));
    }

    @Test
    void testLockMultipleKeys() {
        LockActivityRequestModel req =
                new LockActivityRequestModel(1, Set.of("Mon 9:00", "Mon 10:00"));

        interactor.execute(req);

        Schedule updated = presenter.lastResponse.getUpdatedSchedule();
        assertTrue(updated.getLockedSlotKeys().contains("Mon 9:00"));
        assertTrue(updated.getLockedSlotKeys().contains("Mon 10:00"));
        assertEquals(2, updated.getLockedSlotKeys().size());
    }

    @Test
    void testNullRequestDoesNothing() {
        interactor.execute(null);

        assertNull(presenter.lastResponse);
    }

    @Test
    void testNewScheduleCreatedIfMissing() {
        LockActivityRequestModel req =
                new LockActivityRequestModel(99, Set.of("Mon 3:00"));

        interactor.execute(req);

        Schedule updated = presenter.lastResponse.getUpdatedSchedule();
        assertEquals(99, updated.getScheduleId());
    }

    // -------------------
    // Mocks
    // -------------------
    private static class MockLockPresenter implements LockActivityOutputBoundary {
        LockActivityResponseModel lastResponse;

        @Override
        public void present(LockActivityResponseModel response) {
            lastResponse = response;
        }
    }

    private static class MockGenerator implements ScheduleGenerationService {

        @Override
        public Schedule generate(String description, Map<String, String> fixedActivities) {
            return new Schedule(999, "week");
        }

        @Override
        public String findFreeSlot(Schedule schedule) {
            return null;
        }

        @Override
        public void assignActivityToSlot(Schedule schedule, String desc, float dur) { //default input is empty
        }
    }
}

