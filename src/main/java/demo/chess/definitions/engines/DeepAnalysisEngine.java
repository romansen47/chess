package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import demo.chess.game.Game;

public interface DeepAnalysisEngine extends EvaluationEngine {

    /**
     * Runs one finite DeepAnalysis search and returns both its final variants
     * and the usable intermediate depth history from that same search.
     *
     * @param chessGame current position
     * @param config engine configuration
     * @return immutable DeepAnalysis result
     */
    DeepAnalysisResult analyze(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException;
}
