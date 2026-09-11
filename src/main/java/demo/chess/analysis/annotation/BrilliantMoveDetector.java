package demo.chess.analysis.annotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.Game;

final class BrilliantMoveDetector {

    private final MaterialInvestmentDetector materialDetector =
            new MaterialInvestmentDetector();

    BrilliantEvidence find(
            Game rootPosition,
            DeepAnalysisResult result,
            List<EngineLine> finalCandidates,
            String playedMoveUci,
            boolean whiteMover) {
        if (finalCandidates.size() < MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK) {
            return null;
        }

        int finalPlayedIndex = EvaluationScoring.findMoveIndex(
                finalCandidates,
                playedMoveUci);
        if (finalPlayedIndex < 0
                || finalPlayedIndex >= MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK) {
            return null;
        }

        EngineLine finalBest = finalCandidates.get(0);
        EngineLine finalPlayed = finalCandidates.get(finalPlayedIndex);
        double finalRegret = winningChanceRegret(
                finalBest,
                finalPlayed,
                whiteMover);

        // A brilliance signal may explain why a move is difficult for a human,
        // but it must not turn a materially inferior engine choice into "!!".
        if (finalRegret > MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_REGRET_WIN_PERCENT) {
            return null;
        }

        DeepDiscoveryEvidence discovery = findDeepDiscovery(
                result,
                finalPlayed,
                finalPlayedIndex,
                finalRegret,
                playedMoveUci,
                whiteMover);

        double materialInvestment = materialDetector.calculate(
                rootPosition,
                finalPlayed,
                whiteMover,
                MoveAnnotationPolicy.BRILLIANT_MATERIAL_HORIZON_PLIES);
        boolean hasMaterialInvestment =
                materialInvestment >= MoveAnnotationPolicy.BRILLIANT_MATERIAL_INVESTMENT;

        if (discovery == null && !hasMaterialInvestment) {
            return null;
        }

        BrilliantReason reason;
        if (discovery != null && hasMaterialInvestment) {
            reason = BrilliantReason.DEEP_DISCOVERY_AND_MATERIAL_INVESTMENT;
        } else if (discovery != null) {
            reason = BrilliantReason.DEEP_DISCOVERY;
        } else {
            reason = BrilliantReason.MATERIAL_INVESTMENT;
        }

        int finalDepth = discovery != null
                ? discovery.finalDepth
                : Math.max(finalPlayed.getDepth(), maxHistoryDepth(result));

        return new BrilliantEvidence(
                reason,
                hasMaterialInvestment ? materialInvestment : null,
                discovery != null ? discovery.earlyDepth : null,
                discovery != null ? discovery.earlyRank : null,
                finalDepth,
                finalPlayedIndex + 1);
    }

