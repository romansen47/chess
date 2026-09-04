package demo.chess.definitions.fields.impl;

import java.util.Objects;

import demo.chess.definitions.Color;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.pieces.Piece;

/**
 * Implementation of a field (square) on a chessboard.
 */
public class ChessField implements Field {

	private final Color color;
	private final int file;
	private final int rank;
	private final String name;
	private Piece piece = null;

	/**
	 * Creates a new ChessField instance.
	 * @param file the file
	 * @param rank the rank
	 */
	public ChessField(int file, int rank) {
		this.file = file;
		this.rank = rank;
		this.name = String.valueOf((char) ('A' - 1 + file)).toLowerCase().concat(String.valueOf(rank));
		this.color = (file + rank) % 2 == 0 ? Color.BLACK : Color.WHITE;
	}

	/**
	 * Returns the piece.
	 * @return the piece
	 */
	@Override
	public Piece getPiece() {
		return piece;
	}

	/**
	 * Sets the piece.
	 * @param piece the piece
	 */
	@Override
	public void setPiece(Piece piece) {
		this.piece = piece;
	}

	/**
	 * Returns the file.
	 * @return the file
	 */
	@Override
	public int getFile() {
		return file;
	}

	/**
	 * Returns the rank.
	 * @return the rank
	 */
	@Override
	public int getRank() {
		return rank;
	}

	/**
	 * Returns the name.
	 * @return the name
	 */
	@Override
	public String getName() {
		return name.toLowerCase();
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
	 * Returns a string representation of this object.
	 * @return the result of the operation
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Returns the hash code for this object.
	 * @return true when the condition is satisfied; otherwise false
	 */
	@Override
	public int hashCode() {
		return Objects.hash(file, rank);
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
		ChessField other = (ChessField) obj;
		return file == other.file && rank == other.rank;
	}
}
