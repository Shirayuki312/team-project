package plan4life.ai;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/** Shared time formats for parsing and serializing schedule prompts. */
public final class TimeFormats {
    /** Allows inputs like "9:00" while accepting optional seconds. */
    public static final DateTimeFormatter OPTIONAL_SECONDS = new DateTimeFormatterBuilder()
            .appendPattern("H:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter();

    private TimeFormats() {
    }
}