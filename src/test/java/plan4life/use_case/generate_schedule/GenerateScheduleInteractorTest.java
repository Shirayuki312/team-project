package plan4life.use_case.generate_schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plan4life.ai.FixedEventInput;
import plan4life.ai.LlmScheduleService;
import plan4life.ai.ProposedEvent;
import plan4life.ai.RagRetriever;
import plan4life.ai.RagRetriever.RoutineExample;
import plan4life.ai.RoutineEventInput;
import plan4life.data_access.ScheduleDataAccessInterface;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.solver.ConstraintSolver;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenerateScheduleInteractorTest {

    private MockPresenter presenter;
    private TrackingRagRetriever ragRetriever;
    private StubLlmScheduleService llmService;
    private CapturingConstraintSolver solver;
    private InMemoryScheduleDAO scheduleDAO;
    private GenerateScheduleInteractor interactor;

    @BeforeEach
    void setup() {
        presenter = new MockPresenter();
        ragRetriever = new TrackingRagRetriever();
        llmService = new StubLlmScheduleService();
        solver = new CapturingConstraintSolver();
        scheduleDAO = new InMemoryScheduleDAO();
        interactor = new GenerateScheduleInteractor(presenter, ragRetriever, llmService, solver, scheduleDAO);
    }

    @Test
    void execute_usesRagAndSolverPipeline() {
        String fixed = "Mon 09:00-10:00 60 Gym";
        GenerateScheduleRequestModel request = new GenerateScheduleRequestModel(
                "Test routine", fixed, Collections.emptyList());

        interactor.execute(request);

        assertTrue(ragRetriever.invoked);
        assertEquals("Test routine", ragRetriever.lastSummary);
        assertEquals(1, llmService.receivedFixedEvents.size());
        assertEquals(DayOfWeek.MONDAY, llmService.receivedFixedEvents.get(0).getDay());
        assertTrue(solver.called);
        assertNotNull(presenter.lastResponse.getSchedule());
        assertEquals(scheduleDAO.savedSchedule, presenter.lastResponse.getSchedule());
        assertEquals("Some activities could not be placed and were left unassigned.\n" +
                        "Generated via Hugging Face model 'mock-model' (1 events parsed)",
                presenter.lastResponse.getMessage());
    }

    @Test
    void execute_rejectsEmptyInput() {
        GenerateScheduleRequestModel request = new GenerateScheduleRequestModel("   ", "  ", Collections.emptyList());

        interactor.execute(request);

        assertNull(presenter.lastResponse.getSchedule());
        assertEquals("Please describe your routine or add at least one fixed activity.", presenter.lastResponse.getMessage());
    }

    private static class MockPresenter implements GenerateScheduleOutputBoundary {
        GenerateScheduleResponseModel lastResponse;

        @Override
        public void present(GenerateScheduleResponseModel responseModel) {
            lastResponse = responseModel;
        }
    }

    private static class TrackingRagRetriever extends RagRetriever {
        boolean invoked;
        String lastSummary;

        @Override
        public List<RoutineExample> retrieveExamples(String routineSummary, int count) {
            invoked = true;
            lastSummary = routineSummary;
            return Collections.emptyList();
        }
    }

    private static class StubLlmScheduleService extends LlmScheduleService {
        List<FixedEventInput> receivedFixedEvents = new ArrayList<>();

        StubLlmScheduleService() {
            super(new plan4life.ai.PromptBuilder(new RagRetriever(false)));
        }

        @Override
        public List<ProposedEvent> proposeSchedule(String routineSummary,
                                                   List<RoutineEventInput> routineEvents,
                                                   List<FixedEventInput> fixedEvents,
                                                   List<RoutineExample> examples) {
            receivedFixedEvents = new ArrayList<>(fixedEvents);
            List<ProposedEvent> proposals = new ArrayList<>();
            proposals.add(new ProposedEvent(DayOfWeek.MONDAY, LocalTime.of(7, 0), 30, "Breakfast", true));
            return proposals;
        }

        @Override
        public LastCallInfo getLastCallInfo() {
            return LastCallInfo.liveModel("mock-model", receivedFixedEvents.size());
        }
    }

    private static class CapturingConstraintSolver extends ConstraintSolver {
        boolean called;

        @Override
        public Schedule solve(int scheduleId,
                              String scheduleType,
                              List<ProposedEvent> proposedEvents,
                              List<BlockedTime> blockedTimes) {
            called = true;
            Schedule schedule = super.solve(scheduleId, scheduleType, proposedEvents, blockedTimes);
            schedule.addUnplacedActivity("unplaced");
            return schedule;
        }
    }

    private static class InMemoryScheduleDAO implements ScheduleDataAccessInterface {
        Schedule savedSchedule;

        @Override
        public Schedule getSchedule(int id) {
            return savedSchedule;
        }

        @Override
        public void saveSchedule(Schedule schedule) {
            savedSchedule = schedule;
        }
    }
}