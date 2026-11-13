package dev.srooms;

/** Pure Elo calculations: deterministic and unit-testable. */
public final class EloRating {
    private EloRating() { }

    public static int expectedDelta(int self, int other, int k, double score) {
        if (!Double.isFinite(score) || score < 0 || score > 1) {
            throw new IllegalArgumentException("Score must be in [0, 1]");
        }
        if (k < 1 || k > 128) throw new IllegalArgumentException("K must be in [1, 128]");
        double exponent = Math.max(-10, Math.min(10, (other - self) / 400.0));
        double expected = 1.0 / (1.0 + Math.pow(10, exponent));
        return (int) Math.round(k * (score - expected));
    }

    public static int winDelta(int winner, int loser, int k) {
        return Math.max(1, expectedDelta(winner, loser, k, 1.0));
    }

    public static int clamp(int rating) {
        return Math.max(0, Math.min(50000, rating));
    }
}
