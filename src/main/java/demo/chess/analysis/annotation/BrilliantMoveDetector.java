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

        EngineLine finalPlayed = finalCandidates.get(finalPlayedIndex);
        DeepDiscoveryEvidence discovery = findDeepDiscovery(
                result,
                finalPlayed,
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
        int earlyIndex = EvaluationScoring.findMoveIndex(earlyRanked, playedMoveUci);

        boolean outsideTopThree = earlyIndex < 0
                || earlyIndex >= MoveAnnotationPolicy.BRILLIANT_MAX_FINAL_RANK;

        boolean gainedWinningChance = false;
        if (earlyIndex >= 0) {
            double earlyScore = EvaluationScoring.moverScore(
                    earlyRanked.get(earlyIndex).getEvaluation(),
                    whiteMover);
            double finalScore = EvaluationScoring.moverScore(
                    finalPlayed.getEvaluation(),
                    whiteMover);
            double gain = EvaluationScoring.winPercentFromMoverScore(finalScore)
                    - EvaluationScoring.winPercentFromMoverScore(earlyScore);
            gainedWinningChance =
                    gain >= MoveAnnotationPolicy.BRILLIANT_DISCOVERY_WIN_PERCENT_GAIN;
        }

        if (!outsideTopThree && !gainedWinningChance) {
            return null;
        }

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
