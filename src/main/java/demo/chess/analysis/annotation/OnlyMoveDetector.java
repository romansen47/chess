package demo.chess.analysis.annotation;

import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;

final class OnlyMoveDetector {

    boolean isTrivial(
            DeepAnalysisResult result,
            String playedMoveUci,
            boolean whiteMover,
            int decisionDepth) {
        SearchTimeline timeline = new SearchTimeline(result);
        List<Map.Entry<Integer, List<EngineLine>>> early =
                timeline.relativeWindow(
                        2,
                        MoveAnnotationPolicy
                                .ONLY_MOVE_TRIVIAL_EARLY_START_RATIO,
                        MoveAnnotationPolicy
                                .ONLY_MOVE_TRIVIAL_EARLY_END_RATIO,
                        decisionDepth);

        if (early.size()
                < MoveAnnotationPolicy
                        .ONLY_MOVE_TRIVIAL_MIN_SNAPSHOTS) {
            return false;
        }

        int obvious = 0;
        for (Map.Entry<Integer, List<EngineLine>> snapshot : early) {
            List<EngineLine> ranked =
                    EvaluationScoring.orderedLines(
                            snapshot.getValue());
            if (ranked.size() < 2
                    || !playedMoveUci.equalsIgnoreCase(
                            EvaluationScoring.firstMove(
                                    ranked.get(0)))) {
                continue;
            }

            double gap = EvaluationScoring.gap(
                    ranked.get(0),
                    ranked.get(1),
                    whiteMover);
            if (gap
                    >= MoveAnnotationPolicy
                            .ONLY_MOVE_TRIVIAL_WIN_PERCENT_GAP) {
                obvious++;
            }
        }

        return ((double) obvious / early.size())
                >= MoveAnnotationPolicy
                        .ONLY_MOVE_TRIVIAL_SNAPSHOT_RATIO;
    }
}
