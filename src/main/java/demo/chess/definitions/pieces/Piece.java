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
	 * Returns the logger for the piece.
	 *
	 * @return the logger
	 */
	Logger getLogger();

	/**
	 * Returns the type of the piece.
	 *
	 * @return the piece type
	 */
	PieceType getType();

	/**
	 * Returns the color of the piece.
	 *
	 * @return the color of the piece
	 */
	Color getColor();

	/**
	 * Returns a list of possible unvalidated moves for the piece.
	 *
	 * @return the list of possible unvalidated moves
	 */
	List<Move> getPossibleUnvalidatedMoves();

	/**
	 * Returns a list of simple unvalidated moves for the piece.
	 *
	 * @return the list of simple unvalidated moves
	 */
	List<Move> getSimpleUnvalidatedMoves();

	/**
	 * Returns the field the piece is currently on.
	 *
	 * @return the field
	 */
	Field getField();

	/**
	 * Sets the field the piece is on.
	 *
	 * @param target the target field
	 */
	void setField(Field target);

	/**
	 * Returns the chessboard the piece belongs to.
	 *
	 * @return the chessboard
	 */
	Board getChessBoard();

	/**
	 * Returns the move list for the piece.
	 *
	 * @return the move list
	 */
	List<Move> getMoveList();

	/**
	 * Sets the move list for the piece.
	 *
	 * @param moveList the move list to set
	 */
	void setMoveList(List<Move> moveList);
}