    private DeepDiscoveryEvidence findDeepDiscovery(
            DeepAnalysisResult result,
            EngineLine finalPlayed,
            int finalPlayedIndex,
            double finalRegret,
            String playedMoveUci,
            boolean whiteMover) {
        List<Map.Entry<Integer, List<EngineLine>>> snapshots =
                usableSnapshots(result);
        if (snapshots.size() < 3) {
            return null;
        }

        int finalDepth = Math.max(finalPlayed.getDepth(), maxHistoryDepth(result));
        if (finalDepth <= 0) {
            return null;
        }

        List<Map.Entry<Integer, List<EngineLine>>> early = phase(
                snapshots,
                finalDepth,
                MoveAnnotationPolicy.BRILLIANT_DISCOVERY_EARLY_START_RATIO,
                MoveAnnotationPolicy.BRILLIANT_DISCOVERY_EARLY_END_RATIO);
        List<Map.Entry<Integer, List<EngineLine>>> middle = phase(
                snapshots,
                finalDepth,
                MoveAnnotationPolicy.BRILLIANT_DISCOVERY_MIDDLE_START_RATIO,
                MoveAnnotationPolicy.BRILLIANT_DISCOVERY_MIDDLE_END_RATIO);
        List<Map.Entry<Integer, List<EngineLine>>> late = phase(
                snapshots,
                finalDepth,
                MoveAnnotationPolicy.BRILLIANT_DISCOVERY_LATE_START_RATIO,
                1.0);

        int minSnapshots = MoveAnnotationPolicy.BRILLIANT_DISCOVERY_MIN_PHASE_SNAPSHOTS;
        if (early.size() < minSnapshots
                || middle.size() < minSnapshots
                || late.size() < minSnapshots) {
            return null;
        }

        PhaseMetrics earlyMetrics = phaseMetrics(
                early,
                playedMoveUci,
                whiteMover);
        PhaseMetrics middleMetrics = phaseMetrics(
                middle,
                playedMoveUci,
                whiteMover);
        PhaseMetrics lateMetrics = phaseMetrics(
                late,
                playedMoveUci,
                whiteMover);

        boolean lateStableTopThree = stableLateRank(
                late,
                playedMoveUci,
                whiteMover,
                MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK);
        if (!lateStableTopThree) {
            return null;
        }

        /*
         * A) Rank/regret discovery
         *
         * The move starts significantly behind the alternatives and becomes
         * progressively more competitive through early -> middle -> late
         * search phases. Median phase regret is used instead of a single depth
         * so one volatile snapshot cannot create "!!".
         */
        double earlyToMiddleRegretGain =
                earlyMetrics.medianRegret - middleMetrics.medianRegret;
        double middleToLateRegretGain =
                middleMetrics.medianRegret - lateMetrics.medianRegret;
        double totalRegretGain =
                earlyMetrics.medianRegret - lateMetrics.medianRegret;

        boolean rankRegretDiscovery =
                earlyMetrics.medianRegret
                        >= MoveAnnotationPolicy
                                .BRILLIANT_DISCOVERY_MIN_EARLY_REGRET_WIN_PERCENT
                && totalRegretGain
                        >= MoveAnnotationPolicy
                                .BRILLIANT_DISCOVERY_MIN_REGRET_IMPROVEMENT_WIN_PERCENT
                && earlyToMiddleRegretGain
                        >= MoveAnnotationPolicy
                                .BRILLIANT_DISCOVERY_MIN_REGRET_PHASE_STEP_WIN_PERCENT
                && middleToLateRegretGain
                        >= MoveAnnotationPolicy
                                .BRILLIANT_DISCOVERY_MIN_REGRET_PHASE_STEP_WIN_PERCENT;

        /*
         * B) Strength discovery
         *
         * Some moves are already plausible candidates early, but the engine
         * only discovers how powerful the move really is as the search
         * develops. This is deliberately separate from rank/regret discovery:
         * a move such as ...Qd3 can already be rank 1 while its practical
         * winning chance rises dramatically across the search.
         *
         * Strength discovery is intentionally conservative:
         * - the move must finish rank 1;
         * - it must already appear in enough early snapshots and usually be a
         *   top-three candidate there;
         * - practical winning chance must rise in both phase transitions;
         * - the move must remain rank 1 in at least two of the last three late
         *   snapshots.
         */
        boolean hasStrengthSamples =
                earlyMetrics.strengthSampleCount >= minSnapshots
                && middleMetrics.strengthSampleCount >= minSnapshots
                && lateMetrics.strengthSampleCount >= minSnapshots;

        double earlyTopThreeRatio = earlyMetrics.strengthSampleCount == 0
                ? 0.0
                : (double) earlyMetrics.topThreeCount
                        / earlyMetrics.strengthSampleCount;

        double earlyToMiddleStrengthGain =
                middleMetrics.medianStrength - earlyMetrics.medianStrength;
        double middleToLateStrengthGain =
                lateMetrics.medianStrength - middleMetrics.medianStrength;
        double totalStrengthGain =
                lateMetrics.medianStrength - earlyMetrics.medianStrength;

        boolean strengthDiscovery =
                finalPlayedIndex == 0
                && hasStrengthSamples
                && earlyTopThreeRatio
                        >= MoveAnnotationPolicy.BRILLIANT_DISCOVERY_EARLY_TOP_THREE_RATIO
                && totalStrengthGain
                        >= MoveAnnotationPolicy
                                .BRILLIANT_DISCOVERY_MIN_STRENGTH_GAIN_WIN_PERCENT
                && earlyToMiddleStrengthGain
                        >= MoveAnnotationPolicy
                                .BRILLIANT_DISCOVERY_MIN_STRENGTH_PHASE_STEP_WIN_PERCENT
                && middleToLateStrengthGain
                        >= MoveAnnotationPolicy
                                .BRILLIANT_DISCOVERY_MIN_STRENGTH_PHASE_STEP_WIN_PERCENT
                && stableLateRank(
                        late,
                        playedMoveUci,
                        whiteMover,
                        1);

        if (!rankRegretDiscovery && !strengthDiscovery) {
            return null;
        }

        Map.Entry<Integer, List<EngineLine>> representative =
                representativeEarlySnapshot(early, playedMoveUci);
        List<EngineLine> rankedRepresentative =
                EvaluationScoring.rankLines(representative.getValue(), whiteMover);
        int representativeIndex =
                EvaluationScoring.findMoveIndex(rankedRepresentative, playedMoveUci);

        return new DeepDiscoveryEvidence(
                representative.getKey(),
                representativeIndex >= 0 ? representativeIndex + 1 : null,
                finalDepth);
    }

