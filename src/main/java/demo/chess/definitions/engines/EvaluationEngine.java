package demo.chess.definitions.engines;

import java.io.IOException;
import java.util.List;
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
     * Clears all cached evaluation snapshots owned by this engine instance.
     */
    void clearCachedLines();

    /**
     * Compatibility alias for callers compiled against the former misspelled API.
     *
     * @deprecated use {@link #clearCachedLines()}
     */
    @Deprecated
    default void clearChachedLines() {
        clearCachedLines();
    }
}
