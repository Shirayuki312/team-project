package plan4life.use_case.set_reminder;

import org.junit.jupiter.api.Test;
import plan4life.data_access.ReminderDataAccessInterface;
import plan4life.entities.Reminder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SetReminderInteractor.
 *
 * This class also defines small in-memory fake implementations of
 * - ReminderDataAccessInterface (InMemoryReminderDAO)
 * - SetReminderOutputBoundary (CollectingPresenter)
 */
class SetReminderInteractorTest {

    // ---------------------------------------------------------------------
    // Fake DAO & Presenter used by all tests
    // ---------------------------------------------------------------------

    /**
     * Simple in-memory DAO implementation for testing.
     */
    static class InMemoryReminderDAO implements ReminderDataAccessInterface {
        final Map<String, Reminder> storage = new HashMap<>();

        @Override
        public void saveReminder(Reminder reminder) {
            storage.put(reminder.getId(), reminder);
        }

        @Override
        public void deleteReminder(String id) {
            storage.remove(id);
        }

        @Override
        public Reminder getReminder(String id) {
            return storage.get(id);
        }

        @Override
        public List<Reminder> getAllReminders() {
            return new ArrayList<>(storage.values());
        }
    }

    /**
     * Simple presenter that just stores the last response model
     * for each type of callback.
     */
    static class CollectingPresenter implements SetReminderOutputBoundary {
        SetReminderResponseModel lastScheduled;
        SetReminderResponseModel lastFired;
        SetReminderResponseModel lastCancelled;

        boolean scheduledCalled = false;
        boolean firedCalled = false;
        boolean cancelledCalled = false;

        @Override
        public void presentReminderScheduled(SetReminderResponseModel responseModel) {
            scheduledCalled = true;
            lastScheduled = responseModel;
        }

        @Override
        public void presentReminderFired(SetReminderResponseModel responseModel) {
            firedCalled = true;
            lastFired = responseModel;
        }

        @Override
        public void presentReminderCancelled(SetReminderResponseModel responseModel) {
            cancelledCalled = true;
            lastCancelled = responseModel;
        }
    }

    // ---------------------------------------------------------------------
    // 1) When reminder time is in the past -> fire immediately
    // ---------------------------------------------------------------------

    @Test
    void testSetReminderImmediateFireWhenTimeInPast() {
        InMemoryReminderDAO dao = new InMemoryReminderDAO();
        CollectingPresenter presenter = new CollectingPresenter();
        SetReminderInteractor interactor = new SetReminderInteractor(dao, presenter);

        // Start 1 minute in the past, so reminderTime = now - 1 minute
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        LocalDateTime end = start.plusHours(1);

        SetReminderRequestModel request = new SetReminderRequestModel(
                "Past Event",
                start,
                end,
                1,                    // minutesBefore
                "Message only",
                "MEDIUM",
                true,                 // sendMessage
                false,                // sendEmail
                true,                 // playSound
                true                  // important
        );

        interactor.setReminder(request);

        // DAO should have exactly one reminder stored
        assertEquals(1, dao.storage.size());
        Reminder stored = dao.storage.values().iterator().next();
        assertEquals("Past Event", stored.getTitle());

        // Presenter should have both scheduled + fired populated
        assertTrue(presenter.scheduledCalled);
        assertTrue(presenter.firedCalled);
        assertNotNull(presenter.lastScheduled);
        assertNotNull(presenter.lastFired);

        // Basic sanity check on response fields
        assertEquals("Past Event", presenter.lastScheduled.getTitle());
        assertEquals("Past Event", presenter.lastFired.getTitle());
    }

    // ---------------------------------------------------------------------
    // 2) When reminder time is in the future -> schedule Timer & fire later
    // ---------------------------------------------------------------------

    @Test
    void testSetReminderSchedulesTimerAndFiresLater() throws InterruptedException {
        InMemoryReminderDAO dao = new InMemoryReminderDAO();
        CollectingPresenter presenter = new CollectingPresenter();
        SetReminderInteractor interactor = new SetReminderInteractor(dao, presenter);

        // Start 1 second in the future so delayMillis > 0
        LocalDateTime start = LocalDateTime.now().plusSeconds(1);
        LocalDateTime end = start.plusHours(1);

        SetReminderRequestModel request = new SetReminderRequestModel(
                "Future Event",
                start,
                end,
                0,                    // minutesBefore -> reminderTime == start
                "Message with sound",
                "HIGH",
                true,
                true,
                true,
                true
        );

        interactor.setReminder(request);

        // Immediately after scheduling, only "scheduled" should be called
        assertTrue(presenter.scheduledCalled);
        assertFalse(presenter.firedCalled);

        // Wait long enough for the timer to fire
        Thread.sleep(1500);

        assertTrue(presenter.firedCalled);
        assertNotNull(presenter.lastFired);
        assertEquals("Future Event", presenter.lastFired.getTitle());
    }

