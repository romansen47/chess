package demo.chess.definitions.moves;

import demo.chess.definitions.MoveType;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.pieces.Piece;

/**
 * Interface representing a move in a chess game.
 */
public interface Move {

	/**
	 * Returns the source.
	 * @return the source
	 */
	Field getSource();

	/**
	 * Returns the target.
	 * @return the target
	 */
	Field getTarget();

	/**
	 * Returns the piece.
	 * @return the piece
	 */
	Piece getPiece();

	/**
	 * Returns the move type.
	 * @return the move type
	 */
	MoveType getMoveType();
}
