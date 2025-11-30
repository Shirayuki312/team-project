package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;

import java.util.Map;
import java.util.Objects;

/**
 * Interactor that orchestrates schedule generation.
 * Supports:
 *   - Routine description (LLM analysis)
 *   - Fixed activities (user-specified exact times)
 *   - Free activities (description + duration, no time)
 */
public class GenerateScheduleInteractor implements GenerateScheduleInputBoundary {
    private final GenerateScheduleOutputBoundary presenter;
    private final ScheduleGenerationService generationService;

    public GenerateScheduleInteractor(GenerateScheduleOutputBoundary presenter,
                                      ScheduleGenerationService generationService) {
        this.presenter = Objects.requireNonNull(presenter);
        this.generationService = Objects.requireNonNull(generationService);
    }

    @Override
    public void execute(GenerateScheduleRequestModel requestModel) {

        if (requestModel == null) {
            presenter.present(new GenerateScheduleResponseModel(new Schedule()));
            return;
        }

        String description = requestModel.getRoutineDescription();
        Map<String, String> fixed = requestModel.getFixedActivities();

        // 1. Generate baseline schedule using RAG/LLM + fixed constraints
        Schedule schedule = generationService.generate(description, fixed);

        // 2. Assign free activities intelligently
        for (String free : requestModel.getFreeActivities()) {

            String activityDesc = free;
            float duration = 1.0f;

            // Detect:  "Running (2h)"
            int parenStart = free.lastIndexOf("(");
            int parenEnd = free.lastIndexOf(")");

            if (parenStart != -1 && parenEnd != -1 && parenEnd > parenStart) {
                activityDesc = free.substring(0, parenStart).trim();

                String inside = free.substring(parenStart + 1, parenEnd).trim();
                inside = inside.replace("h", "").trim();  // remove "h"

                try {
                    duration = Float.parseFloat(inside);
                } catch (NumberFormatException ignore) {
                    duration = 1.0f;
                }
            }

            generationService.assignActivityToSlot(schedule, activityDesc, duration);
        }
        // 3. Present output
        presenter.present(new GenerateScheduleResponseModel(schedule));
    }
}