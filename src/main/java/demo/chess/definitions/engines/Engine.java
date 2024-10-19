package demo.chess.definitions.engines;

public enum Engine {

	STOCKFISH("stockfish"), FRUIT("fruit"), FAIRY("fairy-stockfish"), TEXEL("texel"), ETHEREAL("ethereal"), TOGA2("toga2");// GNUCHESS("gnuchessu");

	private final String label;
	
	Engine(String label) {
		this.label = label;
	}
	
	public String label() {
		return label;
	}
}
