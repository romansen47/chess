package demo.chess.definitions;

/**
 * Semantic side of a castling move.
 *
 * <p>The side is independent of the king and rook start files. In Chess960 the
 * king-side rook is the original rook to the king's right and the queen-side
 * rook is the original rook to the king's left.</p>
 */
public enum CastlingSide {
    KING_SIDE,
    QUEEN_SIDE
}
