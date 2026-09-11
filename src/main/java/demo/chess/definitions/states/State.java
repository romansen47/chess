package demo.chess.definitions.states;

public enum State {

	WHITE_MATED("white mated"),
	BLACK_MATED("black mated"),
	STALEMATE("stalemate"),
	WHITE_RESIGNED("white resigned"),
	BLACK_RESIGNED("black resigned"),
	LOST_ON_TIME("lost on time"),
	DRAW_BY_50_MOVES_RULE("Draw by fifty-move rule"),
	DRAW_BY_THREEFOLD_REPETITION("Draw by threefold repetition"),
	DRAW_BY_INSUFFICIENT_MATERIAL("Draw by insufficient mating material");

	/**
	 * Creates a new State instance.
	 * @param label the label
	 */
	private State(String label) {
		this.label = label;
	}

	/** The label representing the state. */
	private final String label;

	/**
	 * Returns the label.
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
}
