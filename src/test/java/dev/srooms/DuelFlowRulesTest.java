package dev.srooms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DuelFlowRulesTest {
    @Test void neverPlaceGlassForFirstPlayer() {
        assertFalse(DuelFlowRules.shouldPlaceCrystal(1, true, false));
    }
    @Test void onlyPlaceGlassWhenBothArePhysicallyInside() {
        assertTrue(DuelFlowRules.shouldPlaceCrystal(2, true, true));
        assertFalse(DuelFlowRules.shouldPlaceCrystal(2, false, true));
        assertFalse(DuelFlowRules.shouldPlaceCrystal(2, true, false));
        assertFalse(DuelFlowRules.shouldPlaceCrystal(3, true, true));
    }
    @Test void zeroCountdownStartsImmediately() {
        assertTrue(DuelFlowRules.startImmediately(0));
        assertFalse(DuelFlowRules.startImmediately(3));
    }
    @Test void optionalTeleportDefaultsOff() {
        Room room = new Room("pvp", null, null);
        assertFalse(room.teleportOnJoin());
        assertFalse(room.openCrystalOnFight());
        assertEquals(0, room.countdown());
        room.teleportOnJoin(true);
        assertTrue(room.teleportOnJoin());
    }
    @Test void countdownAcceptsZeroAndMaximum() {
        Room room = new Room("pvp", null, null);
        room.countdown(10);
        assertEquals(10, room.countdown());
        room.countdown(0);
        assertEquals(0, room.countdown());
        room.countdown(1000);
        assertEquals(30, room.countdown());
    }
}
