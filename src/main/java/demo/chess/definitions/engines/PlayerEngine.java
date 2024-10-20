package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

public interface PlayerEngine extends ChessEngine {

	Move getBestMove(Game chessGame, EngineConfig config)
			throws NoMoveFoundException, IOException, InterruptedException, ExecutionException;

	@Override
	void stopEvaluation();
}
