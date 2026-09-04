package demo.chess.definitions.pieces;

import java.util.List;

import org.apache.logging.log4j.Logger;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;

/**
 * Interface representing a chess piece.
 */
public interface Piece {

	/**
	 * Returns the logger.
	 * @return the logger
	 */
	Logger getLogger();

	/**
	 * Returns the type.
	 * @return the type
	 */
	PieceType getType();

	/**
	 * Returns the color.
	 * @return the color
	 */
	Color getColor();

	/**
	 * Returns the possible unvalidated moves.
	 * @return the possible unvalidated moves
	 */
	List<Move> getPossibleUnvalidatedMoves();

	/**
	 * Returns the simple unvalidated moves.
	 * @return the simple unvalidated moves
	 */
	List<Move> getSimpleUnvalidatedMoves();

	/**
	 * Returns the field.
	 * @return the field
	 */
	Field getField();

	/**
	 * Sets the field.
	 * @param target the target
	 */
	void setField(Field target);

	/**
	 * Returns the chess board.
	 * @return the chess board
	 */
	Board getChessBoard();

	/**
	 * Returns the move list.
	 * @return the move list
	 */
	List<Move> getMoveList();

	/**
	 * Sets the move list.
	 * @param moveList the move list
	 */
	void setMoveList(List<Move> moveList);
}
