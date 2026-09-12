package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;

/**
 * Detects the deliberately narrow "deep discovery" extraordinary signal.
 *
 * <p>The question is no longer whether the move climbs relative to worsening
 * alternatives. The move itself must become substantially stronger as search
 * deepens, and it must not already have been an obvious zero-regret best move
 * in the early search window.</p>
 */
final class DeepDiscoveryDetector {

    Evidence find(
            DeepAnalysisResult result,
            List<EngineLine> finalCandidates,
            String playedMoveUci,
            boolean whiteMover) {
        int finalPlayedIndex =
                EvaluationScoring.findMoveIndex(
                        finalCandidates,
                        playedMoveUci);
        if (finalPlayedIndex != 0) {
            return null;
        }

        SearchTimeline timeline = new SearchTimeline(result);
        int finalDepth = timeline.finalDepth();
        if (finalDepth <= 0) {
            return null;
        }

        List<Map.Entry<Integer, List<EngineLine>>> early =
                timeline.relativeWindow(
                        MoveAnnotationPolicy.EXTRAORDINARY_MAX_FINAL_RANK,
                        MoveAnnotationPolicy
                                .EXTRAORDINARY_DISCOVERY_EARLY_START_RATIO,
                        MoveAnnotationPolicy
                                .EXTRAORDINARY_DISCOVERY_EARLY_END_RATIO);

        List<SnapshotSample> samples = new ArrayList<>();
        for (Map.Entry<Integer, List<EngineLine>> snapshot : early) {
            List<EngineLine> ranked =
                    EvaluationScoring.rankLines(
                            snapshot.getValue(),
                            whiteMover);
            int playedIndex =
                    EvaluationScoring.findMoveIndex(
                            ranked,
                            playedMoveUci);
            if (playedIndex < 0) {
                continue;
            }

            EngineLine played = ranked.get(playedIndex);
            double strength = winningChance(played, whiteMover);
            double regret = Math.max(
                    0.0,
                    winningChance(ranked.get(0), whiteMover)
                            - strength);
            samples.add(new SnapshotSample(
                    snapshot.getKey(),
                    playedIndex + 1,
                    regret,
                    strength));
        }

        if (samples.size()
                < MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_MIN_EARLY_SNAPSHOTS) {
            return null;
        }

        double earlyRegret = median(
                samples.stream()
                        .map(SnapshotSample::regret)
                        .toList());
        double earlyStrength = median(
                samples.stream()
                        .map(SnapshotSample::strength)
                        .toList());

        EngineLine finalPlayed = finalCandidates.get(0);
        double finalStrength =
                winningChance(finalPlayed, whiteMover);
        double strengthGain = finalStrength - earlyStrength;

        if (earlyRegret
                < MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_MIN_EARLY_REGRET_WIN_PERCENT) {
            return null;
        }

        if (strengthGain
                < MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_MIN_STRENGTH_GAIN_WIN_PERCENT) {
            return null;
        }

        SnapshotSample representative =
                samples.get(samples.size() - 1);

        return new Evidence(
                representative.depth(),
                representative.rank(),
                finalDepth,
                1,
                earlyRegret,
                earlyStrength,
                finalStrength);
    }

    private double winningChance(
            EngineLine line,
            boolean whiteMover) {
        double moverScore = EvaluationScoring.moverScore(
                line.getEvaluation(),
                whiteMover);
        return EvaluationScoring.winPercentFromMoverScore(
                moverScore);
    }

    private double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1)
                + sorted.get(middle)) / 2.0;
    }

    record Evidence(
            int earlyDepth,
            int earlyRank,
            int finalDepth,
            int finalRank,
            double earlyRegret,
            double earlyStrength,
            double finalStrength) {
    }

    private record SnapshotSample(
            int depth,
            int rank,
            double regret,
            double strength) {
    }
}
