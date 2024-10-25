package demo.chess.definitions.states;

public enum State {

	WHITE_MATED("white mated"), BLACK_MATED("black mated"), STALEMATE("stalemate"), WHITE_RESIGNED("white resigned"),
	BLACK_RESIGNED("black resigned"), LOST_ON_TIME("lost on time"), DRAW_BY_50_MOVES_RULE("Draw by 50 moves rule"), DRAW_BY_THREEFOLD_REPETITION("Draw by threefold repetition");

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
	
	public String getLabel() {
		return label;
	}
}
