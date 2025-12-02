package plan4life.use_case.generate_schedule;

import plan4life.ai.FixedEventInput;
import plan4life.ai.LlmScheduleService;
import plan4life.ai.ProposedEvent;
import plan4life.ai.RagRetriever;
import plan4life.ai.RoutineEventInput;
import plan4life.ai.TimeFormats;
import plan4life.data_access.ScheduleDataAccessInterface;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateScheduleInteractor implements GenerateScheduleInputBoundary {
    private static final int EXAMPLE_COUNT = 2;
    private static final Pattern FIXED_EVENT_PATTERN = Pattern.compile(
            "(?i)^(mon|monday|tue|tuesday|wed|wednesday|thu|thursday|fri|friday|sat|saturday|sun|sunday)\\s+" +
                    "(\\d{1,2}:\\d{2})(?:\\s*-\\s*(\\d{1,2}:\\d{2}))?(?:\\s+(\\d+))?\\s+(.+)$");

    private final GenerateScheduleOutputBoundary presenter;
    private final RagRetriever ragRetriever;
    private final LlmScheduleService llmScheduleService;
    private final ConstraintSolver constraintSolver;
    private final ScheduleDataAccessInterface scheduleDAO;

    public GenerateScheduleInteractor(GenerateScheduleOutputBoundary presenter,
                                      RagRetriever ragRetriever,
                                      LlmScheduleService llmScheduleService,
                                      ConstraintSolver constraintSolver,
                                      ScheduleDataAccessInterface scheduleDAO) {
        this.presenter = Objects.requireNonNull(presenter);
        this.ragRetriever = Objects.requireNonNull(ragRetriever);
        this.llmScheduleService = Objects.requireNonNull(llmScheduleService);
        this.constraintSolver = Objects.requireNonNull(constraintSolver);
        this.scheduleDAO = Objects.requireNonNull(scheduleDAO);
    }

    @Override
    public void execute(GenerateScheduleRequestModel requestModel) {
        if (requestModel == null) {
            presenter.present(new GenerateScheduleResponseModel(new Schedule()));
            return;
        }

        String routineSummary = requestModel.getRoutineDescription();
        List<FixedEventInput> fixedEvents = parseFixedEvents(requestModel.getFixedActivities());
        List<RoutineEventInput> routineEvents = Collections.emptyList();

        System.out.printf("[GenerateScheduleInteractor] fixed events parsed: %d%n", fixedEvents.size());

        if ((routineSummary == null || routineSummary.isBlank()) && fixedEvents.isEmpty()) {
            presenter.present(new GenerateScheduleResponseModel(null,
                    "Please describe your routine or add at least one fixed activity."));
            return;
        }

        try {
            List<RagRetriever.RoutineExample> examples = ragRetriever.retrieveExamples(routineSummary, EXAMPLE_COUNT);
            List<ProposedEvent> proposals = llmScheduleService.proposeSchedule(routineSummary, routineEvents, fixedEvents, examples);

            System.out.printf("[GenerateScheduleInteractor] proposals returned: %d%n", proposals == null ? 0 : proposals.size());
            LlmScheduleService.LastCallInfo lastCall = llmScheduleService.getLastCallInfo();
            System.out.printf("[GenerateScheduleInteractor] generation mode: %s%n",
                    lastCall != null && lastCall.usedLiveModel() ? "live AI" : "fallback / heuristic");

            Schedule schedule = constraintSolver.solve(2, "week", proposals, Collections.emptyList());
            scheduleDAO.saveSchedule(schedule);
            presenter.present(new GenerateScheduleResponseModel(schedule,
                    buildGenerationMessage(schedule, llmScheduleService.getLastCallInfo())));
        } catch (Exception ex) {
            presenter.present(new GenerateScheduleResponseModel(null,
                    "Unable to generate a schedule right now. Please try again."));
        }
    }

    private String buildGenerationMessage(Schedule schedule, LlmScheduleService.LastCallInfo lastCallInfo) {
        if (schedule == null) {
            return "No plan was generated.";
        }
        boolean hasActivities = !schedule.getActivities().isEmpty();
        boolean hasPlacedBlocks = !schedule.getLockedBlocks().isEmpty() || !schedule.getUnlockedBlocks().isEmpty();
        if (!hasActivities && !hasPlacedBlocks) {
            return appendSource("No plan could be generated for the provided details.", lastCallInfo);
        }
        if (!schedule.getUnplacedActivities().isEmpty()) {
            return appendSource("Some activities could not be placed and were left unassigned.", lastCallInfo);
        }
        return appendSource(null, lastCallInfo);
    }

    private String appendSource(String base, LlmScheduleService.LastCallInfo lastCallInfo) {
        if (lastCallInfo == null) {
            return base;
        }
        String source = lastCallInfo.asUserMessage();
        if (base == null || base.isBlank()) {
            return source;
        }
        return base + "\n" + source;
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

        LocalTime start = LocalTime.parse(matcher.group(2), TimeFormats.OPTIONAL_SECONDS);
        LocalTime end = matcher.group(3) == null ? null : LocalTime.parse(matcher.group(3), TimeFormats.OPTIONAL_SECONDS);
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