package demo.chess.definitions.moves;

import demo.chess.definitions.pieces.impl.Pawn;

/**
 * Interface representing an en passant move in a chess game.
 */
public interface EnPassant extends Move {

	/**
	 * Returns the slayed piece.
	 * @return the slayed piece
	 */
	Pawn getSlayedPiece();
}
