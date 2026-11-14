package dev.srooms;

/**
 * Tracks the two intentional block-clicks required for a region.
 * Independent of Bukkit so both event order and duplicate events can be
 * regression-tested without starting a Minecraft server.
 */
final class SelectionClicks {
    enum Result { FIRST_POINT, SECOND_POINT, NEED_FIRST, SAME_BLOCK, TOO_FAST, STAGE_COOLDOWN }

    record Point(String world, int x, int y, int z) { }

    private final long minClickGapNanos;
    private final long stageCooldownNanos;
    private Point first;
    private long firstAt;
    private long readyAt = Long.MIN_VALUE;

    SelectionClicks(int minClickGapMillis, int stageCooldownMillis) {
        minClickGapNanos = Math.max(0, Math.min(1000, minClickGapMillis)) * 1_000_000L;
        stageCooldownNanos = Math.max(0, Math.min(2000, stageCooldownMillis)) * 1_000_000L;
    }

    Result click(boolean left, Point current, long timestampNanos) {
        if (timestampNanos < readyAt) return Result.STAGE_COOLDOWN;
        if (left) {
            first = current;
            firstAt = timestampNanos;
            return Result.FIRST_POINT;
        }
        if (first == null) return Result.NEED_FIRST;
        if (first.equals(current)) return Result.SAME_BLOCK;
        if (timestampNanos - firstAt < minClickGapNanos) return Result.TOO_FAST;
        return Result.SECOND_POINT;
    }

    Point first() { return first; }

    void nextStage(long timestampNanos) {
        first = null;
        firstAt = 0;
        readyAt = timestampNanos + stageCooldownNanos;
    }
}
