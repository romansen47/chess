package demo.chess.definitions.states;

public enum State {

	WHITE_MATED("white_mated"), BLACK_MATED("black_mated"), STALEMATE("stalemate"), WHITE_RESIGNED("white_resigned"), BLACK_RESIGNED("black_resigned"), LOST_ON_TIME("lost on time");

	/** The label representing the state. */
	private final String label;

	/**
	 * Constructs a State enum with the specified label.
	 *
	 * @param label the label representing the state
	 */
	private State(String label) {
		this.label = label;
	}
}
