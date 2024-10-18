package demo.chess.definitions.engines;

public enum Engine {

	STOCKFISH("stockfish"), GNUCHESS("gnuchess"), FRUIT("fruit"), FAIRY("fairy-stockfish");

	private final String label;
	
	Engine(String label) {
		this.label = label;
	}
	
	public String label() {
		return label;
	}
}
