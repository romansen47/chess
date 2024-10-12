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
	 * Constructs a queen piece with the specified color, field, and chess board.
	 *
	 * @param color      the color of the queen
	 * @param field      the field the queen is placed on
	 * @param chessBoard the chess board the queen belongs to
	 */
	public Queen(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	@Override
	public String toString() {
		return getColor().label + "Q" + getField().getName();
	}

	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		moveList.addAll(getPossibleUnvalidatedRookLikeMoves());
		moveList.addAll(getPossibleUnvalidatedBishopLikeMoves());
		return moveList;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public PieceType getType() {
		return PieceType.QUEEN;
	}

	@Override
	public List<Move> getSimpleUnvalidatedMoves() {
		List<Move> moveList = new ArrayList<>();
		moveList.addAll(BishopLike.super.getSimpleUnvalidatedMoves());
		moveList.addAll(RookLike.super.getSimpleUnvalidatedMoves());
		return moveList;
	}
}
