package demo.chess.definitions.engines.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import demo.chess.definitions.ChessStartingPosition;

public class UciPositionCommandTest {

    private static final String POSITION_518_FEN =
            ChessStartingPosition.STANDARD.initialFen();

    @Test
    public void emptyMoveListUsesExplicitPosition518Fen() {
        assertEquals(
                "position fen " + POSITION_518_FEN,
                UciPositionCommand.build(new StringBuilder()));
        assertEquals(
                "position fen " + POSITION_518_FEN,
                UciPositionCommand.build(new StringBuilder("   ")));
    }

    @Test
    public void moveListUsesMovesClauseAfterExplicitFen() {
        assertEquals(
                "position fen " + POSITION_518_FEN + " moves e2e4 e7e5 g1f3",
                UciPositionCommand.build(new StringBuilder("e2e4 e7e5 g1f3 ")));
    }
}
