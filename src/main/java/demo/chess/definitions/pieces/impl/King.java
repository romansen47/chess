package demo.chess.definitions.pieces.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.impl.CastlingImpl;
import demo.chess.definitions.moves.impl.ChessMove;
import demo.chess.definitions.pieces.Piece;

/**
 * Implementation of the king piece in a chess game.
 */
public class King extends PieceImpl {

	private static final Logger logger = LogManager.getLogger(King.class);

	/**
	 * Creates a new King instance.
	 * @param color the color
	 * @param field the field
	 * @param chessBoard the chess board
	 * @param setField the set field
	 */
	public King(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return getColor().label + "K" + getField().getName();
	}

	/**
	 * Returns the simple unvalidated moves.
	 * @return the simple unvalidated moves
	 */
	@Override
	public List<Move> getSimpleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		int file = getField().getFile();
		int rank = getField().getRank();
		addMove(file - 1, rank - 1, moveList);
		addMove(file - 1, rank, moveList);
		addMove(file - 1, rank + 1, moveList);
		addMove(file, rank - 1, moveList);
		addMove(file, rank + 1, moveList);
		addMove(file + 1, rank - 1, moveList);
		addMove(file + 1, rank, moveList);
		addMove(file + 1, rank + 1, moveList);
		moveList.addAll(addCastlingMoves());
		return moveList;
	}

	/**
	 * Returns the possible unvalidated moves.
	 * @return the possible unvalidated moves
	 */
	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		List<Move> moveList = getSimpleUnvalidatedMoves();
		moveList.addAll(addCastlingMoves());
		return moveList;
	}

	/**
	 * Adds the castling moves.
	 * @return the result of the operation
	 */
	private List<Move> addCastlingMoves() {
		List<Move> moveList = new ArrayList<>();
		List<Piece> allMovedPieces = this.getMoveList().stream().map(Move::getPiece).distinct()
				.collect(Collectors.toList());
		if (allMovedPieces.contains(this)) {
			return moveList;
		}
		Piece rookA1 = getChessBoard().getField(1, 1).getPiece();
		Piece rookH1 = getChessBoard().getField(8, 1).getPiece();
		Piece rookA8 = getChessBoard().getField(1, 8).getPiece();
		Piece rookH8 = getChessBoard().getField(8, 8).getPiece();

		if (getColor().equals(Color.WHITE)) {
			if (rookA1 != null && rookA1 instanceof Rook) {
				moveList.add(new CastlingImpl(this, (Rook) rookA1));
			}
			if (rookH1 != null && rookH1 instanceof Rook) {
				moveList.add(new CastlingImpl(this, (Rook) rookH1));
			}
		} else {
			if (rookA8 != null && rookA8 instanceof Rook) {
				moveList.add(new CastlingImpl(this, (Rook) rookA8));
			}
			if (rookH8 != null && rookH8 instanceof Rook) {
				moveList.add(new CastlingImpl(this, (Rook) rookH8));
			}
		}
		return moveList;
	}

	/**
	 * Adds the move.
	 * @param i the i
	 * @param j the j
	 * @param moveList the move list
	 */
	private void addMove(int i, int j, List<Move> moveList) {
		if (i > 0 && j > 0 && i < 9 && j < 9) {
			Field targetField = this.getChessBoard().getField(i, j);
			Piece targetPiece = targetField.getPiece();
			if (targetPiece == null || !targetPiece.getColor().equals(this.getColor())) {
				Move move = new ChessMove(this, this.getField(), targetField);
				moveList.add(move);
			}
		}
	}

	/**
	 * Returns the logger.
	 * @return the logger
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Returns the type.
	 * @return the type
	 */
	@Override
	public PieceType getType() {
		return PieceType.KING;
	}
}
