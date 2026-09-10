package demo.chess.definitions.clocks.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Regression tests for core-owned clock calculations.
 */
public class ChessClockTest {

    @Test
    public void remainingTimeUsesIncrementAndAdditionalTime() {
        ChessClock clock = new ChessClock();
        clock.setTargetTimeMillis(300_000L);
        clock.setIncrementMillis(5_000L);
        clock.setTimeUpAction(() -> {
        });

        clock.start();
        try {
            long before = clock.getRemainingTimeMillis();

            clock.addIncrement();
            long afterIncrement = clock.getRemainingTimeMillis();

            clock.addAdditionalTime(10_000L);
            long afterAdditionalTime = clock.getRemainingTimeMillis();

            assertTrue(afterIncrement - before >= 4_500L);
            assertTrue(afterAdditionalTime - afterIncrement >= 9_500L);
            assertFalse(clock.isTimeUp());
        } finally {
            clock.stop();
        }
    }

    @Test
    public void zeroTargetIsTimeUpWithoutNegativeRemainingTime() {
        ChessClock clock = new ChessClock();
        clock.setTargetTimeMillis(0L);

        assertTrue(clock.isTimeUp());
        assertTrue(clock.getRemainingTimeMillis() == 0L);
    }
}
