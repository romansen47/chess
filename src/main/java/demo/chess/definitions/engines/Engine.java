package demo.chess.definitions.engines;

public enum Engine {

	STOCKFISH_16("stockfish", "Stockfish 16"), STOCKFISH_11("stockfish11", "Stockfish 11"), STOCKFISH_13("stockfish13", "Stockfish 13"), FRUIT("fruit", "Fruit engine"),
	FAIRY("fairy-stockfish", "Fairy Stockfish"), TEXEL("texel", "Texel"), ETHEREAL("ethereal", "Ethereal"), TOGA2("toga2", "Toga 2"), KOMODO("komodo", "old Komodo engine");

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
