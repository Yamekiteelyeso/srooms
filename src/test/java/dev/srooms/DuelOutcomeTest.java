package dev.srooms;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DuelOutcomeTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test void firstDefeatedInRealFight() {
        assertEquals(second, DuelOutcome.winner(true, first, second, first));
    }

    @Test void secondDefeatedInRealFight() {
        assertEquals(first, DuelOutcome.winner(true, first, second, second));
    }

    @Test void countdownLeaveCannotAwardVictory() {
        assertNull(DuelOutcome.winner(false, first, second, first));
        assertNull(DuelOutcome.winner(false, first, second, second));
    }

    @Test void waitingForOpponentCannotAwardVictory() {
        assertNull(DuelOutcome.winner(false, first, null, first));
        assertNull(DuelOutcome.winner(true, first, null, first));
    }

    @Test void timeLimitDrawCannotAwardVictory() {
        assertNull(DuelOutcome.winner(true, first, second, null));
    }

    @Test void unrelatedLoserCannotAwardVictory() {
        assertNull(DuelOutcome.winner(true, first, second, UUID.randomUUID()));
    }

    @Test void duplicateParticipantCannotAwardVictory() {
        assertNull(DuelOutcome.winner(true, first, first, first));
    }
}
