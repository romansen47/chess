package demo.chess.definitions;

/**
 * Enum representing the different types of chess pieces.
 */
public enum PieceType {

	/** The pawn chess piece. */
	PAWN("p"),

	/** The knight chess piece. */
	KNIGHT("n"),

	/** The bishop chess piece. */
	BISHOP("b"),

	/** The queen chess piece. */
	QUEEN("q"),

	/** The king chess piece. */
	KING("k"),

	/** The rook chess piece. */
	ROOK("r");


	/** The label representing the color. */
	public final String label;

	/**
	 * Constructs a PieceType enum with the specified label.
	 *
	 * @param label the label representing the PieceType
	 */
	private PieceType(String label) {
		this.label = label;
	}
}
