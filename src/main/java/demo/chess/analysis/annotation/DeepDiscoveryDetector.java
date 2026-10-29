package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;

/**
 * Detects the deliberately narrow "deep discovery" extraordinary signal.
 *
 * <p>The human-oriented question is whether the move looked materially less
 * convincing in one fixed shallow search window and only became clearly
 * strong later. The shallow window is deliberately independent of the final
 * search depth or thinking time.</p>
 */
final class DeepDiscoveryDetector {

    Assessment assess(
            DeepAnalysisResult result,
            List<EngineLine> finalCandidates,
            String playedMoveUci,
            boolean whiteMover) {
        int finalPlayedIndex =
                EvaluationScoring.findMoveIndex(
                        finalCandidates,
                        playedMoveUci);
        if (finalPlayedIndex != 0) {
            return new Assessment(null, null);
        }

        SearchTimeline timeline = new SearchTimeline(result);
        int finalDepth = finalCandidates.stream()
                .mapToInt(EngineLine::getDepth)
                .max()
                .orElse(0);
        if (finalDepth <= 0) {
            return new Assessment(null, null);
        }

        int earlyStart =
                MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_EARLY_START_DEPTH;
        int earlyEnd =
                MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_EARLY_END_DEPTH;

        List<Map.Entry<Integer, List<EngineLine>>> early =
                timeline.fixedWindow(
                        MoveAnnotationPolicy.EXTRAORDINARY_MAX_FINAL_RANK,
                        earlyStart,
                        earlyEnd);

        List<SnapshotSample> samples = new ArrayList<>();
        for (Map.Entry<Integer, List<EngineLine>> snapshot : early) {
            List<EngineLine> ranked =
                    EvaluationScoring.orderedLines(
                            snapshot.getValue());
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

        EngineLine finalPlayed = finalCandidates.get(0);
        double finalStrength =
                winningChance(finalPlayed, whiteMover);

        SnapshotSample representative =
                samples.isEmpty()
                        ? null
                        : samples.get(samples.size() - 1);

        if (samples.size()
                < MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_MIN_EARLY_SNAPSHOTS) {
            return rejected(
                    DeepDiscoveryRejectionReason
                            .INSUFFICIENT_EARLY_SNAPSHOTS,
                    earlyStart,
                    earlyEnd,
                    samples,
                    representative,
                    finalDepth,
                    finalStrength,
                    null,
                    null,
                    null);
        }

        double earlyRegret = median(
                samples.stream()
                        .map(SnapshotSample::regret)
                        .toList());
        double earlyStrength = median(
                samples.stream()
                        .map(SnapshotSample::strength)
                        .toList());
        double strengthGain = finalStrength - earlyStrength;

        if (earlyRegret
                < MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_MIN_EARLY_REGRET_WIN_PERCENT) {
            return rejected(
                    DeepDiscoveryRejectionReason
                            .EARLY_REGRET_TOO_SMALL,
                    earlyStart,
                    earlyEnd,
                    samples,
                    representative,
                    finalDepth,
                    finalStrength,
                    earlyRegret,
                    earlyStrength,
                    strengthGain);
        }

        if (strengthGain
                < MoveAnnotationPolicy
                        .EXTRAORDINARY_DISCOVERY_MIN_STRENGTH_GAIN_WIN_PERCENT) {
            return rejected(
                    DeepDiscoveryRejectionReason
                            .STRENGTH_GAIN_TOO_SMALL,
                    earlyStart,
                    earlyEnd,
                    samples,
                    representative,
                    finalDepth,
                    finalStrength,
                    earlyRegret,
                    earlyStrength,
                    strengthGain);
        }

        Evidence evidence = new Evidence(
                representative.depth(),
                representative.rank(),
                finalDepth,
                1,
                earlyRegret,
                earlyStrength,
                finalStrength);

        MoveAnnotationDiagnostics diagnostics =
                diagnostics(
                        null,
                        earlyStart,
                        earlyEnd,
                        samples,
                        representative,
                        finalDepth,
                        finalStrength,
                        earlyRegret,
                        earlyStrength,
                        strengthGain);

        return new Assessment(evidence, diagnostics);
    }

    private Assessment rejected(
            DeepDiscoveryRejectionReason reason,
            int earlyStart,
            int earlyEnd,
            List<SnapshotSample> samples,
            SnapshotSample representative,
            int finalDepth,
            double finalStrength,
            Double earlyRegret,
            Double earlyStrength,
            Double strengthGain) {
        return new Assessment(
                null,
                diagnostics(
                        reason,
                        earlyStart,
                        earlyEnd,
                        samples,
                        representative,
                        finalDepth,
                        finalStrength,
                        earlyRegret,
                        earlyStrength,
                        strengthGain));
    }

    private MoveAnnotationDiagnostics diagnostics(
            DeepDiscoveryRejectionReason reason,
            int earlyStart,
            int earlyEnd,
            List<SnapshotSample> samples,
            SnapshotSample representative,
            int finalDepth,
            double finalStrength,
            Double earlyRegret,
            Double earlyStrength,
            Double strengthGain) {
        return new MoveAnnotationDiagnostics(
                true,
                reason,
                earlyStart,
                earlyEnd,
                samples.size(),
                representative != null
                        ? representative.depth()
                        : null,
                representative != null
                        ? representative.rank()
                        : null,
                finalDepth,
                1,
                0.0,
                earlyRegret,
                earlyStrength,
                finalStrength,
                strengthGain);
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

    record Assessment(
            Evidence evidence,
            MoveAnnotationDiagnostics diagnostics) {
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
