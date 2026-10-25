package demo.chess.game;

import demo.chess.definitions.PieceType;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;

/**
 * Tracks the FIDE halfmove clock used by the fifty-move rule.
 *
 * <p>The counter is reset by pawn moves and captures and incremented by every
 * other completed move. Call {@link #record(Move)} before the move mutates its
 * target square so ordinary captures can be detected from the current board.</p>
 */
public final class HalfmoveClock {

    public static final int FIFTY_MOVE_RULE_PLIES = 100;

    private int pliesSincePawnMoveOrCapture;

    /** Records one move against the pre-move board state. */
    public void record(Move move) {
        if (resetsClock(move)) {
            pliesSincePawnMoveOrCapture = 0;
        } else {
            pliesSincePawnMoveOrCapture++;
        }
    }

    /** @return halfmoves since the latest pawn move or capture */
    public int getPliesSincePawnMoveOrCapture() {
        return pliesSincePawnMoveOrCapture;
    }

    /** @return whether one hundred reversible halfmoves have been completed */
    public boolean isFiftyMoveRuleReached() {
        return pliesSincePawnMoveOrCapture >= FIFTY_MOVE_RULE_PLIES;
    }

    private boolean resetsClock(Move move) {
        if (move == null || move.getPiece() == null) return false;
        if (move.getPiece().getType() == PieceType.PAWN) return true;
        if (move instanceof EnPassant) return true;
        if (move instanceof Castling) return false;
        return move.getTarget() != null && move.getTarget().getPiece() != null;
    }
}
