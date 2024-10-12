package demo.chess.definitions.pieces.impl;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.RookLike;

/**
 * Implementation of the rook piece in a chess game.
 */
public class Rook extends PieceImpl implements RookLike {

	private static final Logger logger = LogManager.getLogger(Rook.class);

	/**
	 * Constructs a rook piece with the specified color, field, and chess board.
	 *
	 * @param color      the color of the rook
	 * @param field      the field the rook is placed on
	 * @param chessBoard the chess board the rook belongs to
	 */
	public Rook(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	@Override
	public String toString() {
		return getColor().label + "R" + getField().getName();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		return getPossibleUnvalidatedRookLikeMoves();
	}

	@Override
	public PieceType getType() {
		return PieceType.ROOK;
	}
}
