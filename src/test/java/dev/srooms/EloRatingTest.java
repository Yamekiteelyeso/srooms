package dev.srooms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EloRatingTest {
    @Test void equalRatedWinnerGains16() {
        assertEquals(16, EloRating.winDelta(1000, 1000, 32));
    }
    @Test void upsetGivesMorePoints() {
        assertTrue(EloRating.winDelta(800, 1200, 32) > EloRating.winDelta(1200, 800, 32));
    }
    @Test void drawBetweenEqualsDoesNotMove() {
        assertEquals(0, EloRating.expectedDelta(1000, 1000, 32, 0.5));
    }
    @Test void higherRatedDrawLosesPoints() {
        assertTrue(EloRating.expectedDelta(1400, 1000, 32, 0.5) < 0);
    }
    @Test void ratingNeverGoesNegative() {
        assertEquals(0, EloRating.clamp(-10));
    }
    @Test void invalidKRejected() {
        assertThrows(IllegalArgumentException.class, () -> EloRating.expectedDelta(1000, 1000, 0, 1));
    }
}
