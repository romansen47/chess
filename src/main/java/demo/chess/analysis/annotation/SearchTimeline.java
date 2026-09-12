package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;

/**
 * Small shared view of the engine search history.
 *
 * <p>Both only-move and extraordinary-move detection need relative-depth
 * windows. Keeping that plumbing here prevents each detector from inventing
 * its own notion of final depth and usable snapshots.</p>
 */
final class SearchTimeline {

    private final DeepAnalysisResult result;

    SearchTimeline(DeepAnalysisResult result) {
        this.result = result;
    }

    int finalDepth() {
        int depth = result.getFinalLines().stream()
                .mapToInt(EngineLine::getDepth)
                .max()
                .orElse(0);
        for (Integer historicalDepth : result.getDepthHistory().keySet()) {
            if (historicalDepth != null) {
                depth = Math.max(depth, historicalDepth);
            }
        }
        return depth;
    }

    List<Map.Entry<Integer, List<EngineLine>>> relativeWindow(
            int minimumLines,
            double startRatio,
            double endRatio) {
        int finalDepth = finalDepth();
        if (finalDepth <= 0) {
            return List.of();
        }

        int startDepth = Math.max(
                1,
                (int) Math.ceil(finalDepth * startRatio));
        int endDepth = Math.max(
                startDepth,
                (int) Math.floor(finalDepth * endRatio));

        List<Map.Entry<Integer, List<EngineLine>>> snapshots =
                usableSnapshots(minimumLines);
        return snapshots.stream()
                .filter(entry -> entry.getKey() >= startDepth
                        && entry.getKey() <= endDepth)
                .toList();
    }

    List<Map.Entry<Integer, List<EngineLine>>> usableSnapshots(
            int minimumLines) {
        List<Map.Entry<Integer, List<EngineLine>>> snapshots =
                new ArrayList<>();

        for (Map.Entry<Integer, List<EngineLine>> entry
                : result.getDepthHistory().entrySet()) {
            if (entry.getKey() != null
                    && entry.getKey() > 0
                    && entry.getValue() != null
                    && entry.getValue().size() >= minimumLines) {
                snapshots.add(entry);
            }
        }

        snapshots.sort(Comparator.comparingInt(Map.Entry::getKey));
        return snapshots;
    }
}
