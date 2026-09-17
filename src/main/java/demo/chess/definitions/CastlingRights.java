package demo.chess.definitions;

/**
 * Mutable castling-rights state bound to the two original rooks of each color.
 *
 * <p>Chess960 cannot represent castling rights with booleans alone because the
 * participating rook may start on any legal file. Each active right therefore
 * stores the original rook file. Clearing a value permanently removes that
 * castling right for the current game state.</p>
 *
 * <p>The object belongs to one {@code Game} instance and is copied when an
 * independent state copy is required. Starting-position geometry itself remains
 * immutable in {@link ChessStartingPosition}.</p>
 */
public final class CastlingRights {

    private Integer whiteKingSideRookFile;
    private Integer whiteQueenSideRookFile;
    private Integer blackKingSideRookFile;
    private Integer blackQueenSideRookFile;

    /** Creates full initial castling rights for a starting position. */
    public CastlingRights(ChessStartingPosition startingPosition) {
        ChessStartingPosition position = startingPosition != null
                ? startingPosition
                : ChessStartingPosition.STANDARD;
        whiteKingSideRookFile = position.getKingSideRookFile();
        whiteQueenSideRookFile = position.getQueenSideRookFile();
        blackKingSideRookFile = position.getKingSideRookFile();
        blackQueenSideRookFile = position.getQueenSideRookFile();
    }

    private CastlingRights(CastlingRights source) {
        whiteKingSideRookFile = source.whiteKingSideRookFile;
        whiteQueenSideRookFile = source.whiteQueenSideRookFile;
        blackKingSideRookFile = source.blackKingSideRookFile;
        blackQueenSideRookFile = source.blackQueenSideRookFile;
    }

    /** @return independent copy of the current rights */
    public CastlingRights copy() {
        return new CastlingRights(this);
    }

    /** Returns whether the requested castling right is still active. */
    public boolean canCastle(Color color, CastlingSide side) {
        return getRookFile(color, side) != null;
    }

    /**
     * Returns the original rook file associated with an active right.
     *
     * @return one-based file number, or {@code null} when the right is lost
     */
    public Integer getRookFile(Color color, CastlingSide side) {
        if (color == Color.WHITE) {
            return side == CastlingSide.KING_SIDE
                    ? whiteKingSideRookFile
                    : whiteQueenSideRookFile;
        }
        return side == CastlingSide.KING_SIDE
                ? blackKingSideRookFile
                : blackQueenSideRookFile;
    }

    /** Permanently disables one castling right for the current game state. */
    public void disable(Color color, CastlingSide side) {
        if (color == Color.WHITE) {
            if (side == CastlingSide.KING_SIDE) whiteKingSideRookFile = null;
            else whiteQueenSideRookFile = null;
        } else {
            if (side == CastlingSide.KING_SIDE) blackKingSideRookFile = null;
            else blackQueenSideRookFile = null;
        }
    }

    /** Permanently disables both castling rights of one color. */
    public void disableAll(Color color) {
        disable(color, CastlingSide.KING_SIDE);
        disable(color, CastlingSide.QUEEN_SIDE);
    }

    /**
     * Disables the castling right owned by the original rook on {@code file}.
     * This is used both when that rook moves and when it is captured.
     */
    public void disableRookAt(Color color, int file) {
        for (CastlingSide side : CastlingSide.values()) {
            Integer rookFile = getRookFile(color, side);
            if (rookFile != null && rookFile == file) disable(color, side);
        }
    }
}
