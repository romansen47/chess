package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;

import demo.chess.game.Game;

public interface EvaluationEngine extends ChessEngine {

	List<Pair<Double, String>> getBestLines(Game chessgame, EngineConfig config) throws IOException, InterruptedException, ExecutionException;


}
