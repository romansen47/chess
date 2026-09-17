package demo.chess.definitions;

/**
 * Castling rights bound to the two original rooks of a Chess960 position.
 * The rook file is kept because K/Q alone is insufficient in Chess960.
 */
public final class CastlingRights {

    private Integer whiteKingSideRookFile;
    private Integer whiteQueenSideRookFile;
    private Integer blackKingSideRookFile;
    private Integer blackQueenSideRookFile;

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

    public CastlingRights copy() {
        return new CastlingRights(this);
    }

    public boolean canCastle(Color color, CastlingSide side) {
        return getRookFile(color, side) != null;
    }

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

    public void disable(Color color, CastlingSide side) {
        if (color == Color.WHITE) {
            if (side == CastlingSide.KING_SIDE) whiteKingSideRookFile = null;
            else whiteQueenSideRookFile = null;
        } else {
            if (side == CastlingSide.KING_SIDE) blackKingSideRookFile = null;
            else blackQueenSideRookFile = null;
        }
    }

    public void disableAll(Color color) {
        disable(color, CastlingSide.KING_SIDE);
        disable(color, CastlingSide.QUEEN_SIDE);
    }

    public void disableRookAt(Color color, int file) {
        for (CastlingSide side : CastlingSide.values()) {
            Integer rookFile = getRookFile(color, side);
            if (rookFile != null && rookFile == file) {
                disable(color, side);
            }
        }
    }
}
