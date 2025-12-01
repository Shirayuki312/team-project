package plan4life.use_case.generate_schedule;

import plan4life.ai.FixedEventInput;
import plan4life.ai.LlmScheduleService;
import plan4life.ai.ProposedEvent;
import plan4life.ai.RagRetriever;
import plan4life.ai.RoutineEventInput;
import plan4life.entities.Schedule;
import plan4life.solver.ConstraintSolver;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateScheduleInteractor implements GenerateScheduleInputBoundary {
    private final GenerateScheduleOutputBoundary presenter;
    private final RagRetriever ragRetriever;
    private final LlmScheduleService llmScheduleService;
    private final ConstraintSolver constraintSolver;

    private static final int EXAMPLE_COUNT = 2;
    private static final Pattern FIXED_EVENT_PATTERN = Pattern.compile(
            "(?i)^(mon|monday|tue|tuesday|wed|wednesday|thu|thursday|fri|friday|sat|saturday|sun|sunday)\\s+" +
                    "(\\d{1,2}:\\d{2})(?:\\s*-\\s*(\\d{1,2}:\\d{2}))?(?:\\s+(\\d+))?\\s+(.+)$");

    public GenerateScheduleInteractor(GenerateScheduleOutputBoundary presenter,
                                      RagRetriever ragRetriever,
                                      LlmScheduleService llmScheduleService,
                                      ConstraintSolver constraintSolver) {
        this.presenter = presenter;
        this.ragRetriever = ragRetriever;
        this.llmScheduleService = llmScheduleService;
        this.constraintSolver = constraintSolver;
    }

    @Override
    public void execute(GenerateScheduleRequestModel requestModel) {
        String routineSummary = requestModel.getRoutineDescription();
        List<FixedEventInput> fixedEvents = parseFixedEvents(requestModel.getFixedActivities());
        List<RoutineEventInput> routineEvents = Collections.emptyList();

        if ((routineSummary == null || routineSummary.isBlank()) && fixedEvents.isEmpty()) {
            presenter.present(new GenerateScheduleResponseModel(null,
                    "Please describe your routine or add at least one fixed activity."));
            return;
        }

        try {
            List<RagRetriever.RoutineExample> examples = ragRetriever.retrieveExamples(routineSummary, EXAMPLE_COUNT);
            List<ProposedEvent> proposals = llmScheduleService.proposeSchedule(routineSummary, routineEvents, fixedEvents, examples);

            Schedule schedule = constraintSolver.solve(2, "week", proposals, Collections.emptyList());
            presenter.present(new GenerateScheduleResponseModel(schedule, buildGenerationMessage(schedule)));
        } catch (Exception ex) {
            presenter.present(new GenerateScheduleResponseModel(null,
                    "Unable to generate a schedule right now. Please try again."));
        }
    }

    private String buildGenerationMessage(Schedule schedule) {
        if (schedule == null) {
            return "No plan was generated.";
        }
        boolean hasActivities = !schedule.getActivities().isEmpty();
        boolean hasPlacedBlocks = !schedule.getLockedBlocks().isEmpty() || !schedule.getUnlockedBlocks().isEmpty();
        if (!hasActivities && !hasPlacedBlocks) {
            return "No plan could be generated for the provided details.";
        }
        if (!schedule.getUnplacedActivities().isEmpty()) {
            return "Some activities could not be placed and were left unassigned.";
        }
        return null;
    }

    private List<FixedEventInput> parseFixedEvents(String fixedActivities) {
        if (fixedActivities == null || fixedActivities.isBlank()) {
            return Collections.emptyList();
        }

        String[] lines = fixedActivities.split("[\\n;]");
        List<FixedEventInput> results = new ArrayList<>();
        for (String rawLine : lines) {
            parseFixedEvent(rawLine.trim()).ifPresent(results::add);
        }
        return results;
    }

    private Optional<FixedEventInput> parseFixedEvent(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = FIXED_EVENT_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        DayOfWeek day = parseDay(matcher.group(1));
        if (day == null) {
            return Optional.empty();
        }

        LocalTime start = LocalTime.parse(matcher.group(2));
        LocalTime end = matcher.group(3) == null ? null : LocalTime.parse(matcher.group(3));
        int durationMinutes = parseDuration(start, end, matcher.group(4));
        String name = matcher.group(5).trim();

        if (name.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new FixedEventInput(day, start, durationMinutes, name, true));
    }

    private DayOfWeek parseDay(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private int parseDuration(LocalTime start, LocalTime end, String durationGroup) {
        if (durationGroup != null && !durationGroup.isBlank()) {
            try {
                int parsed = Integer.parseInt(durationGroup.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        if (end != null) {
            long between = Duration.between(start, end).toMinutes();
            if (between > 0) {
                return (int) between;
            }
        }
        return 60;
    }
}