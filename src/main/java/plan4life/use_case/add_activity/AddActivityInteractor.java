package plan4life.use_case.add_activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddActivityInteractor implements AddActivityInputBoundary {

    private final AddActivityOutputBoundary presenter;
    private final List<String> pendingActivities = new ArrayList<>();

    public AddActivityInteractor(AddActivityOutputBoundary presenter) {
        this.presenter = presenter;
    }

    @Override
    public void execute(AddActivityRequestModel requestModel) {
        if (requestModel == null) {
            AddActivityResponseModel response = new AddActivityResponseModel(
                    "No activity entered. Nothing was added.",
                    Collections.unmodifiableList(pendingActivities)
            );
            presenter.present(response);
            return;
        }

        String name = requestModel.getName();
        if (name == null || name.trim().isEmpty()) {
            AddActivityResponseModel response = new AddActivityResponseModel(
                    "Activity name cannot be empty.",
                    Collections.unmodifiableList(pendingActivities)
            );
            presenter.present(response);
            return;
        }

        float duration = requestModel.getDurationHours();
        if (duration <= 0) {
            AddActivityResponseModel response = new AddActivityResponseModel(
                    "Duration must be a positive number of hours.",
                    Collections.unmodifiableList(pendingActivities)
            );
            presenter.present(response);
            return;
        }

        String tagName = requestModel.getTagName();
        String trimmedName = name.trim();

        String summary;
        if (tagName != null && !tagName.trim().isEmpty()) {
            summary = trimmedName + " (" + duration + " h, " + tagName.trim() + ")";
        } else {
            summary = trimmedName + " (" + duration + " h)";
        }

        pendingActivities.add(summary);

        AddActivityResponseModel response = new AddActivityResponseModel(
                "Added activity: " + summary,
                Collections.unmodifiableList(pendingActivities)
        );
        presenter.present(response);
    }
}
