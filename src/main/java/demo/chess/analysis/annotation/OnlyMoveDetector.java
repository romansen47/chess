package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;

final class OnlyMoveDetector {

    boolean isTrivial(
            DeepAnalysisResult result,
            String playedMoveUci,
            boolean whiteMover) {
        List<Map.Entry<Integer, List<EngineLine>>> snapshots = usableSnapshots(result, 2);
        if (snapshots.size() < MoveAnnotationPolicy.ONLY_MOVE_TRIVIAL_MIN_SNAPSHOTS) {
            return false;
        }

        int finalDepth = finalDepth(result);
        if (finalDepth <= 0) {
            return false;
        }

        int earlyStart = Math.max(
                1,
                (int) Math.ceil(
                        finalDepth * MoveAnnotationPolicy.ONLY_MOVE_TRIVIAL_EARLY_START_RATIO));
        int earlyEnd = Math.max(
                earlyStart,
                (int) Math.floor(
                        finalDepth * MoveAnnotationPolicy.ONLY_MOVE_TRIVIAL_EARLY_END_RATIO));

        List<Map.Entry<Integer, List<EngineLine>>> early = snapshots.stream()
                .filter(entry -> entry.getKey() >= earlyStart && entry.getKey() <= earlyEnd)
                .toList();

        if (early.size() < MoveAnnotationPolicy.ONLY_MOVE_TRIVIAL_MIN_SNAPSHOTS) {
            return false;
        }

        int obvious = 0;
        for (Map.Entry<Integer, List<EngineLine>> snapshot : early) {
            List<EngineLine> ranked = EvaluationScoring.rankLines(snapshot.getValue(), whiteMover);
            if (ranked.size() < 2
                    || !playedMoveUci.equalsIgnoreCase(EvaluationScoring.firstMove(ranked.get(0)))) {
                continue;
            }

            double best = EvaluationScoring.moverScore(ranked.get(0).getEvaluation(), whiteMover);
            double second = EvaluationScoring.moverScore(ranked.get(1).getEvaluation(), whiteMover);
            double gap = EvaluationScoring.winPercentFromMoverScore(best)
                    - EvaluationScoring.winPercentFromMoverScore(second);
            if (gap >= MoveAnnotationPolicy.ONLY_MOVE_TRIVIAL_WIN_PERCENT_GAP) {
                obvious++;
            }
        }

        return ((double) obvious / early.size())
                >= MoveAnnotationPolicy.ONLY_MOVE_TRIVIAL_SNAPSHOT_RATIO;
    }

    private List<Map.Entry<Integer, List<EngineLine>>> usableSnapshots(
            DeepAnalysisResult result,
            int minimumLines) {
        List<Map.Entry<Integer, List<EngineLine>>> snapshots = new ArrayList<>();
        for (Map.Entry<Integer, List<EngineLine>> entry : result.getDepthHistory().entrySet()) {
            if (entry.getKey() != null
                    && entry.getKey() > 0
                    && entry.getValue() != null
                    && entry.getValue().size() >= minimumLines) {
                snapshots.add(entry);
            }
        }
        return snapshots;
    }

    private int finalDepth(DeepAnalysisResult result) {
        int finalDepth = result.getFinalLines().stream()
                .mapToInt(EngineLine::getDepth)
                .max()
                .orElse(0);
        for (Integer depth : result.getDepthHistory().keySet()) {
            if (depth != null) {
                finalDepth = Math.max(finalDepth, depth);
            }
        }
        return finalDepth;
    }
}
