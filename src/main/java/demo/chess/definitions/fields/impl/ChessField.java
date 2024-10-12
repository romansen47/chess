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
	 * Constructs a ChessField with the specified file and rank.
	 *
	 * @param file the file (column) of the field
	 * @param rank the rank (row) of the field
	 */
	public ChessField(int file, int rank) {
		this.file = file;
		this.rank = rank;
		this.name = String.valueOf((char) ('A' - 1 + file)).toLowerCase().concat(String.valueOf(rank));
		this.color = (file + rank) % 2 == 0 ? Color.BLACK : Color.WHITE;
	}

	@Override
	public Piece getPiece() {
		return piece;
	}

	@Override
	public void setPiece(Piece piece) {
		this.piece = piece;
	}

	@Override
	public int getFile() {
		return file;
	}

	@Override
	public int getRank() {
		return rank;
	}

	@Override
	public String getName() {
		return name.toLowerCase();
	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(file, rank);
	}

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
