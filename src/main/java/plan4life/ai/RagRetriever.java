package plan4life.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Retrieves similar routines to prime the LLM with examples.
 * Falls back to a lightweight token-overlap similarity so the feature works without embeddings.
 */
public class RagRetriever {

    private static final String DEFAULT_RESOURCE = "/ai/examples/routines.json";
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                    src == null ? null : context.serialize(src.toString()))
            .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, context) ->
                    json == null ? null : LocalTime.parse(json.getAsString()))
            .create();

    private final List<RoutineExample> examples;
    private final boolean embeddingsAvailable;

    public RagRetriever() {
        this(false);
    }

    public RagRetriever(boolean embeddingsAvailable) {
        this(DEFAULT_RESOURCE, embeddingsAvailable);
    }

    public RagRetriever(String resourcePath, boolean embeddingsAvailable) {
        this.embeddingsAvailable = embeddingsAvailable;
        this.examples = loadExamples(resourcePath);
    }

    /**
     * Returns up to {@code k} examples most similar to the provided summary.
     */
    public List<RoutineExample> retrieveExamples(String routineSummary, int k) {
        if (routineSummary == null || routineSummary.isBlank()) {
            return Collections.emptyList();
        }
        return examples.stream()
                .sorted(Comparator.comparingDouble(example -> -score(routineSummary, example)))
                .limit(Math.max(k, 0))
                .toList();
    }

    private double score(String routineSummary, RoutineExample example) {
        double lexical = tokenOverlap(routineSummary, example.routineSummary);
        if (!embeddingsAvailable) {
            return lexical; // fallback mode stays purely local
        }
        // Placeholder: when embeddings are wired in, blend the lexical score with embedding distance.
        return lexical;
    }

    private double tokenOverlap(String a, String b) {
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        return List.of(text.toLowerCase().split("\\W+")).stream()
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private List<RoutineExample> loadExamples(String resourcePath) {
        try (InputStream stream = RagRetriever.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return new ArrayList<>();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                Type listType = new TypeToken<List<RoutineExample>>() { }.getType();
                List<RoutineExample> loaded = GSON.fromJson(reader, listType);
                return loaded != null ? loaded : new ArrayList<>();
            }
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public static class RoutineExample {
        private String routineSummary;
        private List<ProposedEvent> schedule;
        private String rationale;

        public String getRoutineSummary() {
            return routineSummary;
        }

        public List<ProposedEvent> getSchedule() {
            return schedule == null ? Collections.emptyList() : schedule;
        }

        public String getRationale() {
            return rationale;
        }
    }
}