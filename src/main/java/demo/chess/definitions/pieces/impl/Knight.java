package demo.chess.definitions.pieces.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.impl.ChessMove;

/**
 * Implementation of the knight piece in a chess game.
 */
public class Knight extends PieceImpl {

	private static final Logger logger = LogManager.getLogger(Knight.class);

	/**
	 * Constructs a knight piece with the specified color, field, and chess board.
	 *
	 * @param color      the color of the knight
	 * @param field      the field the knight is placed on
	 * @param chessBoard the chess board the knight belongs to
	 */
	public Knight(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	@Override
	public String toString() {
		return getColor().label + "N" + getField().getName();
	}

	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		return getSimpleUnvalidatedMoves();
	}

	/**
	 * Adds a move to the move list if it is within the bounds of the board and
	 * either the target field is empty or contains an opponent's piece.
	 *
	 * @param moveList the list of moves to add to
	 * @param tmpFile  the file of the potential move
	 * @param tmpRank  the rank of the potential move
	 */
	private void processMove(List<Move> moveList, int tmpFile, int tmpRank) {
		if (tmpFile > 0 && tmpFile < 9 && tmpRank > 0 && tmpRank < 9) {
			Field targetField = getChessBoard().getField(tmpFile, tmpRank);
			if (targetField.getPiece() == null || !getColor().equals(targetField.getPiece().getColor())) {
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
		return PieceType.KNIGHT;
	}

	@Override
	public List<Move> getSimpleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		int file = getField().getFile();
		int rank = getField().getRank();
		processMove(moveList, file + 1, rank + 2);
		processMove(moveList, file + 1, rank - 2);
		processMove(moveList, file + 2, rank + 1);
		processMove(moveList, file + 2, rank - 1);
		processMove(moveList, file - 1, rank + 2);
		processMove(moveList, file - 1, rank - 2);
		processMove(moveList, file - 2, rank + 1);
		processMove(moveList, file - 2, rank - 1);
		return moveList;
	}
}
