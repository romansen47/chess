package demo.chess.definitions.moves.impl;

import java.util.Objects;

import demo.chess.definitions.MoveType;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;

/**
 * Implementation of a regular move in a chess game.
 */
public class ChessMove implements Move {

	private final Field source;
	private final Field target;
	private final Piece piece;

	/**
	 * Creates a new ChessMove instance.
	 * @param piece the piece
	 * @param source the source
	 * @param target the target
	 */
	public ChessMove(Piece piece, Field source, Field target) {
		this.piece = piece;
		this.source = source;
		this.target = target;
	}

	/**
	 * Returns the source.
	 * @return the source
	 */
	@Override
	public Field getSource() {
		return source;
	}

	/**
	 * Returns the target.
	 * @return the target
	 */
	@Override
	public Field getTarget() {
		return target;
	}

	/**
	 * Returns the piece.
	 * @return the piece
	 */
	@Override
	public Piece getPiece() {
		return piece;
	}

	/**
	 * Returns the hash code for this object.
	 * @return true when the condition is satisfied; otherwise false
	 */
	@Override
	public int hashCode() {
		return Objects.hash(piece, source, target);
	}

	/**
	 * Compares this object with another object for equality.
	 * @param obj the obj
	 * @return the result of the operation
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Move) {
			Move other = (Move) obj;
			return other.getSource().equals(getSource()) && other.getTarget().equals(getTarget())
					&& other.getPiece().equals(getPiece());
		}
		return Objects.equals(obj, this);
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return source.toString() + target.toString();
	}

	/**
	 * Returns the move type.
	 * @return the move type
	 */
	@Override
	public MoveType getMoveType() {
		return MoveType.REGULAR;
	}
}
