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
	 * Creates a new Knight instance.
	 * @param color the color
	 * @param field the field
	 * @param chessBoard the chess board
	 * @param setField the set field
	 */
	public Knight(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return getColor().label + "N" + getField().getName();
	}

	/**
	 * Returns the possible unvalidated moves.
	 * @return the possible unvalidated moves
	 */
	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		return getSimpleUnvalidatedMoves();
	}

	/**
	 * Processes the move.
	 * @param moveList the move list
	 * @param tmpFile the tmp file
	 * @param tmpRank the tmp rank
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
		return PieceType.KNIGHT;
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
