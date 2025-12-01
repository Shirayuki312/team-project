package plan4life.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generates a schedule proposal using a Hugging Face model.
 * Falls back to a deterministic schedule when no model is available.
 */
public class LlmScheduleService {

    private static final String DEFAULT_MODEL = "mistralai/Mistral-7B-Instruct-v0.2";
    private static final int EXAMPLE_COUNT = 2;

    private final PromptBuilder promptBuilder;
    private final Gson gson;
    private final HttpClient httpClient;

    public LlmScheduleService() {
        this(new PromptBuilder(new RagRetriever()));
    }

    public LlmScheduleService(PromptBuilder promptBuilder) {
        this(promptBuilder, HttpClient.newHttpClient());
    }

    LlmScheduleService(PromptBuilder promptBuilder, HttpClient httpClient) {
        this.promptBuilder = Objects.requireNonNull(promptBuilder, "promptBuilder");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
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
        String model = Optional.ofNullable(System.getenv("HUGGINGFACE_MODEL_ID"))
                .filter(id -> !id.isBlank())
                .orElse(DEFAULT_MODEL);

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackSchedule(routineEvents, fixedEvents);
        }

        String prompt = promptBuilder.buildSchedulePrompt(routineSummary, routineEvents, fixedEvents, EXAMPLE_COUNT, examples);
        try {
            String responseJson = callHuggingFace(prompt, apiKey, model);
            List<ProposedEvent> parsed = parseProposedEvents(responseJson);
            if (parsed.isEmpty()) {
                return fallbackSchedule(routineEvents, fixedEvents);
            }
            return parsed;
        } catch (Exception ex) {
            return fallbackSchedule(routineEvents, fixedEvents);
        }
    }

    private String callHuggingFace(String prompt, String apiKey, String modelId) throws Exception {
        Map<String, Object> body = Map.of(
                "inputs", prompt,
                "parameters", Map.of(
                        "max_new_tokens", 600,
                        "temperature", 0.2,
                        "return_full_text", false)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api-inference.huggingface.co/models/" + modelId))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Hugging Face returned status " + response.statusCode());
        }
        return response.body();
    }

    private List<ProposedEvent> parseProposedEvents(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return Collections.emptyList();
        }

        String jsonToParse = extractGeneratedText(responseBody).orElse(responseBody);
        try {
            RawSchedule rawSchedule = gson.fromJson(jsonToParse, RawSchedule.class);
            if (rawSchedule == null || rawSchedule.events == null) {
                return Collections.emptyList();
            }
            return rawSchedule.events.stream()
                    .map(this::toProposedEvent)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private Optional<String> extractGeneratedText(String responseBody) {
        try {
            RawHuggingFaceResponse[] responses = gson.fromJson(responseBody, RawHuggingFaceResponse[].class);
            if (responses != null && responses.length > 0 && responses[0].generated_text != null) {
                return Optional.of(responses[0].generated_text);
            }
        } catch (Exception ignored) {
            // fall through to substring-based extraction
        }

        int start = responseBody.indexOf('{');
        int end = responseBody.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return Optional.of(responseBody.substring(start, end + 1));
        }
        return Optional.empty();
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

    private List<ProposedEvent> fallbackSchedule(List<RoutineEventInput> routineEvents, List<FixedEventInput> fixedEvents) {
        List<ProposedEvent> result = new ArrayList<>();
        if (fixedEvents != null) {
            for (FixedEventInput fixed : fixedEvents) {
                result.add(new ProposedEvent(
                        fixed.getDay(),
                        fixed.getStartTime(),
                        fixed.getDurationMinutes(),
                        fixed.getName(),
                        fixed.isLocked()));
            }
        }
        if (routineEvents != null) {
            for (RoutineEventInput routine : routineEvents) {
                result.add(new ProposedEvent(
                        routine.getDay(),
                        routine.getStartTime(),
                        routine.getDurationMinutes(),
                        routine.getName(),
                        false));
            }
        }
        return result;
    }

    private static class RawHuggingFaceResponse {
        String generated_text;
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
}