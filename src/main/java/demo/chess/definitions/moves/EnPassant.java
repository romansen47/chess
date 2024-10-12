package demo.chess.definitions.moves;

import demo.chess.definitions.pieces.impl.Pawn;

/**
 * Interface representing an en passant move in a chess game.
 */
public interface EnPassant extends Move {

	/**
	 * Returns the pawn that is captured by the en passant move.
	 *
	 * @return the slayed pawn
	 */
	Pawn getSlayedPiece();
}
