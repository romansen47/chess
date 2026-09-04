package demo.chess.definitions.moves;

import demo.chess.definitions.pieces.Piece;

/**
 * Interface representing a promotion move in a chess game.
 */
public interface Promotion extends Move {

	/**
	 * Returns the promoted piece.
	 * @return the promoted piece
	 */
	Piece getPromotedPiece();

	/**
	 * Sets the promoted piece.
	 * @param piece the piece
	 */
	void setPromotedPiece(Piece piece);
}