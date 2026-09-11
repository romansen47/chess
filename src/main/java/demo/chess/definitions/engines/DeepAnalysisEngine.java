package demo.chess.definitions.engines;

import java.util.List;
import java.util.Map;

public interface DeepAnalysisEngine extends EvaluationEngine {

    /**
     * Returns the completed depth snapshots collected during the most recent
     * finite deep-analysis search. The map key is the UCI search depth.
     *
     * <p>Live/infinite evaluation does not use this history.</p>
     *
     * @return immutable depth-to-lines snapshots, or an empty map
     */
    default Map<Integer, List<EngineLine>> getLastDepthHistory() {
        return Map.of();
    }
}
