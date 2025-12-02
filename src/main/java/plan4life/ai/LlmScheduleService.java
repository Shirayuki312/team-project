package plan4life.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates a schedule proposal using a Hugging Face model.
 * Falls back to a deterministic schedule when no model is available.
 */
public class LlmScheduleService {

    private static final String DEFAULT_MODEL = "meta-llama/Llama-3.1-8B-Instruct";
    private static final String HF_ROUTER_URL = "https://router.huggingface.co/v1/chat/completions";
    private static final int EXAMPLE_COUNT = 2;
    private static final int EARLIEST_MORNING_HOUR = 6;
    private static final int LATEST_REASONABLE_HOUR = 22;

    private final PromptBuilder promptBuilder;
    private final Gson gson;
    private final HttpClient httpClient;
    private final Random random;

    private LastCallInfo lastCallInfo = LastCallInfo.fallback("Not called yet");

    public LlmScheduleService() {
        this(new PromptBuilder(new RagRetriever()));
    }

    public LlmScheduleService(PromptBuilder promptBuilder) {
        this(promptBuilder, HttpClient.newHttpClient(), new Random());
    }

    LlmScheduleService(PromptBuilder promptBuilder, HttpClient httpClient, Random random) {
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.random = Objects.requireNonNull(random, "random");
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                        src == null ? null : context.serialize(src.toString()))
                .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, context) ->
                        json == null ? null : LocalTime.parse(json.getAsString()))
                .create();
    }

    public List<ProposedEvent> proposeSchedule(String routineSummary,
                                               List<RoutineEventInput> routineEvents,
                                               List<FixedEventInput> fixedEvents) {
        return proposeSchedule(routineSummary, routineEvents, fixedEvents, null);
    }

    public List<ProposedEvent> proposeSchedule(String routineSummary,
                                               List<RoutineEventInput> routineEvents,
                                               List<FixedEventInput> fixedEvents,
                                               List<RagRetriever.RoutineExample> examples) {
        String apiKey = System.getenv("HUGGINGFACE_API_KEY");
        String model = resolveModelId();

        if (apiKey == null || apiKey.isBlank()) {
            lastCallInfo = LastCallInfo.fallback("Missing HUGGINGFACE_API_KEY");
            System.out.println("[LlmScheduleService] No API key found. Using semantic fallback schedule.");
            return fallbackSchedule(routineSummary, routineEvents, fixedEvents, examples);
        }

        String prompt = promptBuilder.buildSchedulePrompt(routineSummary, routineEvents, fixedEvents, EXAMPLE_COUNT, examples);

        System.out.printf("[LlmScheduleService] Using HF router endpoint with model: %s%n", model);

        HfRequestSettings primary = new HfRequestSettings(600, 0.25, 0.90, false);
        HfRequestSettings retry = new HfRequestSettings(320, 0.20, 0.85, true);
        String lastFailureReason = null;

        for (HfRequestSettings settings : List.of(primary, retry)) {
            try {
                String responseJson = callHuggingFace(prompt, apiKey, model, settings);
                List<ProposedEvent> parsed = parseProposedEvents(responseJson, fixedEvents);
                lastCallInfo = LastCallInfo.liveModel(model, parsed.size());
                System.out.printf("[LlmScheduleService] Used Hugging Face model '%s' (API key length %d) with settings %s. Parsed %d events.%n",
                        model, apiKey.length(), settings.summary(), parsed.size());
                return parsed;
            } catch (Exception ex) {
                lastFailureReason = ex.getMessage();
                System.out.printf("[LlmScheduleService] Hugging Face call failed (%s).%n", ex.getMessage());
                if (!settings.isRetry()) {
                    System.out.println("[LlmScheduleService] Retrying with conservative generation parameters...");
                }
            }
        }

        String reason = lastFailureReason == null ? "Unknown Hugging Face failure" : lastFailureReason;
        lastCallInfo = LastCallInfo.fallback("Model call failed after retry: " + reason);
        System.out.printf("[LlmScheduleService] AI mode disabled after retry (%s), using semantic fallback.%n", reason);
        return fallbackSchedule(routineSummary, routineEvents, fixedEvents, examples);
    }

    private String callHuggingFace(String prompt, String apiKey, String modelId, HfRequestSettings settings) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", modelId);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are a scheduling assistant. Respond ONLY with JSON as instructed."),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", settings.temperature());
        body.put("top_p", settings.topP());
        body.put("max_tokens", settings.maxNewTokens());
        body.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_ROUTER_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String responseBody = response.body();
        System.out.printf("[LlmScheduleService] HF router raw response (truncated to 2000 chars): %s%n",
                truncate(responseBody, 2000));
        if (status >= 400) {
            String bodyText = responseBody == null ? "<empty body>" : truncate(responseBody, 800);
            throw new IllegalStateException("Hugging Face returned status " + status + " with body: " + bodyText);
        }
        System.out.printf("[LlmScheduleService] HF router HTTP %d, body length %d (settings %s).%n",
                status, responseBody == null ? 0 : responseBody.length(), settings.summary());
        return responseBody;
    }

    private String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "...";
    }

    public static String resolveModelId() {
        return Optional.ofNullable(System.getenv("HUGGINGFACE_MODEL_ID"))
                .filter(id -> !id.isBlank())
                .orElse(DEFAULT_MODEL);
    }

    // Parse the OpenAI-style chat completion payload into our RawSchedule JSON and surface clear errors when unusable.
    private List<ProposedEvent> parseProposedEvents(String responseBody, List<FixedEventInput> fixedEvents) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Failed to parse schedule JSON from HF response: empty body");
        }

        String jsonToParse;
        try {
            jsonToParse = extractGeneratedText(responseBody);
        } catch (Exception ex) {
            throw new IllegalStateException(ex.getMessage() + " Raw body (truncated): " + truncate(responseBody, 600), ex);
        }

        Exception strictFailure = null;
        try {
            List<ProposedEvent> parsed = parseStrictSchedule(jsonToParse);
            System.out.printf("[LlmScheduleService] Parsed schedule via strict JSON (%d events).%n", parsed.size());
            return postProcessParsedEvents(parsed, fixedEvents);
        } catch (Exception ex) {
            strictFailure = ex;
        }

        List<ProposedEvent> salvaged = salvageSchedule(jsonToParse);
        if (!salvaged.isEmpty()) {
            System.out.printf("[LlmScheduleService] Salvage JSON parsing recovered %d events after strict failure (%s).%n",
                    salvaged.size(), strictFailure == null ? "unknown" : strictFailure.getClass().getSimpleName());
            return postProcessParsedEvents(salvaged, fixedEvents);
        }

        String failureDetail = strictFailure == null ? "Unknown parsing failure"
                : strictFailure.getClass().getSimpleName() + ": " + strictFailure.getMessage();
        throw new IllegalStateException("Failed to parse schedule JSON from HF response: " + failureDetail
                + ". Extracted content (truncated): " + truncate(jsonToParse, 800), strictFailure);
    }

    private List<ProposedEvent> parseStrictSchedule(String jsonToParse) {
        RawSchedule rawSchedule = gson.fromJson(jsonToParse, RawSchedule.class);
        if (rawSchedule == null) {
            throw new IllegalStateException("deserialized schedule was null");
        }
        if (rawSchedule.events == null) {
            throw new IllegalStateException("missing 'events' field");
        }

        List<ProposedEvent> parsed = rawSchedule.events.stream()
                .map(this::toProposedEvent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (parsed.isEmpty()) {
            throw new IllegalStateException("Parsed schedule JSON from HF response contained 0 valid events.");
        }
        return parsed;
    }

    private List<ProposedEvent> salvageSchedule(String jsonToParse) {
        List<ProposedEvent> salvaged = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{[^{}]*?\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(jsonToParse);
        while (matcher.find()) {
            String candidate = matcher.group();
            if (!candidate.contains("\"day\"") || !candidate.contains("\"startTime\"")) {
                continue;
            }

            try {
                JsonReader reader = new JsonReader(new StringReader(candidate));
                reader.setLenient(true);
                RawEvent rawEvent = gson.fromJson(reader, RawEvent.class);
                ProposedEvent event = toProposedEvent(rawEvent);
                if (event != null) {
                    salvaged.add(event);
                }
            } catch (Exception ignored) {
                // Skip unparseable fragments
            }
        }

        if (!salvaged.isEmpty()) {
            System.out.printf("[LlmScheduleService] Salvage pass accepted %d event objects from partial JSON.%n", salvaged.size());
        } else {
            System.out.println("[LlmScheduleService] Salvage pass found no usable event objects.");
        }
        return salvaged;
    }

    private List<ProposedEvent> postProcessParsedEvents(List<ProposedEvent> events, List<FixedEventInput> fixedEvents) {
        if (events == null) {
            return Collections.emptyList();
        }

        Map<String, FixedEventInput> fixedByKey = new HashMap<>();
        Set<String> preservedKeys = new HashSet<>();
        List<ProposedEvent> result = new ArrayList<>();

        if (fixedEvents != null) {
            for (FixedEventInput fixed : fixedEvents) {
                String key = fixedKey(fixed.getName(), fixed.getStartTime());
                fixedByKey.put(key, fixed);
                ProposedEvent locked = clampToWakingHours(new ProposedEvent(
                        fixed.getDay(), fixed.getStartTime(), fixed.getDurationMinutes(), fixed.getName(), true));
                if (locked != null && preservedKeys.add(slotKey(locked))) {
                    result.add(locked);
                }
            }
        }

        for (ProposedEvent event : events) {
            ProposedEvent clamped = clampToWakingHours(event);
            if (clamped == null) {
                continue;
            }

            String nameTimeKey = fixedKey(clamped.getName(), clamped.getStartTime());
            FixedEventInput matchingFixed = fixedByKey.get(nameTimeKey);
            if (matchingFixed != null && !clamped.getDay().equals(matchingFixed.getDay())) {
                // Drop extra occurrences of a fixed event on the wrong day.
                continue;
            }

            String slotKey = slotKey(clamped);
            if (preservedKeys.contains(slotKey)) {
                continue;
            }
            preservedKeys.add(slotKey);
            result.add(clamped);
        }

        return result;
    }

    private String extractGeneratedText(String responseBody) {
        try {
            OpenAiLikeChatResponse chat = gson.fromJson(responseBody, OpenAiLikeChatResponse.class);
            if (chat != null && chat.choices != null && !chat.choices.isEmpty()) {
                OpenAiChoice choice = chat.choices.get(0);
                if (choice != null && choice.message != null && choice.message.content != null && !choice.message.content.isBlank()) {
                    String content = choice.message.content.trim();
                    System.out.printf("[LlmScheduleService] Extracted text via OpenAI chat branch (len=%d).%n", content.length());
                    return content;
                }
            }
        } catch (Exception ignored) {
            // fall through
        }

        try {
            RawHuggingFaceResponse[] responses = gson.fromJson(responseBody, RawHuggingFaceResponse[].class);
            if (responses != null && responses.length > 0 && responses[0].generated_text != null) {
                System.out.println("[LlmScheduleService] Extracted text via HF array branch.");
                return responses[0].generated_text;
            }
        } catch (Exception ignored) {
            // fall through to substring-based extraction
        }

        int start = responseBody.indexOf('{');
        int end = responseBody.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String content = responseBody.substring(start, end + 1);
            System.out.println("[LlmScheduleService] Extracted text via substring fallback branch.");
            return content;
        }
        throw new IllegalStateException("Failed to extract generated text from HF response");
    }

    /**
     * Helper for debugging HF parsing in isolation. Safe to invoke from an IDE run configuration.
     */
    @SuppressWarnings("unused")
    private void debugOnceWithSamplePrompt() throws Exception {
        String apiKey = System.getenv("HUGGINGFACE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing HUGGINGFACE_API_KEY for debug call");
        }
        String samplePrompt = "Generate a minimal schedule JSON with two events for testing.";
        HfRequestSettings settings = new HfRequestSettings(200, 0.2, 0.85, false);
        String response = callHuggingFace(samplePrompt, apiKey, resolveModelId(), settings);
        List<ProposedEvent> events = parseProposedEvents(response, Collections.emptyList());
        System.out.printf("[LlmScheduleService] Debug parsed %d events from HF.%n", events.size());
    }

    private ProposedEvent toProposedEvent(RawEvent raw) {
        if (raw == null || raw.day == null || raw.startTime == null || raw.name == null) {
            return null;
        }
        try {
            DayOfWeek day = DayOfWeek.valueOf(raw.day.toUpperCase(Locale.ROOT));
            LocalTime start = LocalTime.parse(raw.startTime);
            int duration = raw.durationMinutes == null ? 0 : raw.durationMinutes;
            boolean locked = raw.locked != null && raw.locked;
            return new ProposedEvent(day, start, duration, raw.name, locked);
        } catch (Exception ex) {
            return null;
        }
    }

    private ProposedEvent clampToWakingHours(ProposedEvent candidate) {
        if (candidate == null) {
            return null;
        }
        int durationSlots = Math.max(1, (int) Math.ceil(candidate.getDurationMinutes() / 60.0));
        int latestStartHour = LATEST_REASONABLE_HOUR - durationSlots;
        if (latestStartHour < EARLIEST_MORNING_HOUR) {
            return null;
        }

        int startHour = candidate.getStartTime().getHour();
        int minute = candidate.getStartTime().getMinute();
        int safeHour = Math.min(Math.max(startHour, EARLIEST_MORNING_HOUR), latestStartHour);
        return new ProposedEvent(candidate.getDay(), LocalTime.of(safeHour, minute),
                candidate.getDurationMinutes(), candidate.getName(), candidate.isLocked());
    }

    private String slotKey(ProposedEvent event) {
        return event.getDay().name() + "|" + event.getStartTime() + "|" + event.getName().toLowerCase(Locale.ROOT);
    }

    private String fixedKey(String name, LocalTime startTime) {
        return name.toLowerCase(Locale.ROOT) + "@" + (startTime == null ? "" : startTime);
    }

    private List<ProposedEvent> fallbackSchedule(String routineSummary,
                                                 List<RoutineEventInput> routineEvents,
                                                 List<FixedEventInput> fixedEvents,
                                                 List<RagRetriever.RoutineExample> examples) {
        String summary = routineSummary == null ? "" : routineSummary.toLowerCase(Locale.ROOT);
        List<ProposedEvent> result = new ArrayList<>();
        Map<DayOfWeek, Integer> dayCounts = new EnumMap<>(DayOfWeek.class);
        Set<String> occupiedKeys = new HashSet<>();

        if (fixedEvents != null) {
            for (FixedEventInput fixed : fixedEvents) {
                addEvent(result, dayCounts, occupiedKeys, new ProposedEvent(
                        fixed.getDay(),
                        fixed.getStartTime(),
                        fixed.getDurationMinutes(),
                        fixed.getName(),
                        fixed.isLocked()), 4);
            }
        }
        if (routineEvents != null) {
            for (RoutineEventInput routine : routineEvents) {
                addEvent(result, dayCounts, occupiedKeys, new ProposedEvent(
                        routine.getDay(),
                        routine.getStartTime(),
                        routine.getDurationMinutes(),
                        routine.getName(),
                        false), 4);
            }
        }

        if (examples != null) {
            for (RagRetriever.RoutineExample example : examples) {
                for (ProposedEvent sample : example.getSchedule()) {
                    addEvent(result, dayCounts, occupiedKeys,
                            new ProposedEvent(sample.getDay(), sample.getStartTime(), sample.getDurationMinutes(),
                                    sample.getName(), sample.isLocked()), 4);
                }
            }
        }

        HeuristicProfile profile = HeuristicProfile.from(summary);
        List<DayOfWeek> gymDays = List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        List<DayOfWeek> projectDays = List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
        int morningStart = profile.preferMorning() ? EARLIEST_MORNING_HOUR : 8;

        if (profile.worker()) {
            for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
                int morningHour = pickHour(profile.preferMorning() ? List.of(7, 8, 9) : List.of(9, 10), 9, morningStart);
                int afternoonHour = pickHour(List.of(13, 14, 15), 14, 8);
                addEvent(result, dayCounts, occupiedKeys,
                        new ProposedEvent(day, LocalTime.of(morningHour, 0), 120, "Work - Focus", false), 3);
                addEvent(result, dayCounts, occupiedKeys,
                        new ProposedEvent(day, LocalTime.of(afternoonHour, 0), 150, "Work - Collaboration", false), 3);
            }
        }

        if (profile.student()) {
            for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)) {
                int studyHour = pickHour(profile.preferMorning() ? List.of(9, 10) : List.of(18, 19, 20), 18, morningStart);
                addEvent(result, dayCounts, occupiedKeys,
                        new ProposedEvent(day, LocalTime.of(studyHour, 0), 120, "Study Session", false), 3);
            }
        }

        if (profile.gym()) {
            for (DayOfWeek day : gymDays) {
                int gymHour = pickHour(profile.preferMorning() ? List.of(6, 7, 8) : List.of(17, 18, 19), profile.preferMorning() ? 7 : 18, EARLIEST_MORNING_HOUR);
                addEvent(result, dayCounts, occupiedKeys,
                        new ProposedEvent(day, LocalTime.of(gymHour, 0), 60, "Gym / Workout", false), 3);
            }
        } else {
            addEvent(result, dayCounts, occupiedKeys,
                    new ProposedEvent(DayOfWeek.THURSDAY, LocalTime.of(profile.preferMorning() ? 7 : 18, 0), 45, "Walk & Stretch", false), 3);
        }

        if (profile.family()) {
            for (DayOfWeek day : List.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)) {
                addEvent(result, dayCounts, occupiedKeys,
                        new ProposedEvent(day, LocalTime.of(19, 0), 60, "Family Dinner", false), 3);
            }
        }

        for (DayOfWeek day : projectDays) {
            int projectHour = pickHour(profile.preferNight() ? List.of(19, 20, 21) : List.of(17, 18, 19), 19, 12);
            addEvent(result, dayCounts, occupiedKeys,
                    new ProposedEvent(day, LocalTime.of(projectHour, 0), 120, "Project Time", false), 3);
        }

        addEvent(result, dayCounts, occupiedKeys,
                new ProposedEvent(DayOfWeek.SATURDAY, LocalTime.of(11, 0), 75, "Groceries & Prep", false), 3);
        addEvent(result, dayCounts, occupiedKeys,
                new ProposedEvent(DayOfWeek.SUNDAY, LocalTime.of(20, 0), 60, "Plan Next Week", false), 3);

        for (DayOfWeek day : DayOfWeek.values()) {
            int perDayMax = 3;
            int minimumBlocks = day.getValue() <= DayOfWeek.FRIDAY.getValue() ? 3 : 2;
            while (dayCounts.getOrDefault(day, 0) < Math.min(perDayMax, minimumBlocks)) {
                int hour = profile.preferNight() ? pickHour(List.of(18, 19, 20, 21), 19, 12)
                        : pickHour(List.of(8, 9, 10, 11, 14, 15), 10, morningStart);
                String name = profile.worker() ? "Task Block" : "Focus Session";
                addEvent(result, dayCounts, occupiedKeys,
                        new ProposedEvent(day, LocalTime.of(hour, 0), 60, name, false), perDayMax);
            }
        }

        System.out.printf("[LlmScheduleService] Fallback generated %d proposed events with semantic heuristics.%n", result.size());
        return result;
    }

    private void addEvent(List<ProposedEvent> result,
                          Map<DayOfWeek, Integer> dayCounts,
                          Set<String> occupiedKeys,
                          ProposedEvent candidate,
                          int maxPerDay) {
        ProposedEvent safeCandidate = clampToWakingHours(candidate);
        if (safeCandidate == null) {
            return;
        }
        DayOfWeek day = safeCandidate.getDay();
        String key = day.name() + "-" + safeCandidate.getStartTime();
        if (occupiedKeys.contains(key)) {
            return;
        }
        if (dayCounts.getOrDefault(day, 0) >= maxPerDay) {
            return;
        }
        result.add(safeCandidate);
        occupiedKeys.add(key);
        dayCounts.put(day, dayCounts.getOrDefault(day, 0) + 1);
    }

    private int pickHour(List<Integer> options, int fallback) {
        return pickHour(options, fallback, 0);
    }

    private int pickHour(List<Integer> options, int fallback, int minHour) {
        int choice = fallback;
        if (options != null && !options.isEmpty()) {
            choice = options.get(random.nextInt(options.size()));
        }
        return Math.max(minHour, choice);
    }

    private record HeuristicProfile(boolean preferMorning, boolean preferNight, boolean worker, boolean student,
                                    boolean gym, boolean family) {
        static HeuristicProfile from(String summary) {
            String text = summary == null ? "" : summary.toLowerCase(Locale.ROOT);
            boolean morning = text.contains("morning") || text.contains("early");
            boolean night = text.contains("night") || text.contains("late");
            boolean worker = text.contains("work") || text.contains("job") || text.contains("office")
                    || text.contains("9-5") || text.contains("9 to 5") || text.contains("9–5");
            boolean student = text.contains("student") || text.contains("class")
                    || text.contains("lecture") || text.contains("study");
            boolean gym = text.contains("gym") || text.contains("workout") || text.contains("exercise")
                    || text.contains("run");
            boolean family = text.contains("family") || text.contains("kids") || text.contains("dinner");
            return new HeuristicProfile(morning, night, worker, student, gym, family);
        }
    }

    /**
     * Information about the most recent LLM call, to let users confirm whether the live model was used.
     */
    public LastCallInfo getLastCallInfo() {
        return lastCallInfo;
    }

    public static class LastCallInfo {
        private final boolean usedLiveModel;
        private final String modelId;
        private final String note;
        private final int parsedEvents;

        private LastCallInfo(boolean usedLiveModel, String modelId, String note, int parsedEvents) {
            this.usedLiveModel = usedLiveModel;
            this.modelId = modelId;
            this.note = note;
            this.parsedEvents = parsedEvents;
        }

        public static LastCallInfo liveModel(String modelId, int parsedEvents) {
            return new LastCallInfo(true, modelId, "Live Hugging Face call succeeded", parsedEvents);
        }

        public static LastCallInfo fallback(String reason) {
            return new LastCallInfo(false, null, reason, 0);
        }

        public boolean usedLiveModel() {
            return usedLiveModel;
        }

        public String modelId() {
            return modelId;
        }

        public String note() {
            return note;
        }

        public int parsedEvents() {
            return parsedEvents;
        }

        public String asUserMessage() {
            if (usedLiveModel) {
                return String.format("Generated via Hugging Face model '%s' (%d events parsed)",
                        modelId == null ? "unknown" : modelId, parsedEvents);
            }
            return String.format("Generated via fallback (reason: %s)", note == null ? "unknown" : note);
        }
    }

    private static class RawHuggingFaceResponse {
        String generated_text;
    }

    private static class OpenAiLikeChatResponse {
        List<OpenAiChoice> choices;
    }

    private static class OpenAiChoice {
        OpenAiMessage message;
    }

    private static class OpenAiMessage {
        String content;
    }

    private static class RawSchedule {
        List<RawEvent> events;
    }

    private static class RawEvent {
        String day;
        String startTime;
        Integer durationMinutes;
        String name;
        Boolean locked;
    }

    private record HfRequestSettings(int maxNewTokens, double temperature, double topP, boolean isRetry) {
        String summary() {
            return String.format(Locale.ROOT, "max_tokens=%d, temp=%.2f, top_p=%.2f%s",
                    maxNewTokens, temperature, topP, isRetry ? " (retry)" : "");
        }
    }
}