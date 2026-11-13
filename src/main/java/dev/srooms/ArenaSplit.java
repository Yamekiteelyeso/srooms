package dev.srooms;

/**
 * Divides a single selected room into two potential player-spawn sides.
 * Prefers the side of the barrier that physically separates both halves.
 * Does not require the administrator to select two room zones.
 */
final class ArenaSplit {
    record Partition(boolean alongX, int firstMin, int firstMax, int secondMin, int secondMax) {
        boolean valid() { return firstMin <= firstMax && secondMin <= secondMax; }
    }

    private ArenaSplit() {}

    static Partition choose(int minX, int maxX, int minZ, int maxZ,
                            int gateMinX, int gateMaxX, int gateMinZ, int gateMaxZ) {
        boolean x = gateMinX > minX && gateMaxX < maxX;
        boolean z = gateMinZ > minZ && gateMaxZ < maxZ;
        boolean useX;
        if (x && z) {
            useX = (gateMaxX - gateMinX) <= (gateMaxZ - gateMinZ);
        } else if (x) {
            useX = true;
        } else if (z) {
            useX = false;
        } else {
            useX = maxX - minX >= maxZ - minZ;
        }
        if (useX) {
            if (x) return new Partition(true, minX, gateMinX - 1, gateMaxX + 1, maxX);
            int center = minX + (maxX - minX) / 2;
            return new Partition(true, minX, center, center + 1, maxX);
        }
        if (z) return new Partition(false, minZ, gateMinZ - 1, gateMaxZ + 1, maxZ);
        int center = minZ + (maxZ - minZ) / 2;
        return new Partition(false, minZ, center, center + 1, maxZ);
    }
}
