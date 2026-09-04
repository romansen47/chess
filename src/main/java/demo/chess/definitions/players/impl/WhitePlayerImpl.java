package demo.chess.definitions.players.impl;

import demo.chess.definitions.Color;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.players.WhitePlayer;

/**
 * Implementation of a white player in a chess game.
 */
public class WhitePlayerImpl extends PlayerImpl implements WhitePlayer {

	/**
	 * Creates a new WhitePlayerImpl instance.
	 * @param moveList the move list
	 * @param string the string
	 */
	public WhitePlayerImpl(MoveList moveList, String string) {
		super(Color.WHITE, moveList, string);
	}
}
