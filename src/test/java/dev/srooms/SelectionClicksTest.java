package dev.srooms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SelectionClicksTest {
    private static final SelectionClicks.Point A = new SelectionClicks.Point("world", 10, 65, 10);
    private static final SelectionClicks.Point B = new SelectionClicks.Point("world", 15, 65, 18);
    private static final long START = 2_000_000_000L;

    @Test void gateCannotCompleteWithOnlyOneClick() {
        SelectionClicks selection = new SelectionClicks(80, 160);
        selection.nextStage(START);
        assertEquals(SelectionClicks.Result.STAGE_COOLDOWN, selection.click(true, A, START + 1));
        assertEquals(SelectionClicks.Result.FIRST_POINT, selection.click(true, A, START + 200_000_000L));
        assertEquals(SelectionClicks.Result.NEED_FIRST, new SelectionClicks(80, 160)
                .click(false, B, START + 200_000_000L));
        assertNotNull(selection.first());
    }

    @Test void validLeftThenRightNeedsTwoDistinctInteractions() {
        SelectionClicks selection = new SelectionClicks(80, 160);
        assertEquals(SelectionClicks.Result.FIRST_POINT, selection.click(true, A, START));
        assertEquals(SelectionClicks.Result.SECOND_POINT, selection.click(false, B, START + 120_000_000L));
    }

    @Test void duplicateSameBlockCannotCompleteSelection() {
        SelectionClicks selection = new SelectionClicks(80, 160);
        selection.click(true, A, START);
        assertEquals(SelectionClicks.Result.SAME_BLOCK, selection.click(false, A, START + 120_000_000L));
    }

    @Test void duplicateEventsInSameTickCannotComplete() {
        SelectionClicks selection = new SelectionClicks(80, 160);
        selection.click(true, A, START);
        assertEquals(SelectionClicks.Result.TOO_FAST, selection.click(false, B, START + 1_000_000L));
    }

    @Test void newStageClearsPreviousPoint() {
        SelectionClicks selection = new SelectionClicks(80, 160);
        selection.click(true, A, START);
        selection.click(false, B, START + 120_000_000L);
        selection.nextStage(START + 120_000_000L);
        assertNull(selection.first());
        assertEquals(SelectionClicks.Result.NEED_FIRST,
                selection.click(false, B, START + 350_000_000L));
    }

    @Test void reselectingFirstPointDoesNotReuseOldSecondPoint() {
        SelectionClicks selection = new SelectionClicks(80, 160);
        selection.click(true, A, START);
        selection.click(true, B, START + 100_000_000L);
        assertEquals(B, selection.first());
        assertEquals(SelectionClicks.Result.SECOND_POINT,
                selection.click(false, A, START + 210_000_000L));
    }
}
