package demo.chess.definitions.moves.impl;

import java.util.Objects;

import demo.chess.definitions.CastlingSide;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Rook;

/**
 * Chess960-capable castling move.
 *
 * <p>The move stores the complete geometry of the compound king/rook move.
 * The inherited {@link #getTarget()} remains the rook source for compatibility
 * with the historical {@code Move} contract, while dedicated accessors expose
 * the semantically relevant king and rook destinations.</p>
 */
public class CastlingImpl extends ChessMove implements Castling {

    private final Rook rook;
    private final CastlingSide side;
    private final Field rookSource;
    private final Field kingTarget;
    private final Field rookTarget;
    private final String name;

    public CastlingImpl(Piece piece, Rook rook) {
        this(piece, rook, rook.getField().getFile() > piece.getField().getFile()
                ? CastlingSide.KING_SIDE
                : CastlingSide.QUEEN_SIDE);
    }

    public CastlingImpl(Piece piece, Rook rook, CastlingSide side) {
        super(piece, piece.getField(), rook.getField());
        this.rook = Objects.requireNonNull(rook, "rook");
        this.side = Objects.requireNonNull(side, "side");
        this.rookSource = rook.getField();

        int rank = piece.getField().getRank();
        this.kingTarget = piece.getChessBoard().getField(
                side == CastlingSide.KING_SIDE ? 7 : 3,
                rank);
        this.rookTarget = piece.getChessBoard().getField(
                side == CastlingSide.KING_SIDE ? 6 : 4,
                rank);
        this.name = getSource().getName() + kingTarget.getName();
    }

    @Override
    public Rook getRook() {
        return rook;
    }

    @Override
    public CastlingSide getSide() {
        return side;
    }

    @Override
    public Field getRookSource() {
        return rookSource;
    }

    @Override
    public Field getKingTarget() {
        return kingTarget;
    }

    @Override
    public Field getRookTarget() {
        return rookTarget;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(name, rook, side, rookSource);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj) || getClass() != obj.getClass()) return false;
        CastlingImpl other = (CastlingImpl) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(rook, other.rook)
                && Objects.equals(rookSource, other.rookSource)
                && side == other.side;
    }
}
