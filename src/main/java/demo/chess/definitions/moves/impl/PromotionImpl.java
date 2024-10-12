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
	 * Constructs a PromotionImpl with the specified piece, source field, and target
	 * field.
	 *
	 * @param piece  the pawn being promoted
	 * @param source the source field of the pawn
	 * @param target the target field of the pawn
	 */
	public PromotionImpl(Piece piece, Field source, Field target, Piece promotedPiece) {
		super(piece, source, target);
		this.promotedPiece = promotedPiece;
	}

	@Override
	public Piece getPromotedPiece() {
		return promotedPiece;
	}

	@Override
	public void setPromotedPiece(Piece promotedPiece) {
		this.promotedPiece = promotedPiece;
	}

	@Override
	public String toString() {
		return super.toString()+getPromotedPiece().getType().label;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(promotedPiece);
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
		PromotionImpl other = (PromotionImpl) obj;
		return Objects.equals(promotedPiece, other.promotedPiece);
	}
}
