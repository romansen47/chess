package demo.chess.definitions.moves;

import demo.chess.definitions.MoveType;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.pieces.Piece;

/**
 * Interface representing a move in a chess game.
 */
public interface Move {

	/**
	 * Returns the source field of the move.
	 *
	 * @return the source field
	 */
	Field getSource();

	/**
	 * Returns the target field of the move.
	 *
	 * @return the target field
	 */
	Field getTarget();

	/**
	 * Returns the piece that is being moved.
	 *
	 * @return the piece being moved
	 */
	Piece getPiece();

	/**
	 * Returns the type of the move.
	 *
	 * @return the move type
	 */
	MoveType getMoveType();
}
