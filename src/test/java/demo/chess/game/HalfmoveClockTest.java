package demo.chess.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.impl.CastlingImpl;
import demo.chess.definitions.moves.impl.ChessMove;
import demo.chess.definitions.pieces.impl.King;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.game.impl.Simulation;

public class HalfmoveClockTest {

    @Test
    public void reachesFiftyMoveRuleAfterOneHundredReversibleHalfmoves() {
        Simulation game = Simulation.createSimulation();
        Move reversible = new ChessMove(
                game.getChessBoard().getField(7, 1).getPiece(),
                game.getChessBoard().getField(7, 1),
                game.getChessBoard().getField(6, 3));
        HalfmoveClock clock = new HalfmoveClock();

        for (int ply = 0; ply < 99; ply++) clock.record(reversible);
        assertFalse(clock.isFiftyMoveRuleReached());

        clock.record(reversible);
        assertTrue(clock.isFiftyMoveRuleReached());
        assertEquals(100, clock.getPliesSincePawnMoveOrCapture());
    }

    @Test
    public void pawnMovesAndCapturesResetTheClock() {
        Simulation game = Simulation.createSimulation();
        Move reversible = new ChessMove(
                game.getChessBoard().getField(7, 1).getPiece(),
                game.getChessBoard().getField(7, 1),
                game.getChessBoard().getField(6, 3));
        Move pawnMove = new ChessMove(
                game.getChessBoard().getField(5, 2).getPiece(),
                game.getChessBoard().getField(5, 2),
                game.getChessBoard().getField(5, 4));
        Move capture = new ChessMove(
                game.getChessBoard().getField(7, 1).getPiece(),
                game.getChessBoard().getField(7, 1),
                game.getChessBoard().getField(7, 7));
        HalfmoveClock clock = new HalfmoveClock();

        clock.record(reversible);
        clock.record(reversible);
        clock.record(pawnMove);
        assertEquals(0, clock.getPliesSincePawnMoveOrCapture());

        clock.record(reversible);
        clock.record(capture);
        assertEquals(0, clock.getPliesSincePawnMoveOrCapture());
    }

    @Test
    public void castlingDoesNotResetTheHalfmoveClock() {
        Simulation game = Simulation.createSimulation();
        King king = (King) game.getChessBoard().getField(5, 1).getPiece();
        Rook rook = (Rook) game.getChessBoard().getField(8, 1).getPiece();
        Castling castling = new CastlingImpl(king, rook);
        HalfmoveClock clock = new HalfmoveClock();

        clock.record(castling);

        assertEquals(1, clock.getPliesSincePawnMoveOrCapture());
    }
}
