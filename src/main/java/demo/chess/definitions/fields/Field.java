package demo.chess.definitions.fields;

import demo.chess.definitions.Color;
import demo.chess.definitions.pieces.Piece;

/**
 * Interface representing a field (square) on a chessboard.
 */
public interface Field {

	/**
	 * Returns the file (column) of the field.
	 *
	 * @return the file of the field
	 */
	int getFile();

	/**
	 * Returns the rank (row) of the field.
	 *
	 * @return the rank of the field
	 */
	int getRank();

	/**
	 * Returns the name of the field in standard chess notation.
	 *
	 * @return the name of the field
	 */
	String getName();

	/**
	 * Returns the color of the field.
	 *
	 * @return the color of the field
	 */
	Color getColor();

	/**
	 * Returns the piece currently on the field.
	 *
	 * @return the piece on the field, or null if the field is empty
	 */
	Piece getPiece();

	/**
	 * Sets the piece on the field.
	 *
	 * @param piece the piece to place on the field
	 */
	void setPiece(Piece piece);
}
