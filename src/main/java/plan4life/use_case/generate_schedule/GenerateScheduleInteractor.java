package plan4life.use_case.generate_schedule;

import plan4life.entities.Schedule;
import java.util.Map;
import java.util.Objects;

/**
 * Interactor coordinates generation of Schedule using an injected ScheduleGenerationService.
 * This keeps the LLM / RAG integration behind an interface (clean architecture).
 */
public class GenerateScheduleInteractor implements GenerateScheduleInputBoundary {
    private final GenerateScheduleOutputBoundary presenter;
    private final ScheduleGenerationService generationService;

    /**
     * @param presenter         the output boundary (presenter)
     * @param generationService service responsible for producing Schedule from input (mock or real LLM)
     */
    public GenerateScheduleInteractor(GenerateScheduleOutputBoundary presenter,
                                      ScheduleGenerationService generationService) {
        this.presenter = Objects.requireNonNull(presenter, "presenter must not be null");
        this.generationService = Objects.requireNonNull(generationService, "generationService must not be null");
    }

    @Override
    public void execute(GenerateScheduleRequestModel requestModel) {
        if (requestModel == null) {
            presenter.present(new GenerateScheduleResponseModel(new Schedule()));
            return;
        }

        String description = requestModel.getRoutineDescription();
        Map<String, String> fixed = requestModel.getFixedActivities();

        // Generate initial schedule
        Schedule schedule = generationService.generate(description, fixed);

        // NEW: auto-place activities without time/date
        for (String activity : requestModel.getFreeActivities()) {
            generationService.assignActivityToSlot(schedule, activity);
        }

        presenter.present(new GenerateScheduleResponseModel(schedule));
    }
}
