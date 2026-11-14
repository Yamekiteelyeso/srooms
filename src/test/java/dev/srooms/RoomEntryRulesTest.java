package dev.srooms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomEntryRulesTest {
    @Test void acceptsPlayerCrossingSelectedCrystalDoorway() {
        assertTrue(RoomEntryRules.canWalkIn(true, true));
    }

    @Test void acceptsPlayerInsideRoomAwayFromGlass() {
        assertTrue(RoomEntryRules.canWalkIn(true, false));
    }

    @Test void rejectsLocationsOutsideArena() {
        assertFalse(RoomEntryRules.canWalkIn(false, false));
    }
}
