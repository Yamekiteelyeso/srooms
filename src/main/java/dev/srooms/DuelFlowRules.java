package dev.srooms;

/** Rules that keep arena glass independent of the waiting player. */
final class DuelFlowRules {
    private DuelFlowRules() { }

    static boolean shouldPlaceCrystal(int participants, boolean firstInside, boolean secondInside) {
        return participants == 2 && firstInside && secondInside;
    }

    static boolean startImmediately(int countdownSeconds) {
        return countdownSeconds <= 0;
    }
}
