package demo.chess.definitions.engines.impl;

public class NoMoveFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new NoMoveFoundException instance.
	 * @param s the s
	 */
	public NoMoveFoundException(String s) {
		super(s);
	}
}