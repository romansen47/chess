package demo.chess.definitions.engines;

public enum Engine {

	STOCKFISH_16("stockfish", "Stockfish 16"), FRUIT("fruit", "Fruit engine"),
	FAIRY("fairy-stockfish", "Fairy Stockfish"), TOGA2("toga2", "Toga 2");

	private final String path;
	private final String comment;

	/**
	 * Creates a new Engine instance.
	 * @param path the path
	 * @param comment the comment
	 */
	Engine(String path, String comment) {
		this.path = path;
		this.comment = comment;
	}

	/**
	 * Performs the path operation.
	 * @return the result of the operation
	 */
	public String path() {
		return path;
	}

	/**
	 * Performs the comment operation.
	 * @return the result of the operation
	 */
	public String comment() {
		return comment;
	}
}
