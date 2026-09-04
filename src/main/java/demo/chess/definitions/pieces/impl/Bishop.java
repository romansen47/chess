package demo.chess.definitions.pieces.impl;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.BishopLike;

/**
 * Implementation of the bishop piece in a chess game.
 */
public class Bishop extends PieceImpl implements BishopLike {

	private static final Logger logger = LogManager.getLogger(Bishop.class);

	/**
	 * Creates a new Bishop instance.
	 * @param color the color
	 * @param field the field
	 * @param chessBoard the chess board
	 * @param setField the set field
	 */
	public Bishop(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return getColor().label + "B" + getField().getName();
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
	 * Returns the possible unvalidated moves.
	 * @return the possible unvalidated moves
	 */
	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		return getPossibleUnvalidatedBishopLikeMoves();
	}

	/**
	 * Returns the type.
	 * @return the type
	 */
	@Override
	public PieceType getType() {
		return PieceType.BISHOP;
	}
}
