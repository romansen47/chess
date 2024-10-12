package demo.chess.definitions.moves;

import demo.chess.definitions.pieces.Piece;

/**
 * Interface representing a promotion move in a chess game.
 */
public interface Promotion extends Move {

	/**
	 * Returns the piece that the pawn is promoted to.
	 *
	 * @return the promoted piece
	 */
	Piece getPromotedPiece();
	void setPromotedPiece(Piece piece);
}