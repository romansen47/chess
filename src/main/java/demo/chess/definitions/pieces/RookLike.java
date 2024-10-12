package demo.chess.definitions.pieces;

import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.impl.ChessMove;

/**
 * Interface representing a piece with rook-like movement capabilities.
 * <p>
 * This interface extends the {@link Piece} interface and provides default
 * methods for calculating rook-like moves on a chessboard.
 * </p>
 */
public interface RookLike extends Piece {

	/**
	 * Returns a list of possible unvalidated rook-like moves for the piece.
	 *
	 * @return the list of possible unvalidated rook-like moves
	 */
	default List<Move> getPossibleUnvalidatedRookLikeMoves() {
		return getSimpleUnvalidatedMoves();
	}

	/**
	 * Returns a list of simple unvalidated moves for the piece.
	 * <p>
	 * This method calculates moves in the four cardinal directions: up, down, left,
	 * and right, stopping at the edge of the board or when encountering another
	 * piece.
	 * </p>
	 *
	 * @return the list of simple unvalidated moves
	 */
	@Override
	default List<Move> getSimpleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		boolean cont = true;
		int file = getField().getFile();

		// Moves in the upward direction
		for (int tmpRank = getField().getRank() + 1; tmpRank < 9; tmpRank++) {
			Field target = getChessBoard().getField(file, tmpRank);
			if (cont && target.getPiece() == null) {
				Move move = new ChessMove(this, getField(), target);
				moveList.add(move);
			} else {
				if (cont && !target.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), target);
					moveList.add(move);
				}
				cont = false;
			}
		}

		// Moves in the downward direction
		cont = true;
		for (int tmpRank = getField().getRank() - 1; tmpRank > 0; tmpRank--) {
			Field target = getChessBoard().getField(file, tmpRank);
			if (cont && target.getPiece() == null) {
				Move move = new ChessMove(this, getField(), target);
				moveList.add(move);
			} else {
				if (cont && !target.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), target);
					moveList.add(move);
				}
				cont = false;
			}
		}

		// Moves in the right direction
		cont = true;
		int rank = getField().getRank();
		for (int tmpFile = getField().getFile() + 1; tmpFile < 9; tmpFile++) {
			Field target = getChessBoard().getField(tmpFile, rank);
			if (cont && target.getPiece() == null) {
				Move move = new ChessMove(this, getField(), target);
				moveList.add(move);
			} else {
				if (cont && !target.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), target);
					moveList.add(move);
				}
				cont = false;
			}
		}

		// Moves in the left direction
		cont = true;
		for (int tmpFile = getField().getFile() - 1; tmpFile > 0; tmpFile--) {
			Field target = getChessBoard().getField(tmpFile, rank);
			if (cont && target.getPiece() == null) {
				Move move = new ChessMove(this, getField(), target);
				moveList.add(move);
			} else {
				if (cont && !target.getPiece().getColor().equals(this.getColor())) {
					Move move = new ChessMove(this, getField(), target);
					moveList.add(move);
				}
				cont = false;
			}
		}

		return moveList;
	}
}
