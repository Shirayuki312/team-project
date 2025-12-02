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
                        src == null ? null : context.serialize(TimeFormats.OPTIONAL_SECONDS.format(src)))
                .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, context) ->
                        json == null ? null : LocalTime.parse(json.getAsString(), TimeFormats.OPTIONAL_SECONDS))
                .create();
    }

    public String buildSchedulePrompt(String routineSummary,
                                      List<RoutineEventInput> routineEvents,
                                      List<FixedEventInput> fixedEvents,
                                      int exampleCount,
                                      List<RagRetriever.RoutineExample> providedExamples) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are a friendly assistant who proposes a weekly schedule.\n");
        builder.append("Return ONLY a single JSON object of the form { \"events\": [ ... ] }.\n");
        builder.append("Do not include comments, explanations, or ellipses. No trailing text.\n");
        builder.append("Every event must include: \"day\" (e.g. \"MONDAY\"), \"startTime\" (\"HH:MM\"), \"durationMinutes\" (integer), \"name\" (string), and \"locked\" (boolean).\n");
        builder.append("Example event: {\"day\": \"MONDAY\", \"startTime\": \"09:00\", \"durationMinutes\": 60, \"name\": \"Task\", \"locked\": false}.\n");
        builder.append("Use 24-hour time. Keep locked=true for fixed items and locked=false for flexible suggestions.\n\n");

        builder.append("Rules to follow strictly:\n");
        builder.append("1) You are given a list of fixed events. Each fixed event must appear exactly once at the specified day and time. Do not create additional events with the same name on other days unless explicitly described in the routine.\n");
        builder.append("2) Mark these fixed events with \"locked\": true and do not change their day, startTime, or duration.\n");
        builder.append("3) Do not schedule events before 06:00 or after 22:00.\n");
        builder.append("4) For a standard workweek profile, distribute events across all weekdays Monday–Friday. Aim for at least 3–5 events per weekday between 06:00 and 22:00, including 1–2 focus/task blocks and a lunch break on most weekdays. Respect the persona (morning vs night) when picking times.\n");
        builder.append("5) Typical times: lunch near noon within 11:30–14:00; dinner with family about 19:00 within 18:00–20:00; dinner prep 16:00–18:00 before dinner; standard workdays 09:00–17:00.\n\n");

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