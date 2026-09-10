package demo.chess.definitions.players.impl;

import java.io.IOException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.Game;

/**
 * Black player for trusted lightweight replays.
 *
 * <p>The simulation step is intentionally skipped. Consequently the move list
 * returned by {@code getValidMoves()} must be treated as cheaply generated move
 * candidates, not as proof that every move preserves king safety.</p>
 */
public class DummyBlackPlayer extends BlackPlayerImpl {

	/**
	 * Creates a new DummyBlackPlayer instance.
	 * @param moveList the move list
	 */
	public DummyBlackPlayer(MoveList moveList) {
		super(moveList, "Dummy black player");
	}

	/**
	 * Skips expensive per-candidate legality simulation for trusted replays.
	 * @param chessGame the chess game
	 * @param move the move
	 * @return always true
	 */
	@Override
	protected boolean simulate(Game chessGame, Move move) throws NoMoveFoundException, IOException {
		return true;
	}
}