    private List<Map.Entry<Integer, List<EngineLine>>> usableSnapshots(
            DeepAnalysisResult result) {
        List<Map.Entry<Integer, List<EngineLine>>> snapshots = new ArrayList<>();
        for (Map.Entry<Integer, List<EngineLine>> entry : result.getDepthHistory().entrySet()) {
            if (entry.getKey() != null
                    && entry.getKey() > 0
                    && entry.getValue() != null
                    && entry.getValue().size() >= MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK) {
                snapshots.add(entry);
            }
        }
        snapshots.sort(Comparator.comparingInt(Map.Entry::getKey));
        return snapshots;
    }

    private List<Map.Entry<Integer, List<EngineLine>>> phase(
            List<Map.Entry<Integer, List<EngineLine>>> snapshots,
            int finalDepth,
            double startRatio,
            double endRatio) {
        int startDepth = Math.max(1, (int) Math.ceil(finalDepth * startRatio));
        int endDepth = Math.max(startDepth, (int) Math.floor(finalDepth * endRatio));

        return snapshots.stream()
                .filter(entry -> entry.getKey() >= startDepth
                        && entry.getKey() <= endDepth)
                .toList();
    }

    private PhaseMetrics phaseMetrics(
            List<Map.Entry<Integer, List<EngineLine>>> phase,
            String playedMoveUci,
            boolean whiteMover) {
        List<Double> regrets = new ArrayList<>();
        List<Double> strengths = new ArrayList<>();
        int topThreeCount = 0;

        for (Map.Entry<Integer, List<EngineLine>> snapshot : phase) {
            List<EngineLine> ranked =
                    EvaluationScoring.rankLines(snapshot.getValue(), whiteMover);
            int playedIndex =
                    EvaluationScoring.findMoveIndex(ranked, playedMoveUci);

            EngineLine reference = playedIndex >= 0
                    ? ranked.get(playedIndex)
                    : ranked.get(ranked.size() - 1);
            regrets.add(winningChanceRegret(
                    ranked.get(0),
                    reference,
                    whiteMover));

            if (playedIndex >= 0) {
                strengths.add(winningChance(reference, whiteMover));
                if (playedIndex < MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK) {
                    topThreeCount++;
                }
            }
        }

        return new PhaseMetrics(
                median(regrets),
                strengths.isEmpty() ? Double.NaN : median(strengths),
                strengths.size(),
                topThreeCount);
    }

