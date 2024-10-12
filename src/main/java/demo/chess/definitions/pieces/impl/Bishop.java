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
	 * Constructs a bishop piece with the specified color, field, and chess board.
	 *
	 * @param color      the color of the bishop
	 * @param field      the field the bishop is placed on
	 * @param chessBoard the chess board the bishop belongs to
	 */
	public Bishop(Color color, Field field, Board chessBoard, boolean setField) {
		super(color, field, chessBoard, setField);
	}

	@Override
	public String toString() {
		return getColor().label + "B" + getField().getName();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public List<Move> getPossibleUnvalidatedMoves() {
		return getPossibleUnvalidatedBishopLikeMoves();
	}

	@Override
	public PieceType getType() {
		return PieceType.BISHOP;
	}
}
