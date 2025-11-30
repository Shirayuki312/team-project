package plan4life.entities;

import java.time.LocalDateTime;
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
}

