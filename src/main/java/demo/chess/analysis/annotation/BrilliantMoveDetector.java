package demo.chess.analysis.annotation;

import java.util.ArrayList;
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
            double finalRegret,
            String playedMoveUci,
            boolean whiteMover) {
        List<Map.Entry<Integer, List<EngineLine>>> snapshots = new ArrayList<>();
        for (Map.Entry<Integer, List<EngineLine>> entry : result.getDepthHistory().entrySet()) {
            if (entry.getKey() != null
                    && entry.getKey() > 0
                    && entry.getValue() != null
                    && entry.getValue().size() >= MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK) {
                snapshots.add(entry);
            }
        }
        if (snapshots.size() < 3) {
            return null;
        }

        int finalDepth = Math.max(finalPlayed.getDepth(), maxHistoryDepth(result));
        if (finalDepth <= 0) {
            return null;
        }

        int earlyLimit = Math.max(
                1,
                (int) Math.floor(
                        finalDepth * MoveAnnotationPolicy.BRILLIANT_DISCOVERY_EARLY_DEPTH_RATIO));

        Map.Entry<Integer, List<EngineLine>> early = null;
        for (Map.Entry<Integer, List<EngineLine>> snapshot : snapshots) {
            if (snapshot.getKey() <= earlyLimit) {
                early = snapshot;
            }
        }
        if (early == null) {
            return null;
        }

        List<EngineLine> earlyRanked =
                EvaluationScoring.rankLines(early.getValue(), whiteMover);
        if (earlyRanked.size() < MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK) {
            return null;
        }

        int earlyIndex = EvaluationScoring.findMoveIndex(earlyRanked, playedMoveUci);
        EngineLine earlyReference = earlyIndex >= 0
                ? earlyRanked.get(earlyIndex)
                : earlyRanked.get(earlyRanked.size() - 1);

        // If the played move is outside MultiPV, the last returned candidate is
        // an upper bound for its score. The resulting regret is therefore a
        // conservative lower bound, which is still useful without treating a
        // mere rank change as evidence.
        double earlyRegret = winningChanceRegret(
                earlyRanked.get(0),
                earlyReference,
                whiteMover);
        double regretImprovement = earlyRegret - finalRegret;

        if (earlyRegret
                    < MoveAnnotationPolicy.BRILLIANT_DISCOVERY_MIN_EARLY_REGRET_WIN_PERCENT
                || regretImprovement
                    < MoveAnnotationPolicy.BRILLIANT_DISCOVERY_MIN_REGRET_IMPROVEMENT_WIN_PERCENT) {
            return null;
        }

        // Avoid one-depth spikes: the move must remain Top 3 in at least two of
        // the last three sufficiently deep snapshots.
        int lateStart = Math.max(
                1,
                (int) Math.ceil(
                        finalDepth * MoveAnnotationPolicy.BRILLIANT_DISCOVERY_LATE_DEPTH_RATIO));
        List<Map.Entry<Integer, List<EngineLine>>> late = snapshots.stream()
                .filter(entry -> entry.getKey() >= lateStart)
                .toList();
        if (late.size() > 3) {
            late = late.subList(late.size() - 3, late.size());
        }
        if (late.size() < 2) {
            return null;
        }

        int stableTopThree = 0;
        for (Map.Entry<Integer, List<EngineLine>> snapshot : late) {
            List<EngineLine> ranked =
                    EvaluationScoring.rankLines(snapshot.getValue(), whiteMover);
            int rank = EvaluationScoring.findMoveIndex(ranked, playedMoveUci);
            if (rank >= 0 && rank < MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK) {
                stableTopThree++;
            }
        }
        if (stableTopThree < 2) {
            return null;
        }

        return new DeepDiscoveryEvidence(
                early.getKey(),
                earlyIndex >= 0 ? earlyIndex + 1 : null,
                finalDepth);
    }

    private double winningChanceRegret(
            EngineLine best,
            EngineLine candidate,
            boolean whiteMover) {
        double bestScore = EvaluationScoring.moverScore(
                best.getEvaluation(),
                whiteMover);
        double candidateScore = EvaluationScoring.moverScore(
                candidate.getEvaluation(),
                whiteMover);
        return Math.max(
                0.0,
                EvaluationScoring.winPercentFromMoverScore(bestScore)
                        - EvaluationScoring.winPercentFromMoverScore(candidateScore));
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
