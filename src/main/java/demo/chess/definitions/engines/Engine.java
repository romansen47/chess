package demo.chess.definitions.engines;

public enum Engine {

	STOCKFISH("stockfish"), GNUCHESS("gnuchess");

	private final String label;
	
	Engine(String label) {
		this.label = label;
	}
	
	public String label() {
		return label;
	}
}
