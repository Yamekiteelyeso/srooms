package dev.srooms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomDefaultsTest {

    @Test
    void newArenaCanBeEnteredByWalkingWithoutAnyCommands() {
        Room room = new Room("Box1", null, null);
        assertTrue(room.enabled(), "The room itself must remain open.");
        assertTrue(room.walkIn(), "Walking in is the core feature.");
    }

    @Test
    void allOptionalNewArenaGameplayTogglesStartDisabled() {
        Room room = new Room("Box1", null, null);
        assertFalse(room.keepInventory());
        assertFalse(room.keepExperience());
        assertFalse(room.allowBuild());
        assertFalse(room.lockHunger());
        assertFalse(room.healOnJoin());
        assertFalse(room.clearEffects());
        assertFalse(room.blockCommands());
        assertFalse(room.blockEnderPearls());
        assertFalse(room.freezeCountdown());
        assertFalse(room.rewardWinner());
        assertFalse(room.kitEnabled());
        assertFalse(room.ranked());
        assertTrue(room.effects().isEmpty());
        assertEquals(0, room.reopenDelay());
    }
}
