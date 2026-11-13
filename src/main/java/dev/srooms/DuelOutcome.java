package dev.srooms;

import java.util.UUID;

/** Pure, testable match outcome rules. A winner requires a started 1v1. */
final class DuelOutcome {
    private DuelOutcome() { }

    static UUID winner(boolean fightStarted, UUID first, UUID second, UUID loser) {
        if (!fightStarted || first == null || second == null || loser == null
                || first.equals(second)) return null;
        if (loser.equals(first)) return second;
        if (loser.equals(second)) return first;
        return null;
    }
}
