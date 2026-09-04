package demo.chess.definitions.players.impl;

import demo.chess.definitions.Color;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.BlackPlayer;

/**
 * Implementation of the {@link BlackPlayer} interface representing a black
 * player in a chess game.
 */
public class BlackPlayerImpl extends PlayerImpl implements BlackPlayer {

	/**
	 * Creates a new BlackPlayerImpl instance.
	 * @param moveList the move list
	 * @param string the string
	 */
	public BlackPlayerImpl(MoveList moveList, String string) {
		super(Color.BLACK, moveList, string);
	}

}
