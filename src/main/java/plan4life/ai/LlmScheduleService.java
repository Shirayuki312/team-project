package plan4life.ai;

import java.util.List;

/**
 * Contract for generating a schedule proposal using an LLM (or a mock implementation).
 */
public interface LlmScheduleService {
    List<ProposedEvent> proposeSchedule(List<RoutineEventInput> routineEvents,
                                        List<FixedEventInput> fixedEvents);
}