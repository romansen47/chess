package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

public interface PlayerEngine extends ChessEngine {

	/**
	 * Returns the best move.
	 * @param chessGame the chess game
	 * @param config the config
	 * @return the best move
	 */
	Move getBestMove(Game chessGame, EngineConfig config)
			throws NoMoveFoundException, IOException, InterruptedException, ExecutionException;

	/**
	 * Stops the evaluation.
	 */
	@Override
	void stopEvaluation();
}
