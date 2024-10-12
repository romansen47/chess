package demo.chess.definitions;

/**
 * Enum representing the different colors used in a chess game.
 */
public enum Color {

	/** The black color, represented by the label "b". */
	BLACK("b"),

	/** The white color, represented by the label "w". */
	WHITE("w"),

	/** The green color, represented by the label "green". */
	GREEN("green"),

	/** The blue color, represented by the label "blue". */
	BROWN("brown"),

	/** The red color, represented by the label "red". */
	RED("red"),

	/** The blue color, represented by the label "blue". */
	BLUE("blue"),

	/** The yellow color, represented by the label "yellow". */
	YELLOW("yellow");

	/** The label representing the color. */
	public final String label;

	/**
	 * Constructs a Color enum with the specified label.
	 *
	 * @param label the label representing the color
	 */
	private Color(String label) {
		this.label = label;
	}
}
