package plan4life.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.LocalTime;
import java.util.List;
import java.util.StringJoiner;

/**
 * Builds a prompt instructing the language model to emit JSON describing the schedule.
 */
public class PromptBuilder {

    private final RagRetriever ragRetriever;
    private final Gson gson;

    public PromptBuilder(RagRetriever ragRetriever) {
        this.ragRetriever = ragRetriever;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalTime.class, (JsonSerializer<LocalTime>) (src, typeOfSrc, context) ->
                        src == null ? null : context.serialize(src.toString()))
                .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, context) ->
                        json == null ? null : LocalTime.parse(json.getAsString()))
                .create();
    }

    public String buildSchedulePrompt(String routineSummary,
                                      List<RoutineEventInput> routineEvents,
                                      List<FixedEventInput> fixedEvents,
                                      int exampleCount,
                                      List<RagRetriever.RoutineExample> providedExamples) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a friendly assistant who proposes a weekly schedule.\n");
        builder.append("Return ONLY JSON with an array named \"events\" where each item has: \n");
        builder.append("{\"day\": \"MONDAY\", \"startTime\": \"09:00\", \"durationMinutes\": 60, \"name\": \"Task\", \"locked\": false}.\n");
        builder.append("Use 24-hour time. Keep locked=true for fixed items and locked=false for flexible suggestions.\n\n");

        builder.append("User routine summary: ").append(routineSummary == null ? "" : routineSummary).append("\n\n");
        builder.append("Flexible routine hints: ").append(gson.toJson(routineEvents)).append("\n");
        builder.append("Fixed items that must be preserved: ").append(gson.toJson(fixedEvents)).append("\n\n");

        List<RagRetriever.RoutineExample> examples = providedExamples;
        if ((examples == null || examples.isEmpty()) && ragRetriever != null) {
            examples = ragRetriever.retrieveExamples(routineSummary, exampleCount);
        }

        if (examples != null && !examples.isEmpty()) {
            StringJoiner joiner = new StringJoiner("\n\n", "Examples to imitate:\n", "\n");
            for (RagRetriever.RoutineExample example : examples) {
                String exampleText = "Routine: " + example.getRoutineSummary() + "\n" +
                        "Schedule JSON: {\"events\": " + gson.toJson(example.getSchedule()) + "}";
                if (example.getRationale() != null) {
                    exampleText += "\nRationale: " + example.getRationale();
                }
                joiner.add(exampleText);
            }
            builder.append(joiner);
            builder.append("\n");
        }

        builder.append("Do not invent days beyond Monday-Sunday. Keep durations non-negative.");
        return builder.toString();
    }
}