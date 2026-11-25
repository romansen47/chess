package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import demo.chess.game.Game;

public interface PositionAnalysisEngine extends EvaluationEngine {

    /**
     * Runs one finite position-analysis search and returns both its final variants
     * and the usable intermediate depth history from that same search.
     *
     * @param chessGame current position
     * @param config engine configuration
     * @return immutable position-analysis result
     */
    PositionAnalysisResult analyze(Game chessGame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException;
}
