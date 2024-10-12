package demo.chess.definitions.board.impl;

import java.util.HashMap;
import java.util.Map;

import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.fields.impl.ChessField;

/**
 * Implementation of a chessboard.
 */
public class ChessBoard implements Board {

	final Map<Integer, Map<Integer, Field>> fields;

	/**
	 * Constructs a ChessBoard with an 8x8 grid of ChessField objects.
	 */
	public ChessBoard() {
		this.fields = new HashMap<>();
		for (int i = 1; i < 9; i++) {
			Map<Integer, Field> rank = new HashMap<>();
			for (int j = 1; j < 9; j++) {
				rank.put(j, new ChessField(i, j));
			}
			fields.put(i, rank);
		}
	}

	@Override
	public Field getField(int i, int j) {
		return fields.get(i).get(j);
	}
}
