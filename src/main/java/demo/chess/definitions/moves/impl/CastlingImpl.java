package demo.chess.definitions.moves.impl;

import java.util.Objects;

import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Rook;

/**
 * Implementation of a castling move in a chess game.
 */
public class CastlingImpl extends ChessMove implements Castling {

	private final Rook rook;

	private final String name;

	/**
	 * Constructs a CastlingImpl with the specified king and rook pieces.
	 *
	 * @param piece the king involved in the castling move
	 * @param rook  the rook involved in the castling move
	 */
	public CastlingImpl(Piece piece, Rook rook) {
		super(piece, piece.getField(), rook.getField());
		this.rook = rook;
		int rank = getPiece().getField().getRank();
		if (getRook().getField().getFile() == 8) {
			name = "e" + rank + "g" + rank;
		} else {
			name = "e" + rank + "c" + rank;
		}
	}

	@Override
	public Rook getRook() {
		return rook;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(name, rook);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj) || (getClass() != obj.getClass())) {
			return false;
		}
		CastlingImpl other = (CastlingImpl) obj;
		return Objects.equals(name, other.name) && Objects.equals(rook, other.rook);
	}
}
