package plan4life.entities;

import java.time.LocalDateTime;
import java.util.*;

public class Schedule {
    private final int scheduleId;
    private final String type;    // "day", "week", etc.
    private final Map<String, String> activities = new LinkedHashMap<>();    // e.g., "Monday 9 AM" -> "Workout"
    private final List<Activity> tasks;
    private final List<ScheduledBlock> unlockedBlocks;
    private final List<ScheduledBlock> lockedBlocks;
    private final List<BlockedTime> blockedTimes;

    // Set of locked time keys (e.g., "Mon 9:00")
    private final Set<String> lockedSlotKeys = new HashSet<>();

    public Schedule(int scheduleId, String type) {
        this.scheduleId = scheduleId;
        this.type = type;

        this.tasks = new ArrayList<>();
        this.unlockedBlocks = new ArrayList<>();
        this.lockedBlocks = new ArrayList<>();
        this.blockedTimes = new ArrayList<>();
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

    // locked keys accessor
    public Set<String> getLockedSlotKeys() {
        return Collections.unmodifiableSet(lockedSlotKeys);
    }

    // lock/unlock using time-key strings
    public void lockSlotKey(String timeKey) {
        if (timeKey == null) return;
        lockedSlotKeys.add(timeKey);
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

    public void removeTask(Activity activity) {
        tasks.remove(activity);
    }

    public List<Activity> getTasks() { return Collections.unmodifiableList(tasks); }

    // Reformatted populateRandomly
    public void populateRandomly() {
        populateRandomly(Collections.emptySet());
    }

    // New populateRandomly that respects locked keys
    public void populateRandomly(Set<String> lockedKeys) {
        activities.clear();
        String[] sampleActivities = {"Work", "Gym", "Lunch", "Relax", "Sleep"};
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        Random rand = new Random();

        for (String day : days) {
            for (int i = 9; i <= 17; i += 2) {
                String time = day + " " + i + ":00";
                if (lockedKeys != null && lockedKeys.contains(time)) {
                    // If the old schedule had a locked activity for this time, preserve it if present:
                    // We'll not overwrite — caller should already have added locked activities into activities map before calling populate.
                    continue;
                }
                activities.put(time, sampleActivities[rand.nextInt(sampleActivities.length)]);
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
            if (block.getColumnIndex() != columnIndex) continue;
            if (block.overlaps(start, end)) {
                return true;
            }
        }

        for (ScheduledBlock block : unlockedBlocks) {
            if (block.overlaps(start, end)) {
                return true;
            }
        }

        return false;
    }

    public void removeOverlappingActivities(LocalDateTime start, LocalDateTime end) {
        // This means remove block if block overlaps this range
        unlockedBlocks.removeIf(block -> block.overlaps(start, end));
        lockedBlocks.removeIf(block -> block.overlaps(start, end));
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
}

