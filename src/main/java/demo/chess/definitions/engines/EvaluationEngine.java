package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import demo.chess.game.Game;

public interface EvaluationEngine extends ChessEngine {

    /**
     * Returns the best lines.
     * @param chessgame the chessgame
     * @param config the config
     * @return the best lines
     */
    List<EngineLine> getBestLines(Game chessgame, EngineConfig config)
            throws IOException, InterruptedException, ExecutionException;

    /**
     * Clears the chached lines.
     */
    void clearChachedLines();

    /**
     * Returns the cached best lines.
     * @return the cached best lines
     */
    Map<String, List<EngineLine>> getCachedBestLines();
}
