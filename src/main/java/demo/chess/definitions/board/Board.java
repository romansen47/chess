package demo.chess.definitions.board;

import demo.chess.definitions.fields.Field;

/**
 * Interface representing a chessboard.
 */
public interface Board {

	/**
	 * Returns the field.
	 * @param i the i
	 * @param j the j
	 * @return the field
	 */
	Field getField(int i, int j);
}
