package plan4life.solver;

import plan4life.ai.ProposedEvent;
import plan4life.ai.rules.ActivityTimeRule;
import plan4life.ai.rules.ActivityTimeRules;
import plan4life.entities.BlockedTime;
import plan4life.entities.Schedule;
import plan4life.entities.ScheduledBlock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A greedy, grid-based scheduler that places events on a 7x24 canvas.
 * Locked/fixed events are preserved exactly as proposed, while flexible
 * events are nudged to the nearest available slot when conflicts arise.
 */
public class ConstraintSolver {

    private static final int HOURS_IN_DAY = 24;
    private static final int DAYS_IN_WEEK = 7;

    private final LocalDate weekStart;
    private final Random random;

    public ConstraintSolver() {
        this(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), new Random());
    }

    public ConstraintSolver(LocalDate weekStart) {
        this(weekStart, new Random());
    }

    public ConstraintSolver(LocalDate weekStart, Random random) {
        this.weekStart = weekStart;
        this.random = random;
    }

    /**
     * Greedily place proposed events into a Schedule while respecting locked blocks and
     * user blocked times. The solver discretizes time into hour-long slots on a weekly grid.
     */
    public Schedule solve(int scheduleId,
                          String scheduleType,
                          List<ProposedEvent> proposedEvents,
                          List<BlockedTime> blockedTimes) {
        Schedule schedule = new Schedule(scheduleId, scheduleType);
        Map<Integer, boolean[]> occupancy = initializeGrid();

        // Respect user blocked periods first so events avoid them.
        if (blockedTimes != null) {
            for (BlockedTime blockedTime : blockedTimes) {
                schedule.addBlockedTime(blockedTime);
                markRangeAsOccupied(occupancy, blockedTime.getColumnIndex(),
                        blockedTime.getStart().getHour(), blockedTime.getEnd().getHour());
            }
        }

        if (proposedEvents == null || proposedEvents.isEmpty()) {
            return schedule;
        }

        List<ProposedEvent> ordered = new ArrayList<>(proposedEvents);
        ordered.sort(Comparator
                .comparing(ProposedEvent::getDay)
                .thenComparing(ProposedEvent::getStartTime));

        // Place locked first so they are always preserved.
        for (ProposedEvent event : ordered) {
            if (event.isLocked()) {
                placeLockedEvent(schedule, occupancy, event);
            }
        }

        List<ProposedEvent> flexible = ordered.stream()
                .filter(event -> !event.isLocked())
                .collect(Collectors.toCollection(ArrayList::new));
        Map<Integer, Integer> occupiedHours = countOccupiedHours(occupancy);
        Collections.shuffle(flexible, random);
        flexible.sort(Comparator
                .comparing((ProposedEvent e) -> occupiedHours.getOrDefault(toColumnIndex(e.getDay()), 0))
                .thenComparing(ProposedEvent::getDay));
        System.out.printf("[ConstraintSolver] placing %d flexible events in balanced order.%n", flexible.size());

        for (ProposedEvent event : flexible) {
            placeFlexibleEvent(schedule, occupancy, event);
        }

        logPerDaySummary(schedule);

        return schedule;
    }

    private void logPerDaySummary(Schedule schedule) {
        int[] counts = new int[DAYS_IN_WEEK];
        schedule.getLockedBlocks().forEach(block -> incrementDayCount(counts, block.getStart().getDayOfWeek()));
        schedule.getUnlockedBlocks().forEach(block -> incrementDayCount(counts, block.getStart().getDayOfWeek()));

        String[] abbreviations = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String summary = "[ConstraintSolver] per-day placed counts:";
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            summary += " " + abbreviations[i] + "=" + counts[i];
        }
        System.out.println(summary);
    }

    private void incrementDayCount(int[] counts, DayOfWeek day) {
        int index = toColumnIndex(day);
        if (index >= 0 && index < counts.length) {
            counts[index]++;
        }
    }

    private Map<Integer, Integer> countOccupiedHours(Map<Integer, boolean[]> occupancy) {
        Map<Integer, Integer> counts = new HashMap<>();
        if (occupancy == null) {
            return counts;
        }
        for (Map.Entry<Integer, boolean[]> entry : occupancy.entrySet()) {
            int hours = 0;
            for (boolean filled : entry.getValue()) {
                if (filled) {
                    hours++;
                }
            }
            counts.put(entry.getKey(), hours);
        }
        return counts;
    }

    private Map<Integer, boolean[]> initializeGrid() {
        Map<Integer, boolean[]> grid = new HashMap<>();
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            grid.put(i, new boolean[HOURS_IN_DAY]);
        }
        return grid;
    }

    private void placeLockedEvent(Schedule schedule, Map<Integer, boolean[]> occupancy, ProposedEvent event) {
        int columnIndex = toColumnIndex(event.getDay());
        LocalDateTime start = toDateTime(event.getDay(), event.getStartTime());
        LocalDateTime end = start.plusMinutes(event.getDurationMinutes());

        schedule.addLockedBlock(new ScheduledBlock(start, end, event.getName(), true, columnIndex));
        String timeKey = formatTimeKey(event.getDay(), event.getStartTime());
        schedule.addActivity(timeKey, event.getName());
        schedule.lockSlotKey(timeKey);

        System.out.printf("[ConstraintSolver] locked -> %s %s (col %d)%n", event.getDay(), event.getStartTime(), columnIndex);

        int startHour = start.getHour();
        int endHour = Math.min(HOURS_IN_DAY, startHour + requiredSlots(event.getDurationMinutes()));
        markRangeAsOccupied(occupancy, columnIndex, startHour, endHour);
    }

    private void placeFlexibleEvent(Schedule schedule, Map<Integer, boolean[]> occupancy, ProposedEvent event) {
        int preferredColumn = toColumnIndex(event.getDay());
        int requiredSlots = requiredSlots(event.getDurationMinutes());

        int windowStart = 0;
        int windowEnd = HOURS_IN_DAY;
        int preferredHour = event.getStartTime().getHour();
        ActivityTimeRule rule = ActivityTimeRules.findRule(event.getName()).orElse(null);
        if (rule != null) {
            windowStart = rule.getWindowStartHour();
            windowEnd = Math.min(HOURS_IN_DAY, rule.getWindowEndHour());
            preferredHour = rule.choosePreferredHour(preferredHour, requiredSlots);

            if (rule.mustPrecedeDinner()) {
                int dinnerStart = findDinnerStartHour(schedule, preferredColumn);
                if (dinnerStart >= 0) {
                    windowEnd = Math.min(windowEnd, dinnerStart);
                    int latestStart = Math.max(windowStart, dinnerStart - requiredSlots);
                    preferredHour = Math.min(preferredHour, latestStart);
                    preferredHour = Math.max(windowStart, preferredHour);
                }
            }
        }

        List<Integer> columnOrder = buildColumnOrderByOccupancy(occupancy, preferredColumn);
        for (int columnIndex : columnOrder) {
            OptionalInt slot = findNearestAvailableSlot(occupancy, columnIndex, preferredHour, requiredSlots,
                    windowStart, windowEnd);
            if (slot.isEmpty()) {
                continue;
            }

            LocalTime placementStart = LocalTime.of(slot.getAsInt(), event.getStartTime().getMinute());
            DayOfWeek placementDay = toDayOfWeek(columnIndex);
            LocalDateTime start = toDateTime(placementDay, placementStart);
            LocalDateTime end = start.plusMinutes(event.getDurationMinutes());

            schedule.addUnlockedBlock(new ScheduledBlock(start, end, event.getName(), false, columnIndex));
            schedule.addActivity(formatTimeKey(placementDay, placementStart), event.getName());

            System.out.printf("[ConstraintSolver] placed -> %s %s (col %d)%n", placementDay, placementStart, columnIndex);

            int startHour = placementStart.getHour();
            int endHour = Math.min(HOURS_IN_DAY, startHour + requiredSlots);
            markRangeAsOccupied(occupancy, columnIndex, startHour, endHour);
            return;
        }

        schedule.addUnplacedActivity(event.getName());
    }

    private OptionalInt findNearestAvailableSlot(Map<Integer, boolean[]> occupancy,
                                                 int columnIndex,
                                                 int preferredHour,
                                                 int requiredSlots,
                                                 int windowStart,
                                                 int windowEnd) {
        int maxStart = windowEnd - requiredSlots;
        if (maxStart < windowStart) {
            return OptionalInt.empty();
        }

        Set<Integer> candidates = new LinkedHashSet<>();
        for (int delta = 0; delta < HOURS_IN_DAY; delta++) {
            int forward = preferredHour + delta;
            int backward = preferredHour - delta;
            if (forward >= windowStart && forward <= maxStart) {
                candidates.add(forward);
            }
            if (backward >= windowStart && backward <= maxStart) {
                candidates.add(backward);
            }
        }

        List<Integer> freeOptions = new ArrayList<>();
        for (int candidate : candidates) {
            if (isRangeFree(occupancy, columnIndex, candidate, candidate + requiredSlots)) {
                freeOptions.add(candidate);
            }
        }

        if (freeOptions.isEmpty()) {
            return OptionalInt.empty();
        }

        int limit = Math.min(5, freeOptions.size());
        int chosen = freeOptions.get(random.nextInt(limit));
        if (limit > 1) {
            System.out.printf("[ConstraintSolver] multiple slots available for column %d, pref %d -> chose %d among %d options.%n",
                    columnIndex, preferredHour, chosen, limit);
        }
        return OptionalInt.of(chosen);
    }

    private int findDinnerStartHour(Schedule schedule, int columnIndex) {
        OptionalInt locked = schedule.getLockedBlocks().stream()
                .filter(block -> block.getColumnIndex() == columnIndex)
                .filter(block -> ActivityTimeRules.isDinnerActivity(block.getActivityName()))
                .mapToInt(block -> block.getStart().getHour())
                .min();

        OptionalInt flexible = schedule.getUnlockedBlocks().stream()
                .filter(block -> block.getColumnIndex() == columnIndex)
                .filter(block -> ActivityTimeRules.isDinnerActivity(block.getActivityName()))
                .mapToInt(block -> block.getStart().getHour())
                .min();

        if (locked.isPresent() && flexible.isPresent()) {
            return Math.min(locked.getAsInt(), flexible.getAsInt());
        }
        return locked.orElseGet(() -> flexible.orElse(-1));
    }

    private List<Integer> buildColumnOrderByOccupancy(Map<Integer, boolean[]> occupancy, int preferredColumn) {
        Map<Integer, Integer> occupiedHours = countOccupiedHours(occupancy);
        List<Integer> otherColumns = new ArrayList<>();
        for (int column = 0; column < DAYS_IN_WEEK; column++) {
            if (column != preferredColumn) {
                otherColumns.add(column);
            }
        }

        otherColumns.sort(Comparator
                .comparing((Integer column) -> occupiedHours.getOrDefault(column, 0))
                .thenComparingInt(column -> column));

        List<Integer> ordered = new ArrayList<>();
        ordered.add(preferredColumn);
        ordered.addAll(otherColumns);
        return ordered;
    }

    private void markRangeAsOccupied(Map<Integer, boolean[]> occupancy, int columnIndex, int startHour, int endHour) {
        if (!occupancy.containsKey(columnIndex)) {
            return;
        }
        boolean[] hours = occupancy.get(columnIndex);
        int safeStart = Math.max(0, startHour);
        int safeEnd = Math.min(HOURS_IN_DAY, endHour);
        for (int hour = safeStart; hour < safeEnd; hour++) {
            hours[hour] = true;
        }
    }

    private boolean isRangeFree(Map<Integer, boolean[]> occupancy, int columnIndex, int startHour, int endHour) {
        boolean[] hours = occupancy.getOrDefault(columnIndex, new boolean[HOURS_IN_DAY]);
        for (int hour = startHour; hour < endHour; hour++) {
            if (hour < 0 || hour >= HOURS_IN_DAY) {
                return false;
            }
            if (hours[hour]) {
                return false;
            }
        }
        return true;
    }

    private int toColumnIndex(DayOfWeek day) {
        return day.getValue() - 1;
    }

    private DayOfWeek toDayOfWeek(int columnIndex) {
        return DayOfWeek.of(columnIndex + 1);
    }

    private LocalDateTime toDateTime(DayOfWeek day, LocalTime time) {
        LocalDate date = weekStart.plusDays(toColumnIndex(day));
        return LocalDateTime.of(date, time);
    }

    private String formatTimeKey(DayOfWeek day, LocalTime time) {
        String[] abbreviations = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String dayName = abbreviations[toColumnIndex(day)];
        return String.format("%s %02d:%02d", dayName, time.getHour(), time.getMinute());
    }

    private int requiredSlots(int durationMinutes) {
        return Math.max(1, (int) Math.ceil(durationMinutes / 60.0));
    }
}