    // ---------------------------------------------------------------------
    // 3) Cancel reminder when timer exists
    // ---------------------------------------------------------------------

    @Test
    void testCancelReminderDeletesAndStopsTimer() {
        InMemoryReminderDAO dao = new InMemoryReminderDAO();
        CollectingPresenter presenter = new CollectingPresenter();
        SetReminderInteractor interactor = new SetReminderInteractor(dao, presenter);

        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        LocalDateTime end = start.plusHours(1);

        SetReminderRequestModel request = new SetReminderRequestModel(
                "Cancelable Event",
                start,
                end,
                5,
                "Sound only",
                "LOW",
                false,
                false,
                true,
                true
        );

        // First schedule a reminder
        interactor.setReminder(request);
        assertEquals(1, dao.storage.size());

        // Then cancel it
        interactor.cancelReminder(request);

        assertEquals(0, dao.storage.size());
        assertTrue(presenter.cancelledCalled);
        assertNotNull(presenter.lastCancelled);
        assertEquals("Cancelable Event", presenter.lastCancelled.getTitle());
        assertFalse(presenter.lastCancelled.isImportant());
    }

    // ---------------------------------------------------------------------
    // 4) Cancel reminder when NO timer exists
    //    (should still call DAO.deleteReminder & presenter)
    // ---------------------------------------------------------------------

    @Test
    void testCancelReminderWhenNoExistingTimer() {
        InMemoryReminderDAO dao = new InMemoryReminderDAO();
        CollectingPresenter presenter = new CollectingPresenter();
        SetReminderInteractor interactor = new SetReminderInteractor(dao, presenter);

        LocalDateTime start = LocalDateTime.now().plusMinutes(30);
        LocalDateTime end = start.plusHours(1);

        // We never called setReminder(...) for this request,
        // so there is no timer in the interactor's internal map.
        SetReminderRequestModel request = new SetReminderRequestModel(
                "NoTimerEvent",
                start,
                end,
                10,
                "Message only",
                "MEDIUM",
                false,
                true,
                false,
                true
        );

        // Cancel should still call DAO.deleteReminder + presenter
        interactor.cancelReminder(request);

        // DAO has no record (delete on empty map is fine)
        assertTrue(dao.storage.isEmpty());

        // Presenter should have been notified
        assertTrue(presenter.cancelledCalled);
        assertNotNull(presenter.lastCancelled);
        assertEquals("NoTimerEvent", presenter.lastCancelled.getTitle());
    }

    // ---------------------------------------------------------------------
    // 5) Direct test for SetReminderResponseModel getters
    //    to ensure 100% coverage on that class.
    // ---------------------------------------------------------------------

    @Test
    void testResponseModelGetters() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = start.plusHours(2);
        LocalDateTime reminderTime = start.minusMinutes(15);

        Reminder reminder = new Reminder(
                "R-1",
                "Getter Event",
                start,
                end,
                reminderTime,
                15,
                "Message with sound",
                "HIGH",
                true,   // sendMessage
                false,  // sendEmail
                true,   // playSound
                true    // important
        );

        SetReminderResponseModel resp = SetReminderResponseModel.fromEntity(reminder);

        // Call every getter at least once
        assertEquals("Getter Event", resp.getTitle());
        assertEquals(start, resp.getStart());
        assertEquals(end, resp.getEnd());
        assertEquals(reminderTime, resp.getReminderTime());
        assertEquals(15, resp.getMinutesBefore());
        assertEquals("Message with sound", resp.getAlertType());
        assertEquals("HIGH", resp.getUrgencyLevel());
        assertTrue(resp.isSendMessage());
        assertFalse(resp.isSendEmail());
        assertTrue(resp.isPlaySound());
        assertTrue(resp.isImportant());
    }
}
