package demo.chess.definitions.engines;

public enum Engine {

	STOCKFISH_16("stockfish", "Stockfish 16"), FRUIT("fruit", "Fruit engine"),
	FAIRY("fairy-stockfish", "Fairy Stockfish"), TOGA2("toga2", "Toga 2");

	private final String path;
	private final String comment;

	Engine(String path, String comment) {
		this.path = path;
		this.comment = comment;
	}

	public String path() {
		return path;
	}

	public String comment() {
		return comment;
	}
}
