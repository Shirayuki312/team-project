package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;

import java.util.Map;
import java.util.Objects;

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

        // Step 1 — generate base schedule
        Schedule schedule = generationService.generate(description, fixed);

        // Step 2 — place free activities ("desc:duration")
        for (String free : requestModel.getFreeActivities()) {

            String[] parts = free.split(":");
            String activityDesc = parts[0].trim();

            float duration = 1.0f;
            if (parts.length > 1) {
                try {
                    duration = Float.parseFloat(parts[1].trim());
                } catch (NumberFormatException ignore) {
                    duration = 1.0f;
                }
            }

            generationService.assignActivityToSlot(schedule, activityDesc, duration);
        }

        // Step 3 — output
        presenter.present(new GenerateScheduleResponseModel(schedule));
    }
}
