package demo.chess.definitions.board;

import demo.chess.definitions.fields.Field;

/**
 * Interface representing a chessboard.
 */
public interface Board {

	/**
	 * Returns the field at the specified file and rank.
	 *
	 * @param i the file (column) of the field
	 * @param j the rank (row) of the field
	 * @return the field at the specified coordinates
	 */
	Field getField(int i, int j);
}
