package demo.chess.analysis.annotation;

import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;

final class OnlyMoveDetector {

    boolean isTrivial(
            DeepAnalysisResult result,
            String playedMoveUci,
            boolean whiteMover) {
        SearchTimeline timeline = new SearchTimeline(result);
        List<Map.Entry<Integer, List<EngineLine>>> early =
                timeline.relativeWindow(
                        2,
                        MoveAnnotationPolicy
                                .ONLY_MOVE_TRIVIAL_EARLY_START_RATIO,
                        MoveAnnotationPolicy
                                .ONLY_MOVE_TRIVIAL_EARLY_END_RATIO);

        if (early.size()
                < MoveAnnotationPolicy
                        .ONLY_MOVE_TRIVIAL_MIN_SNAPSHOTS) {
            return false;
        }

        int obvious = 0;
        for (Map.Entry<Integer, List<EngineLine>> snapshot : early) {
            List<EngineLine> ranked =
                    EvaluationScoring.rankLines(
                            snapshot.getValue(),
                            whiteMover);
            if (ranked.size() < 2
                    || !playedMoveUci.equalsIgnoreCase(
                            EvaluationScoring.firstMove(
                                    ranked.get(0)))) {
                continue;
            }

            double best = EvaluationScoring.moverScore(
                    ranked.get(0).getEvaluation(),
                    whiteMover);
            double second = EvaluationScoring.moverScore(
                    ranked.get(1).getEvaluation(),
                    whiteMover);
            double gap =
                    EvaluationScoring.winPercentFromMoverScore(best)
                    - EvaluationScoring.winPercentFromMoverScore(second);
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
