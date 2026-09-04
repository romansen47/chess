package demo.chess.definitions.pieces.impl;

import java.util.List;
import java.util.Objects;

import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;

/**
 * Abstract implementation of a chess piece.
 */
public abstract class PieceImpl implements Piece {

	private final Color color;
	private Field field;
	private List<Move> moveList;
	private final Board chessBoard;

	/**
	 * Creates a new PieceImpl instance.
	 * @param color the color
	 * @param field the field
	 * @param chessBoard the chess board
	 * @param setField the set field
	 */
	public PieceImpl(Color color, Field field, Board chessBoard, Boolean setField) {
		this.color = color;
		this.chessBoard = chessBoard;
		if (setField) {
			field.setPiece(this);
		}
		this.field = field;
	}

	/**
	 * Returns the field.
	 * @return the field
	 */
	@Override
	public Field getField() {
		return field;
	}

	/**
	 * Sets the field.
	 * @param field the field
	 */
	@Override
	public void setField(Field field) {
		this.field = field;
	}

	/**
	 * Returns the color.
	 * @return the color
	 */
	@Override
	public Color getColor() {
		return color;
	}

	/**
	 * Returns the chess board.
	 * @return the chess board
	 */
	@Override
	public Board getChessBoard() {
		return chessBoard;
	}

	/**
	 * Returns the move list.
	 * @return the move list
	 */
	@Override
	public List<Move> getMoveList() {
		return moveList;
	}

	/**
	 * Sets the move list.
	 * @param moveList the move list
	 */
	@Override
	public void setMoveList(List<Move> moveList) {
		this.moveList = moveList;
	}

	/**
	 * Returns the hash code for this object.
	 * @return true when the condition is satisfied; otherwise false
	 */
	@Override
	public int hashCode() {
		return Objects.hash(color, field);
	}

	/**
	 * Compares this object with another object for equality.
	 * @param obj the obj
	 * @return the result of the operation
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		PieceImpl other = (PieceImpl) obj;
		return color == other.color && Objects.equals(field, other.field) && getType().equals(other.getType());
	}
}
