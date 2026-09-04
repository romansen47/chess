package demo.chess.definitions.players.impl;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.Game;

public class DummyWhitePlayer extends WhitePlayerImpl{

	/**
	 * Creates a new DummyWhitePlayer instance.
	 * @param moveList the move list
	 */
	public DummyWhitePlayer(MoveList moveList) {
		super(moveList, "dummy white player");
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
