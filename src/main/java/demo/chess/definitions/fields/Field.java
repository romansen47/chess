package demo.chess.definitions.fields;

import demo.chess.definitions.Color;
import demo.chess.definitions.pieces.Piece;

/**
 * Interface representing a field (square) on a chessboard.
 */
public interface Field {

	/**
	 * Returns the file.
	 * @return the file
	 */
	int getFile();

	/**
	 * Returns the rank.
	 * @return the rank
	 */
	int getRank();

	/**
	 * Returns the name.
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the color.
	 * @return the color
	 */
	Color getColor();

	/**
	 * Returns the piece.
	 * @return the piece
	 */
	Piece getPiece();

	/**
	 * Sets the piece.
	 * @param piece the piece
	 */
	void setPiece(Piece piece);
}
