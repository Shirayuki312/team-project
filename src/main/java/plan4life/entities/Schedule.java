package plan4life.entities;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Schedule {
    private final int scheduleId;
    private final String type;    // "day", "week", etc.
    private final Map<String, String> activities = new LinkedHashMap<>();
    // Keys are "dayIndex:hour" (e.g., "2:14"), Values are activity description
    private final List<Activity> tasks;
    private final List<ScheduledBlock> unlockedBlocks;
    private final List<ScheduledBlock> lockedBlocks;
    private final List<BlockedTime> blockedTimes;
    private final List<String> unplacedActivities;

    // Set of locked time keys (e.g., "Mon 9:00")
    private final Set<String> lockedSlotKeys = new HashSet<>();

    public Schedule(int scheduleId, String type) {
        this.scheduleId = scheduleId;
        this.type = type;

        this.tasks = new ArrayList<>();
        this.unlockedBlocks = new ArrayList<>();
        this.lockedBlocks = new ArrayList<>();
        this.blockedTimes = new ArrayList<>();
        this.unplacedActivities = new ArrayList<>();
    }

    public Schedule() {
        this(0, "testing");
    }

    public int getScheduleId() { return scheduleId; }
    public String getType() { return type; }

    public List<BlockedTime> getBlockedTimes() {
        return Collections.unmodifiableList(blockedTimes);
    }

    public List<ScheduledBlock> getLockedBlocks() {
        return Collections.unmodifiableList(lockedBlocks);
    }

    public List<ScheduledBlock> getUnlockedBlocks() {
        return Collections.unmodifiableList(unlockedBlocks);
    }

    public List<String> getUnplacedActivities() {
        return Collections.unmodifiableList(unplacedActivities);
    }

    public void addUnplacedActivity(String activityName) {
        if (activityName != null && !activityName.isBlank()) {
            unplacedActivities.add(activityName);
        }
    }

    // locked keys accessor
    public Set<String> getLockedSlotKeys() {
        return Collections.unmodifiableSet(lockedSlotKeys);
    }

    public void addLockedBlock(ScheduledBlock block) {
        if (block != null) {
            lockedBlocks.add(block);
        }
    }

    public void addUnlockedBlock(ScheduledBlock block) {
        if (block != null) {
            unlockedBlocks.add(block);
        }
    }

    // lock/unlock using time-key strings
    public void lockSlotKey(String timeKey) {
        if (timeKey == null) return;
        lockedSlotKeys.add(timeKey);
    }

    public void replaceLockedSlotKeys(Set<String> newLockedKeys) {
        lockedSlotKeys.clear();
        if (newLockedKeys != null) {
            lockedSlotKeys.addAll(newLockedKeys);
        }
    }

    public void unlockSlotKey(String timeKey) {
        if (timeKey == null) return;
        lockedSlotKeys.remove(timeKey);
    }

    public boolean isLockedKey(String timeKey) {
        return timeKey != null && lockedSlotKeys.contains(timeKey);
    }

    public void addActivity(String timeSlot, String activity) {
        activities.put(timeSlot, activity);
    }

    public Map<String, String> getActivities() {
        return Collections.unmodifiableMap(activities);
    }

    public void addTask(Activity activity) {
        tasks.add(activity);
    }

    public List<Activity> getTasks() { return tasks; }

    public void removeTask(Activity activity) { tasks.remove(activity); }

    // Reformatted populateRandomly
    public void populateRandomly() {
        activities.clear();
        Random rand = new Random();
        String[] sampleActivities = {"Work", "Gym", "Study", "Relax", "Sleep"};

        // 7 days, 24 hours
        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 24; hour++) {
                String key = day + ":" + hour;

                if (lockedSlotKeys.contains(key)) continue;

                // fill sparsely — random 30% chance
                if (rand.nextFloat() < 0.3f) {
                    activities.put(key, sampleActivities[rand.nextInt(sampleActivities.length)]);
                }
            }
        }
    }

    public void populateRandomly(Set<String> lockedKeys) {
        activities.clear();
        Random rand = new Random();
        String[] sampleActivities = {"Work", "Gym", "Study", "Relax", "Sleep"};

        for (int day = 0; day < 7; day++) {
            for (int hour = 0; hour < 24; hour++) {
                String key = day + ":" + hour;

                if (lockedKeys != null && lockedKeys.contains(key)) continue;
                if (lockedSlotKeys.contains(key)) continue;

                if (rand.nextFloat() < 0.3f) {
                    activities.put(key, sampleActivities[rand.nextInt(sampleActivities.length)]);
                }
            }
        }
    }


    public boolean overlapsWithExistingBlocks(LocalDateTime start, LocalDateTime end, int columnIndex) {
        for (BlockedTime block : blockedTimes) {
            if (block.getColumnIndex() != columnIndex) continue;
            if (block.overlaps(start, end)) {
                return true;
            }
        }
        return false;
    }

    public boolean overlapsWithActivities(LocalDateTime start, LocalDateTime end, int columnIndex) {
        for (ScheduledBlock block : lockedBlocks) {
            if (block.overlaps(start, end, columnIndex)) {
                return true;
            }
        }

        for (ScheduledBlock block : unlockedBlocks) {
            if (block.overlaps(start, end, columnIndex)) {
                return true;
            }
        }

        return false;
    }

    public void removeOverlappingActivities(LocalDateTime start, LocalDateTime end, int columnIndex) {
        // Remove only the unlocked blocks in the matching column that overlap this range
        unlockedBlocks.removeIf(block -> block.overlaps(start, end, columnIndex));

        // Prune activities map only for the impacted column/time window, skipping locked entries
        LocalTimeRange removalRange = new LocalTimeRange(start.toLocalTime(), end.toLocalTime());
        activities.entrySet().removeIf(entry -> {
            ParsedTimeKey parsed = parseTimeKey(entry.getKey());
            if (parsed == null || parsed.columnIndex != columnIndex) {
                return false;
            }
            if (lockedSlotKeys.contains(entry.getKey())) {
                return false;
            }
            return removalRange.contains(parsed.time);
        });
    }

    public void addBlockedTime(BlockedTime block) {
        blockedTimes.add(block);
    }

    public void removeBlockedTime(BlockedTime block) {
        blockedTimes.remove(block);
    }

    // Copy locked activities into this schedule from a source schedule
    public void copyLockedActivitiesFrom(Schedule source) {
        if (source == null) return;
        // copy map entries for locked keys if present in source
        for (String key : source.getLockedSlotKeys()) {
            String activity = source.getActivities().get(key);
            if (activity != null) {
                this.activities.put(key, activity);
                this.lockedSlotKeys.add(key);
            }
        }
    }

    public void placeActivity(int dayIndex, int startHour, String description) {
        String key = dayIndex + ":" + startHour;
        activities.put(key, description);
    }

    public void placeActivityDuration(int dayIndex, int startHour, float duration, String description) {
        int hours = (int)Math.ceil(duration);
        for (int h = 0; h < hours; h++) {
            String key = dayIndex + ":" + (startHour + h);
            activities.put(key, description);
        }
    }

    public Integer getId() {
        return scheduleId;
    }

    // Just for JUnit tests. This would normally be package-protected or private.
    public void addUnlockedBlockForTest(ScheduledBlock block) {
        unlockedBlocks.add(block);
    }

    public void clearBlockedTimes() {
        this.blockedTimes.clear();
    }

    public void clearLockedSlotKeys() {
        lockedSlotKeys.clear();
    }
}

    private ParsedTimeKey parseTimeKey(String timeKey) {
        if (timeKey == null || !timeKey.contains(" ")) {
            return null;
        }

        String[] parts = timeKey.split(" ");
        if (parts.length < 2) {
            return null;
        }

        int columnIndex = toColumnIndex(parts[0]);
        if (columnIndex < 0) {
            return null;
        }

        try {
            LocalTime time = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm"));
            return new ParsedTimeKey(columnIndex, time);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private int toColumnIndex(String dayAbbreviation) {
        if (dayAbbreviation == null) {
            return -1;
        }

        return switch (dayAbbreviation.trim().toUpperCase(Locale.ROOT)) {
            case "MON", "MONDAY" -> 0;
            case "TUE", "TUESDAY" -> 1;
            case "WED", "WEDNESDAY" -> 2;
            case "THU", "THURSDAY" -> 3;
            case "FRI", "FRIDAY" -> 4;
            case "SAT", "SATURDAY" -> 5;
            case "SUN", "SUNDAY" -> 6;
            default -> -1;
        };
    }

    private static class ParsedTimeKey {
        final int columnIndex;
        final LocalTime time;

        ParsedTimeKey(int columnIndex, LocalTime time) {
            this.columnIndex = columnIndex;
            this.time = time;
        }
    }

    private static class LocalTimeRange {
        private final LocalTime start;
        private final LocalTime end;

        LocalTimeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        boolean contains(LocalTime time) {
            return !time.isBefore(start) && !time.isAfter(end);
        }
    }
}