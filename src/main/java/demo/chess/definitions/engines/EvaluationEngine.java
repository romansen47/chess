package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;

import demo.chess.game.Game;

public interface EvaluationEngine extends ChessEngine {

	List<Pair<Pair<Double, Integer>, String>> getBestLines(Game chessgame, EngineConfig config)
			throws IOException, InterruptedException, ExecutionException;

	void clearChachedLines();

	Map<String, List<Pair<Pair<Double, Integer>, String>>> getCachedBestLines();
}
