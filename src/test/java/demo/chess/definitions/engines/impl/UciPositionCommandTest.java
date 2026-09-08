package demo.chess.definitions.engines.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UciPositionCommandTest {

    /**
     * Verifies that an empty move list produces the plain start position command.
     */
    @Test
    public void emptyMoveListUsesPlainStartPosition() {
        assertEquals("position startpos", UciPositionCommand.build(new StringBuilder()));
        assertEquals("position startpos", UciPositionCommand.build(new StringBuilder("   ")));
    }

    /**
     * Verifies that existing moves are appended using valid UCI syntax.
     */
    @Test
    public void moveListUsesMovesClause() {
        assertEquals(
                "position startpos moves e2e4 e7e5 g1f3",
                UciPositionCommand.build(new StringBuilder("e2e4 e7e5 g1f3 ")));
    }
}
