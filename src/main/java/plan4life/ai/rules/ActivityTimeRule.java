package plan4life.ai.rules;

/**
 * Represents preferred placement guidance for an activity name pattern.
 */
public class ActivityTimeRule {
    private final int windowStartHour;
    private final int windowStartMinute;
    private final int windowEndHour;
    private final int windowEndMinute;
    private final int preferredStartHour;
    private final int preferredStartMinute;
    private final boolean mustPrecedeDinner;
    private final boolean weekdaysOnly;

    public ActivityTimeRule(int windowStartHour, int windowEndHour, int preferredStartHour, boolean mustPrecedeDinner) {
        this(windowStartHour, 0, windowEndHour, 0, preferredStartHour, 0, mustPrecedeDinner, false);
    }

    public ActivityTimeRule(int windowStartHour,
                            int windowStartMinute,
                            int windowEndHour,
                            int windowEndMinute,
                            int preferredStartHour,
                            int preferredStartMinute,
                            boolean mustPrecedeDinner,
                            boolean weekdaysOnly) {
        this.windowStartHour = Math.max(0, windowStartHour);
        this.windowStartMinute = clampMinute(windowStartMinute);
        this.windowEndHour = Math.min(24, windowEndHour);
        this.windowEndMinute = clampMinute(windowEndMinute);
        this.preferredStartHour = preferredStartHour;
        this.preferredStartMinute = clampMinute(preferredStartMinute);
        this.mustPrecedeDinner = mustPrecedeDinner;
        this.weekdaysOnly = weekdaysOnly;
    }

    public int getWindowStartHour() {
        return windowStartHour;
    }

    public int getWindowStartMinute() {
        return windowStartMinute;
    }

    public int getWindowEndHour() {
        return windowEndHour;
    }

    public int getWindowEndMinute() {
        return windowEndMinute;
    }

    public int getPreferredStartHour() {
        return preferredStartHour;
    }

    public int getPreferredStartMinute() {
        return preferredStartMinute;
    }

    public boolean mustPrecedeDinner() {
        return mustPrecedeDinner;
    }

    public boolean isWeekdaysOnly() {
        return weekdaysOnly;
    }

    /**
     * Choose a preferred hour that stays within the rule window and accommodates the required slots.
     */
    public int choosePreferredHour(int proposedHour, int requiredSlots) {
        int maxStart = windowEndHour - requiredSlots;
        if (maxStart < windowStartHour) {
            return windowStartHour;
        }

        int candidate = proposedHour;
        if (candidate < windowStartHour || candidate > maxStart) {
            candidate = preferredStartHour;
        }

        if (candidate < windowStartHour) {
            candidate = windowStartHour;
        }
        if (candidate > maxStart) {
            candidate = maxStart;
        }
        return candidate;
    }

    /**
     * Blend a proposed hour with the preferred hour, leaning toward the proposed time when both are valid.
     */
    public int blendPreferredHour(int proposedHour, int requiredSlots) {
        int maxStart = windowEndHour - requiredSlots;
        if (maxStart < windowStartHour) {
            return windowStartHour;
        }

        if (proposedHour >= windowStartHour && proposedHour <= maxStart) {
            int blended = Math.round((proposedHour * 2 + preferredStartHour) / 3.0f);
            if (blended < windowStartHour) {
                blended = windowStartHour;
            }
            if (blended > maxStart) {
                blended = maxStart;
            }
            return blended;
        }
        return choosePreferredHour(proposedHour, requiredSlots);
    }

    private int clampMinute(int minute) {
        if (minute < 0) {
            return 0;
        }
        if (minute > 59) {
            return 59;
        }
        return minute;
    }
}