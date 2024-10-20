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
	 * Constructs a king piece with the specified color, field, and chess board.
	 *
	 * @param color      the color of the king
	 * @param field      the field the king is placed on
	 * @param chessBoard the chess board the king belongs to
	 */
	public King(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	@Override
	public String toString() {
		return getColor().label + "K" + getField().getName();
	}

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

	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		List<Move> moveList = getSimpleUnvalidatedMoves();
		moveList.addAll(addCastlingMoves());
		return moveList;
	}

	/**
	 * Adds castling moves to the move list if they are valid.
	 *
	 * @return the list of castling moves
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
	 * Adds a move to the move list if it is within the bounds of the board and
	 * either the target field is empty or contains an opponent's piece.
	 *
	 * @param i        the file of the potential move
	 * @param j        the rank of the potential move
	 * @param moveList the list of moves to add to
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

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public PieceType getType() {
		return PieceType.KING;
	}
}
