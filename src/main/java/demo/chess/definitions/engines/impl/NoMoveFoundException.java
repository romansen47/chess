package demo.chess.definitions.engines.impl;

public class NoMoveFoundException extends Exception{

	private static final long serialVersionUID = 1L;

	public NoMoveFoundException(String s) {
		super(s);
	}
}