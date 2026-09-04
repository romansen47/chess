package demo.chess.definitions.moves.impl;

import java.util.Objects;

import demo.chess.definitions.MoveType;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Pawn;

/**
 * Implementation of an en passant move in a chess game.
 */
public class EnPassantImpl extends ChessMove implements EnPassant {

	private final Pawn slayedPawn;

	/**
	 * Creates a new EnPassantImpl instance.
	 * @param piece the piece
	 * @param source the source
	 * @param target the target
	 * @param slayedPawn the slayed pawn
	 */
	public EnPassantImpl(Piece piece, Field source, Field target, Pawn slayedPawn) {
		super(piece, source, target);
		this.slayedPawn = slayedPawn;
	}

	/**
	 * Returns the slayed piece.
	 * @return the slayed piece
	 */
	@Override
	public Pawn getSlayedPiece() {
		return slayedPawn;
	}

	/**
	 * Returns the hash code for this object.
	 * @return true when the condition is satisfied; otherwise false
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(slayedPawn);
		return result;
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
	 * Returns the move type.
	 * @return the move type
	 */
	@Override
	public MoveType getMoveType() {
		return MoveType.EN_PASSANT;
	}
}
