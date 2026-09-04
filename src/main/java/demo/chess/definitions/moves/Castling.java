package demo.chess.definitions.moves;

import demo.chess.definitions.pieces.impl.Rook;

/**
 * Interface representing a castling move in a chess game.
 */
public interface Castling extends Move {

	/**
	 * Returns the rook.
	 * @return the rook
	 */
	Rook getRook();
}
