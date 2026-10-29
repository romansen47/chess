package demo.chess.analysis.annotation;

import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;

/** Selects the stable search snapshot used for positive human annotations. */
final class HumanAnnotationHorizon {

    Selection select(
            DeepAnalysisResult result,
            String playedMoveUci,
            boolean whiteMover) {
        List<EngineLine> finalLines = EvaluationScoring.orderedLines(
                result.getFinalLines());
        int finalDepth = maximumDepth(finalLines);
        if (finalDepth
                <= MoveAnnotationPolicy.HUMAN_ANNOTATION_HORIZON_DEPTH) {
            return new Selection(finalLines, finalDepth, true);
        }

        List<Map.Entry<Integer, List<EngineLine>>> window =
                new SearchTimeline(result).fixedWindow(
                        2,
                        MoveAnnotationPolicy
                                .HUMAN_ANNOTATION_STABILITY_START_DEPTH,
                        MoveAnnotationPolicy
                                .HUMAN_ANNOTATION_HORIZON_DEPTH);
        if (window.size()
                < MoveAnnotationPolicy
                        .HUMAN_ANNOTATION_MIN_STABLE_SNAPSHOTS) {
            return new Selection(finalLines, finalDepth, false);
        }

        Map.Entry<Integer, List<EngineLine>> checkpoint =
                window.get(window.size() - 1);
        List<EngineLine> checkpointLines =
                EvaluationScoring.orderedLines(checkpoint.getValue());
        int checkpointRank = EvaluationScoring.findMoveIndex(
                checkpointLines,
                playedMoveUci);
        if (checkpointRank < 0) {
            return new Selection(checkpointLines, checkpoint.getKey(), false);
        }

        int stableSnapshots = 0;
        for (Map.Entry<Integer, List<EngineLine>> entry : window) {
            List<EngineLine> lines =
                    EvaluationScoring.orderedLines(entry.getValue());
            int rank = EvaluationScoring.findMoveIndex(lines, playedMoveUci);
            if (rank < 0 || (checkpointRank == 0 && rank != 0)) {
                continue;
            }
            if (rank < MoveAnnotationPolicy.EXTRAORDINARY_MAX_FINAL_RANK
                    && regret(lines, rank, whiteMover)
                            <= MoveAnnotationPolicy
                                    .EXTRAORDINARY_MAX_FINAL_REGRET_WIN_PERCENT) {
                stableSnapshots++;
            }
        }

        double stableRatio = (double) stableSnapshots / window.size();
        return new Selection(
                checkpointLines,
                checkpoint.getKey(),
                stableRatio
                        >= MoveAnnotationPolicy
                                .HUMAN_ANNOTATION_STABLE_SNAPSHOT_RATIO);
    }

    private double regret(
            List<EngineLine> lines,
            int playedIndex,
            boolean whiteMover) {
        double best = winningChance(lines.get(0), whiteMover);
        double played = winningChance(lines.get(playedIndex), whiteMover);
        return Math.max(0.0, best - played);
    }

    private double winningChance(EngineLine line, boolean whiteMover) {
        double score = EvaluationScoring.moverScore(
                line.getEvaluation(),
                whiteMover);
        return EvaluationScoring.winPercentFromMoverScore(score);
    }

    private int maximumDepth(List<EngineLine> lines) {
        return lines.stream().mapToInt(EngineLine::getDepth).max().orElse(0);
    }

    record Selection(
            List<EngineLine> lines,
            int depth,
            boolean certified) {
    }
}
