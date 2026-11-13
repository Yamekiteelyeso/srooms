package dev.srooms;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArenaSplitTest {
    @Test void splitsAlongXWhenBarrierIsAnXWall() {
        ArenaSplit.Partition p = ArenaSplit.choose(0, 20, 0, 8, 10, 10, 0, 8);
        assertTrue(p.alongX());
        assertEquals(0, p.firstMin());
        assertEquals(9, p.firstMax());
        assertEquals(11, p.secondMin());
        assertEquals(20, p.secondMax());
        assertTrue(p.valid());
    }

    @Test void splitsAlongZWhenBarrierIsAZWall() {
        ArenaSplit.Partition p = ArenaSplit.choose(0, 8, 0, 20, 0, 8, 10, 10);
        assertFalse(p.alongX());
        assertEquals(9, p.firstMax());
        assertEquals(11, p.secondMin());
        assertTrue(p.valid());
    }

    @Test void handlesEntranceGateOnEdgeWithoutRequiringTwoZones() {
        ArenaSplit.Partition p = ArenaSplit.choose(0, 20, 0, 8, 0, 0, 0, 8);
        assertTrue(p.alongX());
        assertEquals(10, p.firstMax());
        assertEquals(11, p.secondMin());
        assertTrue(p.valid());
    }

    @Test void rejectsDegenerateRoomWhenAutomaticSidesCannotFit() {
        ArenaSplit.Partition p = ArenaSplit.choose(0, 0, 0, 0, 0, 0, 0, 0);
        assertFalse(p.valid());
    }
}
