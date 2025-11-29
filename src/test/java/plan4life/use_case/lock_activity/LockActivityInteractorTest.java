package plan4life.use_case.lock_activity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.data_access.InMemoryScheduleDAO;
import plan4life.entities.Schedule;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LockActivityInteractorTest {

    private InMemoryScheduleDAO dao;
    private MockLockActivityPresenter presenter;
    private LockActivityInteractor interactor;
    private Schedule schedule;

    @BeforeEach
    void setup() {

        dao = new InMemoryScheduleDAO();
        presenter = new MockLockActivityPresenter();
        interactor = new LockActivityInteractor(presenter, dao);

        schedule = new Schedule(1, "day");
        schedule.addActivity("Mon 9:00", "Workout");
        schedule.addActivity("Mon 10:00", "Study");
        dao.saveSchedule(schedule);
    }

    @Test
    void testLockSingleTimeKey() {
        LockActivityRequestModel request =
                new LockActivityRequestModel(1, Set.of("Mon 9:00"));

        interactor.execute(request);

        LockActivityResponseModel response = presenter.getLastResponse();
        assertNotNull(response);

        Schedule updated = response.getUpdatedSchedule();
        assertTrue(updated.getLockedSlotKeys().contains("Mon 9:00"));
        assertEquals(1, updated.getScheduleId());
    }

    @Test
    void testLockMultipleTimeKeys() {
        LockActivityRequestModel request =
                new LockActivityRequestModel(1, Set.of("Mon 9:00", "Mon 10:00"));

        interactor.execute(request);

        Schedule updated = presenter.getLastResponse().getUpdatedSchedule();

        assertTrue(updated.getLockedSlotKeys().contains("Mon 9:00"));
        assertTrue(updated.getLockedSlotKeys().contains("Mon 10:00"));
        assertEquals(2, updated.getLockedSlotKeys().size());
    }

    @Test
    void testUnlockingKeys() {
        // Pre-lock keys
        schedule.lockSlotKey("Mon 9:00");
        schedule.lockSlotKey("Mon 10:00");
        dao.saveSchedule(schedule);

        // Now send empty set → unlock everything
        LockActivityRequestModel request =
                new LockActivityRequestModel(1, Set.of());

        interactor.execute(request);

        Schedule updated = presenter.getLastResponse().getUpdatedSchedule();
        assertTrue(updated.getLockedSlotKeys().isEmpty());
    }

    @Test
    void testNullRequestDoesNotCrash() {
        interactor.execute(null);

        // For null request, interactor should not call presenter at all
        assertNull(presenter.getLastResponse());
    }

}
