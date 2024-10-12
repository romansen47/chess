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
	 * Constructs a BlackPlayerImpl instance with the specified move list.
	 *
	 * @param moveList the move list associated with the player
	 * @param string
	 */
	public BlackPlayerImpl(MoveList moveList, String string) {
		super(Color.BLACK, moveList, string);
	}
}
