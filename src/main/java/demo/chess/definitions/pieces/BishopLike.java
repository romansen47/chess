package demo.chess.definitions.pieces;

import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.impl.ChessMove;

/**
 * Interface representing a piece with bishop-like movement capabilities.
 * <p>
 * This interface extends the {@link Piece} interface and provides default
 * methods for calculating bishop-like moves on a chessboard.
 * </p>
 */
public interface BishopLike extends Piece {

	/**
	 * Returns a list of simple unvalidated moves for the piece.
	 * <p>
	 * This method calculates moves in the four diagonal directions, stopping at the
	 * edge of the board or when encountering another piece.
	 * </p>
	 *
	 * @return the list of simple unvalidated moves
	 */
	@Override
	default List<Move> getSimpleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		int file = getField().getFile();
		int rank = getField().getRank();
		boolean cont;

		// Moves in the top-right diagonal direction
		cont = true;
		for (int tmp = 1; file + tmp < 9 && rank + tmp < 9; tmp++) {
			Field targetField = getChessBoard().getField(file + tmp, rank + tmp);
			Piece targetPiece = targetField.getPiece();
			if (cont && targetPiece == null) {
				Move move = new ChessMove(this, getField(), targetField);
				moveList.add(move);
			} else {
				if (cont && !targetPiece.getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetField);
					moveList.add(move);
				}
				cont = false;
			}
		}

		// Moves in the top-left diagonal direction
		cont = true;
		for (int tmp = 1; file - tmp > 0 && rank + tmp < 9; tmp++) {
			Field targetField = getChessBoard().getField(file - tmp, rank + tmp);
			Piece targetPiece = targetField.getPiece();
			if (cont && targetPiece == null) {
				Move move = new ChessMove(this, getField(), targetField);
				moveList.add(move);
			} else {
				if (cont && !targetPiece.getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetField);
					moveList.add(move);
				}
				cont = false;
			}
		}

		// Moves in the bottom-right diagonal direction
		cont = true;
		for (int tmp = 1; file + tmp < 9 && rank - tmp > 0; tmp++) {
			Field targetField = getChessBoard().getField(file + tmp, rank - tmp);
			Piece targetPiece = targetField.getPiece();
			if (cont && targetPiece == null) {
				Move move = new ChessMove(this, getField(), targetField);
				moveList.add(move);
			} else {
				if (cont && !targetPiece.getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetField);
					moveList.add(move);
				}
				cont = false;
			}
		}

		// Moves in the bottom-left diagonal direction
		cont = true;
		for (int tmp = 1; file - tmp > 0 && rank - tmp > 0; tmp++) {
			Field targetField = getChessBoard().getField(file - tmp, rank - tmp);
			Piece targetPiece = targetField.getPiece();
			if (cont && targetPiece == null) {
				Move move = new ChessMove(this, getField(), targetField);
				moveList.add(move);
			} else {
				if (cont && !targetPiece.getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), targetField);
					moveList.add(move);
				}
				cont = false;
			}
		}

		return moveList;
	}

	/**
	 * Returns a list of possible unvalidated bishop-like moves for the piece.
	 *
	 * @return the list of possible unvalidated bishop-like moves
	 */
	default List<Move> getPossibleUnvalidatedBishopLikeMoves() {
		return getSimpleUnvalidatedMoves();
	}
}
