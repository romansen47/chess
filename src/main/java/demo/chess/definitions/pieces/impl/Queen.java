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
import demo.chess.definitions.pieces.BishopLike;
import demo.chess.definitions.pieces.RookLike;

/**
 * Implementation of the queen piece in a chess game.
 */
public class Queen extends PieceImpl implements BishopLike, RookLike {

	private static final Logger logger = LogManager.getLogger(Queen.class);

	/**
	 * Creates a new Queen instance.
	 * @param color the color
	 * @param field the field
	 * @param chessBoard the chess board
	 * @param setField the set field
	 */
	public Queen(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	/**
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return getColor().label + "Q" + getField().getName();
	}

	/**
	 * Returns the possible unvalidated moves.
	 * @return the possible unvalidated moves
	 */
	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		moveList.addAll(getSimpleUnvalidatedMoves());
		return moveList;
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
		return PieceType.QUEEN;
	}

	/**
	 * Returns the simple unvalidated moves.
	 * @return the simple unvalidated moves
	 */
	@Override
	public List<Move> getSimpleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		moveList.addAll(BishopLike.super.getSimpleUnvalidatedMoves());
		moveList.addAll(RookLike.super.getSimpleUnvalidatedMoves());
		return moveList;
	}
}
