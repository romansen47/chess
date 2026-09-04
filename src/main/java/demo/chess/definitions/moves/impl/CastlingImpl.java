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
	 * Creates a new CastlingImpl instance.
	 * @param piece the piece
	 * @param rook the rook
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

	/**
	 * Returns the rook.
	 * @return the rook
	 */
	@Override
	public Rook getRook() {
		return rook;
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Returns the hash code for this object.
	 * @return true when the condition is satisfied; otherwise false
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(name, rook);
		return result;
	}

	/**
	 * Compares this object with another object for equality.
	 * @param obj the obj
	 * @return the result of the operation
	 */
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
