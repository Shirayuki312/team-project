package plan4life.use_case.block_off_time;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.data_access.ScheduleDataAccessInterface;
import plan4life.entities.ScheduledBlock;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockOffTimeInteractorTest {

    private ScheduleDataAccessInterface fakeDAO;
    private MockBlockOffTimePresenter presenter;
    private BlockOffTimeInteractor interactor;
    private Schedule testSchedule;

    @BeforeEach
    void setUp() {
        // Create a test schedule
        testSchedule = new Schedule(1, "week");

        // Fake DAO implementation
        fakeDAO = new ScheduleDataAccessInterface() {
            private final HashMap<Integer, Schedule> schedules = new HashMap<>();
            {
                schedules.put(1, testSchedule);
            }

            @Override
            public Schedule getSchedule(int scheduleId) {
                return schedules.get(scheduleId);
            }

            @Override
            public void saveSchedule(Schedule schedule) {
                schedules.put(schedule.getId(), schedule);
            }
        };

        presenter = new MockBlockOffTimePresenter();
        interactor = new BlockOffTimeInteractor(fakeDAO, presenter);
    }

    @Test
    void testSuccessfulBlock() {
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 1, 11, 0);

        BlockOffTimeRequestModel request = new BlockOffTimeRequestModel(1, start, end, "Meeting", 0);
        BlockOffTimeResponseModel response = interactor.execute(request);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getUpdatedBlockedTimes().size());
        assertEquals("Meeting", response.getUpdatedBlockedTimes().get(0).getDescription());
        assertSame(testSchedule, response.getUpdatedSchedule());
    }

    @Test
    void testInvalidTimeRange() {
        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 1, 11, 0);

        BlockOffTimeRequestModel request = new BlockOffTimeRequestModel(1, start, end, "Invalid", 0);
        BlockOffTimeResponseModel response = interactor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid time range.", response.getMessage());
        assertTrue(response.getUpdatedBlockedTimes().isEmpty());
    }

    @Test
    void testOverlappingBlock() {
        BlockedTime existing = new BlockedTime(
                LocalDateTime.of(2025, 12, 1, 10, 0),
                LocalDateTime.of(2025, 12, 1, 11, 0),
                "Existing",
                0
        );
        testSchedule.addBlockedTime(existing);

        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 10, 30);
        LocalDateTime end = LocalDateTime.of(2025, 12, 1, 11, 30);
        BlockOffTimeRequestModel request = new BlockOffTimeRequestModel(1, start, end, "Overlap", 0);

        BlockOffTimeResponseModel response = interactor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("The selected time overlaps with an existing blocked period.", response.getMessage());

        // check the schedule itself, not response list
        assertEquals(1, testSchedule.getBlockedTimes().size());
        assertEquals("Existing", testSchedule.getBlockedTimes().get(0).getDescription());
    }

    @Test
    void testScheduleNotFound() {
        BlockOffTimeRequestModel request = new BlockOffTimeRequestModel(999,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                "No schedule",
                0);

        BlockOffTimeResponseModel response = interactor.execute(request);

        assertFalse(response.isSuccess());
        assertEquals("Schedule not found.", response.getMessage());
        assertNull(response.getUpdatedSchedule());
    }

    @Test
    void testOverlappingActivitiesRemovedOnSuccess() {
        // Add an unlocked block that overlaps with the new blocked time
        ScheduledBlock overlappingBlock = new ScheduledBlock(
                LocalDateTime.of(2025, 12, 1, 10, 0),
                LocalDateTime.of(2025, 12, 1, 11, 0),
                "OldActivity",
                false, // unlocked
                0
        );
        testSchedule.addUnlockedBlockForTest(overlappingBlock);

        LocalDateTime start = LocalDateTime.of(2025, 12, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 1, 11, 0);

        BlockOffTimeRequestModel request = new BlockOffTimeRequestModel(1, start, end, "Meeting", 0);
        BlockOffTimeResponseModel response = interactor.execute(request);

        assertTrue(response.isSuccess());
        // overlapping unlocked block should be removed
        assertTrue(testSchedule.getUnlockedBlocks().isEmpty(), "Overlapping unlocked blocks should be removed");
        assertEquals(1, testSchedule.getBlockedTimes().size());
        assertEquals("Meeting", testSchedule.getBlockedTimes().get(0).getDescription());
    }
}