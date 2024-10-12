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
	 * Constructs an EnPassantImpl with the specified piece, source field, target
	 * field, and the slayed pawn.
	 *
	 * @param piece      the pawn performing the en passant
	 * @param source     the source field of the pawn
	 * @param target     the target field of the pawn
	 * @param slayedPawn the pawn that is captured by the en passant move
	 */
	public EnPassantImpl(Piece piece, Field source, Field target, Pawn slayedPawn) {
		super(piece, source, target);
		this.slayedPawn = slayedPawn;
	}

	@Override
	public Pawn getSlayedPiece() {
		return slayedPawn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(slayedPawn);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Move) {
			Move other = (Move) obj;
			return other.getSource().equals(getSource()) && other.getTarget().equals(getTarget())
					&& other.getPiece().equals(getPiece());
		}
		return Objects.equals(obj, this);
	}

	@Override
	public MoveType getMoveType() {
		return MoveType.EN_PASSANT;
	}
}
