package demo.chess.definitions.players.impl;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.Game;

public class DummyBlackPlayer extends BlackPlayerImpl{

	/**
	 * Creates a new DummyBlackPlayer instance.
	 * @param moveList the move list
	 */
	public DummyBlackPlayer(MoveList moveList) {
		super(moveList, "Dummy black player");
	}

	/**
	 * Performs the simulate operation.
	 * @param chessGame the chess game
	 * @param move the move
	 * @return the result of the operation
	 */
	@Override
	protected boolean simulate(Game chessGame, Move move) throws NoMoveFoundException, IOException {
		return true;
	}

}
