package demo.chess.definitions.engines;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable result of one finite DeepAnalysis search.
 *
 * <p>The final lines are the variants selected at the highest usable depth.
 * depthHistory contains usable snapshots from intermediate search depths.
 * Both parts belong to the same engine search and therefore cannot get out of
 * sync as they could with a separate "last history" side channel.</p>
 */
public final class DeepAnalysisResult {

    private final List<EngineLine> finalLines;
    private final Map<Integer, List<EngineLine>> depthHistory;

    public DeepAnalysisResult(
            List<EngineLine> finalLines,
            Map<Integer, List<EngineLine>> depthHistory) {
        this.finalLines = finalLines == null
                ? List.of()
                : List.copyOf(finalLines);

        TreeMap<Integer, List<EngineLine>> sorted = new TreeMap<>();
        if (depthHistory != null) {
            for (Map.Entry<Integer, List<EngineLine>> entry : depthHistory.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                sorted.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
        }

        Map<Integer, List<EngineLine>> ordered = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<EngineLine>> entry : sorted.entrySet()) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        this.depthHistory = Collections.unmodifiableMap(ordered);
    }

    public static DeepAnalysisResult empty() {
        return new DeepAnalysisResult(List.of(), Map.of());
    }

    public List<EngineLine> getFinalLines() {
        return finalLines;
    }

    public Map<Integer, List<EngineLine>> getDepthHistory() {
        return depthHistory;
    }
}
