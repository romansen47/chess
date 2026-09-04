package demo.chess.definitions.moves.impl;

import java.util.Objects;

import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;

/**
 * Implementation of a promotion move in a chess game.
 */
public class PromotionImpl extends ChessMove implements Promotion {

	private Piece promotedPiece;

	/**
	 * Creates a new PromotionImpl instance.
	 * @param piece the piece
	 * @param source the source
	 * @param target the target
	 * @param promotedPiece the promoted piece
	 */
	public PromotionImpl(Piece piece, Field source, Field target, Piece promotedPiece) {
		super(piece, source, target);
		this.promotedPiece = promotedPiece;
	}

	/**
	 * Returns the promoted piece.
	 * @return the promoted piece
	 */
	@Override
	public Piece getPromotedPiece() {
		return promotedPiece;
	}

	/**
	 * Sets the promoted piece.
	 * @param promotedPiece the promoted piece
	 */
	@Override
	public void setPromotedPiece(Piece promotedPiece) {
		this.promotedPiece = promotedPiece;
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return super.toString() + getPromotedPiece().getType().label;
	}

	/**
	 * Returns the hash code for this object.
	 * @return true when the condition is satisfied; otherwise false
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(promotedPiece);
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
		PromotionImpl other = (PromotionImpl) obj;
		return Objects.equals(promotedPiece, other.promotedPiece);
	}
}
