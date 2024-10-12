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
	 * Constructs a ChessMove with the specified piece, source field, and target
	 * field.
	 *
	 * @param piece  the piece being moved
	 * @param source the source field of the piece
	 * @param target the target field of the piece
	 */
	public ChessMove(Piece piece, Field source, Field target) {
		this.piece = piece;
		this.source = source;
		this.target = target;
	}

	@Override
	public Field getSource() {
		return source;
	}

	@Override
	public Field getTarget() {
		return target;
	}

	@Override
	public Piece getPiece() {
		return piece;
	}

	/**
	 * Fuer die website ist das notwendig - nicht entfernen
	 */
	@Override
	public int hashCode() {
		return Objects.hash(piece, source, target);
	}

	/**
	 * Fuer die website ist das notwendig - nicht entfernen
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

	@Override
	public String toString() {
		return source.toString() + target.toString();
	}

	@Override
	public MoveType getMoveType() {
		return MoveType.REGULAR;
	}
}
