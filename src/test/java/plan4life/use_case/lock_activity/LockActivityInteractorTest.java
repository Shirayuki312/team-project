package plan4life.use_case.lock_activity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.data_access.InMemoryScheduleDAO;
import plan4life.entities.Schedule;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LockActivityInteractorTest {

    private InMemoryScheduleDAO dao;
    private MockLockPresenter presenter;
    private LockActivityInteractor interactor;
    private Schedule schedule;

    @BeforeEach
    void setup() {

        dao = new InMemoryScheduleDAO();
        presenter = new MockLockPresenter();

        interactor = new LockActivityInteractor(presenter, dao);

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
    void testUnlockingWorks() {
        schedule.lockSlotKey("Mon 9:00");
        schedule.lockSlotKey("Mon 10:00");
        dao.saveSchedule(schedule);

        LockActivityRequestModel req =
                new LockActivityRequestModel(1, Set.of("Mon 9:00"));

        interactor.execute(req);

        Schedule updated = presenter.lastResponse.getUpdatedSchedule();

        assertFalse(updated.getLockedSlotKeys().contains("Mon 9:00"));
        assertTrue(updated.getLockedSlotKeys().contains("Mon 10:00"));
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
    // Mock Presenter
    // -------------------
    private static class MockLockPresenter implements LockActivityOutputBoundary {
        LockActivityResponseModel lastResponse;

        @Override
        public void present(LockActivityResponseModel response) {
            lastResponse = response;
        }
    }
}
