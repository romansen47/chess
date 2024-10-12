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
	 * Constructs a PieceImpl with the specified color, field, and chess board.
	 *
	 * @param color      the color of the piece
	 * @param field      the field the piece is placed on
	 * @param chessBoard the chess board the piece belongs to
	 */
	public PieceImpl(Color color, Field field, Board chessBoard, Boolean setField) {
		this.color = color;
		this.chessBoard = chessBoard;
		if (setField) {
			field.setPiece(this);
		}
		this.field = field;
	}

	@Override
	public Field getField() {
		return field;
	}

	@Override
	public void setField(Field field) {
		this.field = field;
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public Board getChessBoard() {
		return chessBoard;
	}

	@Override
	public List<Move> getMoveList() {
		return moveList;
	}

	@Override
	public void setMoveList(List<Move> moveList) {
		this.moveList = moveList;
	}

	@Override
	public int hashCode() {
		return Objects.hash(color, field);
	}

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
