package plan4life.ai.rules;

import java.util.Locale;
import java.util.Optional;

/**
 * Maps activity name patterns to suggested placement windows and preferences.
 */
public final class ActivityTimeRules {

    private ActivityTimeRules() {
    }

    public static Optional<ActivityTimeRule> findRule(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            return Optional.empty();
        }

        String normalized = activityName.toLowerCase(Locale.ROOT);
        if (normalized.contains("gym")) {
            return Optional.of(new ActivityTimeRule(5, 0, 10, 0, 6, 0, false, false));
        }
        if (normalized.contains("breakfast")) {
            return Optional.of(new ActivityTimeRule(7, 0, 9, 30, 8, 0, false, false));
        }
        if (normalized.contains("morning routine") || normalized.contains("morning ritual")) {
            return Optional.of(new ActivityTimeRule(5, 0, 11, 0, 7, 0, false, false));
        }
        if (normalized.contains("dinner") && normalized.contains("prep")) {
            return Optional.of(new ActivityTimeRule(16, 0, 18, 0, 17, 0, true, false));
        }
        if (normalized.contains("lunch")) {
            return Optional.of(new ActivityTimeRule(11, 0, 13, 30, 12, 0, false, false));
        }
        if (isDinnerActivity(normalized)) {
            return Optional.of(new ActivityTimeRule(18, 0, 20, 0, 19, 0, false, false));
        }
        if (normalized.contains("focus block") || normalized.contains("deep work") || normalized.contains("focus session")) {
            return Optional.of(new ActivityTimeRule(8, 0, 15, 0, 9, 0, false, true));
        }
        if (normalized.contains("work")) {
            return Optional.of(new ActivityTimeRule(9, 0, 17, 0, 9, 0, false, true));
        }
        if (normalized.contains("grocery") || normalized.contains("groceries")) {
            return Optional.of(new ActivityTimeRule(15, 0, 20, 0, 18, 0, false, false));
        }
        return Optional.empty();
    }

    public static boolean isDinnerActivity(String activityName) {
        if (activityName == null) {
            return false;
        }
        String normalized = activityName.toLowerCase(Locale.ROOT);
        return normalized.contains("dinner") && !normalized.contains("prep");
    }

    public static boolean isMorningRoutine(String activityName) {
        if (activityName == null) {
            return false;
        }
        String normalized = activityName.toLowerCase(Locale.ROOT);
        return normalized.contains("morning routine") || normalized.contains("morning ritual");
    }
}