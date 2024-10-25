package demo.chess.definitions;

/**
 * Enum representing the different types of chess pieces.
 */
public enum PieceType {

	/** The pawn chess piece. */
	PAWN("p", 1),

	/** The knight chess piece. */
	KNIGHT("n", 2),

	/** The bishop chess piece. */
	BISHOP("b", 3),

	/** The rook chess piece. */
	ROOK("r", 4),

	/** The queen chess piece. */
	QUEEN("q", 5),

	/** The king chess piece. */
	KING("k", 6);

	/** The label representing the color. */
	public final String label;
	
	public final long hash;
	/**
	 * Constructs a PieceType enum with the specified label.
	 *
	 * @param label the label representing the PieceType
	 */
	private PieceType(String label, long hash) {
		this.label = label;
		this.hash = hash;
	}
	
	public long hash() {
		return hash;
	}
	
}