    private boolean stableLateRank(
            List<Map.Entry<Integer, List<EngineLine>>> late,
            String playedMoveUci,
            boolean whiteMover,
            int maxRankExclusive) {
        List<Map.Entry<Integer, List<EngineLine>>> tail = late;
        if (tail.size() > 3) {
            tail = tail.subList(tail.size() - 3, tail.size());
        }
        if (tail.size() < 2) {
            return false;
        }

        int stable = 0;
        for (Map.Entry<Integer, List<EngineLine>> snapshot : tail) {
            List<EngineLine> ranked =
                    EvaluationScoring.rankLines(snapshot.getValue(), whiteMover);
            int rank = EvaluationScoring.findMoveIndex(ranked, playedMoveUci);
            if (rank >= 0 && rank < maxRankExclusive) {
                stable++;
            }
        }
        return stable >= 2;
    }

    private Map.Entry<Integer, List<EngineLine>> representativeEarlySnapshot(
            List<Map.Entry<Integer, List<EngineLine>>> early,
            String playedMoveUci) {
        for (int index = early.size() - 1; index >= 0; index--) {
            Map.Entry<Integer, List<EngineLine>> snapshot = early.get(index);
            if (EvaluationScoring.findMoveIndex(snapshot.getValue(), playedMoveUci) >= 0) {
                return snapshot;
            }
        }
        return early.get(early.size() - 1);
    }

    private double winningChanceRegret(
            EngineLine best,
            EngineLine candidate,
            boolean whiteMover) {
        return Math.max(
                0.0,
                winningChance(best, whiteMover)
                        - winningChance(candidate, whiteMover));
    }

    private double winningChance(
            EngineLine line,
            boolean whiteMover) {
        double score = EvaluationScoring.moverScore(
                line.getEvaluation(),
                whiteMover);
        return EvaluationScoring.winPercentFromMoverScore(score);
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private int maxHistoryDepth(DeepAnalysisResult result) {
        int max = 0;
        for (Integer depth : result.getDepthHistory().keySet()) {
            if (depth != null) {
                max = Math.max(max, depth);
            }
        }
        return max;
    }

    static final class BrilliantEvidence {
        private final BrilliantReason reason;
        private final Double materialInvestment;
        private final Integer earlyDepth;
        private final Integer earlyRank;
        private final int finalDepth;
        private final int finalRank;

        BrilliantEvidence(
                BrilliantReason reason,
                Double materialInvestment,
                Integer earlyDepth,
                Integer earlyRank,
                int finalDepth,
                int finalRank) {
            this.reason = reason;
            this.materialInvestment = materialInvestment;
            this.earlyDepth = earlyDepth;
            this.earlyRank = earlyRank;
            this.finalDepth = finalDepth;
            this.finalRank = finalRank;
        }

        BrilliantReason getReason() {
            return reason;
        }

        Double getMaterialInvestment() {
            return materialInvestment;
        }

        Integer getEarlyDepth() {
            return earlyDepth;
        }

        Integer getEarlyRank() {
            return earlyRank;
        }

        int getFinalDepth() {
            return finalDepth;
        }

        int getFinalRank() {
            return finalRank;
        }
    }

    private static final class PhaseMetrics {
        private final double medianRegret;
        private final double medianStrength;
        private final int strengthSampleCount;
        private final int topThreeCount;

        private PhaseMetrics(
                double medianRegret,
                double medianStrength,
                int strengthSampleCount,
                int topThreeCount) {
            this.medianRegret = medianRegret;
            this.medianStrength = medianStrength;
            this.strengthSampleCount = strengthSampleCount;
            this.topThreeCount = topThreeCount;
        }
    }

    private static final class DeepDiscoveryEvidence {
        private final int earlyDepth;
        private final Integer earlyRank;
        private final int finalDepth;

        private DeepDiscoveryEvidence(int earlyDepth, Integer earlyRank, int finalDepth) {
            this.earlyDepth = earlyDepth;
            this.earlyRank = earlyRank;
            this.finalDepth = finalDepth;
        }
    }
}
