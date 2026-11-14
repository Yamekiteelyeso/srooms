package dev.srooms;

/**
 * A crystal selection is a door/barrier *inside* an arena, not a forbidden
 * area. Players entering through that doorway must be registered normally.
 */
final class RoomEntryRules {
    private RoomEntryRules() { }

    static boolean canWalkIn(boolean insideRoom, boolean inGlassSelection) {
        // The second argument documents and protects the important edge case.
        // A room includes its entrance even if the glass footprint overlaps.
        return insideRoom;
    }
}
