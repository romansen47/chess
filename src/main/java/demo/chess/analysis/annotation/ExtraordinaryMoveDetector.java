package demo.chess.analysis.annotation;

import java.util.List;
import java.util.Objects;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.players.Player;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Detects objectively sound moves with one clearly explainable extraordinary
 * property.
 *
 * <p>The detector deliberately supports only two independent signals:
 * a real material sacrifice and genuine deep discovery of the move's own
 * strength. "!!" is therefore an explanatory marker, not a claim that the
 * move is uniquely brilliant in a human or historical sense.</p>
 */
final class ExtraordinaryMoveDetector {

    private final MaterialSacrificeDetector materialSacrificeDetector =
            new MaterialSacrificeDetector();
    private final DeepDiscoveryDetector deepDiscoveryDetector =
            new DeepDiscoveryDetector();

    ExtraordinaryEvidence find(
            Game rootPosition,
            DeepAnalysisResult result,
            List<EngineLine> finalCandidates,
            String playedMoveUci,
            boolean whiteMover) {
        if (finalCandidates.size()
                < MoveAnnotationPolicy.EXTRAORDINARY_MAX_FINAL_RANK) {
            return null;
        }

        int finalPlayedIndex =
                EvaluationScoring.findMoveIndex(
                        finalCandidates,
                        playedMoveUci);
        if (finalPlayedIndex < 0
                || finalPlayedIndex
                        >= MoveAnnotationPolicy.EXTRAORDINARY_MAX_FINAL_RANK) {
            return null;
        }

        EngineLine finalBest = finalCandidates.get(0);
        EngineLine finalPlayed =
                finalCandidates.get(finalPlayedIndex);
        double finalRegret = Math.max(
                0.0,
                winningChance(finalBest, whiteMover)
                        - winningChance(finalPlayed, whiteMover));

        if (finalRegret
                > MoveAnnotationPolicy
                        .EXTRAORDINARY_MAX_FINAL_REGRET_WIN_PERCENT) {
            return null;
        }

        DeepDiscoveryDetector.Evidence discovery =
                deepDiscoveryDetector.find(
                        result,
                        finalCandidates,
                        playedMoveUci,
                        whiteMover);

        MaterialSacrificeEvidence sacrifice =
                materialSacrificeDetector.find(
                        rootPosition,
                        finalPlayed,
                        playedMoveUci,
                        whiteMover);
        if (sacrifice != null
                && !materialContextIsMeaningful(
                        finalBest,
                        whiteMover)) {
            sacrifice = null;
        }

        if (discovery == null && sacrifice == null) {
            return null;
        }

        ExtraordinaryReason reason;
        if (discovery != null && sacrifice != null) {
            reason =
                    ExtraordinaryReason
                            .DEEP_DISCOVERY_AND_MATERIAL_SACRIFICE;
        } else if (discovery != null) {
            reason = ExtraordinaryReason.DEEP_DISCOVERY;
        } else {
            reason = ExtraordinaryReason.MATERIAL_SACRIFICE;
        }

        int finalDepth = discovery != null
                ? discovery.finalDepth()
                : Math.max(
                        finalPlayed.getDepth(),
                        new SearchTimeline(result).finalDepth());

        return new ExtraordinaryEvidence(
                reason,
                sacrifice != null ? sacrifice.getValue() : null,
                sacrifice != null ? sacrifice.getType() : null,
                discovery != null ? discovery.earlyDepth() : null,
                discovery != null ? discovery.earlyRank() : null,
                finalDepth,
                finalPlayedIndex + 1,
                givesCheck(
                        rootPosition,
                        playedMoveUci,
                        whiteMover),
                discovery != null ? discovery.earlyRegret() : null,
                discovery != null ? discovery.earlyStrength() : null,
                discovery != null ? discovery.finalStrength() : null);
    }

    private boolean materialContextIsMeaningful(
            EngineLine bestAvailableMove,
            boolean whiteMover) {
        double moverScore = EvaluationScoring.moverScore(
                bestAvailableMove.getEvaluation(),
                whiteMover);
        double bestWinPercent =
                EvaluationScoring.winPercentFromMoverScore(
                        moverScore);
        return bestWinPercent
                >= MoveAnnotationPolicy
                        .EXTRAORDINARY_MATERIAL_MIN_BEST_WIN_PERCENT;
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

    private boolean givesCheck(
            Game rootPosition,
            String playedMoveUci,
            boolean whiteMover) {
        try {
            Game afterMove =
                    Simulation.forkSimulationFrom(
                            rootPosition.getMoveList());
            Move move =
                    LegalMoveResolver.resolveUci(
                            afterMove,
                            playedMoveUci);
            afterMove.apply(move);

            Player mover = whiteMover
                    ? afterMove.getWhitePlayer()
                    : afterMove.getBlackPlayer();
            Player opponent = whiteMover
                    ? afterMove.getBlackPlayer()
                    : afterMove.getWhitePlayer();

            if (opponent == null
                    || opponent.getKing() == null
                    || opponent.getKing().getField() == null) {
                return false;
            }

            return mover.getSimpleMoves().stream()
                    .map(Move::getTarget)
                    .filter(Objects::nonNull)
                    .anyMatch(
                            opponent.getKing()
                                    .getField()::equals);
        } catch (Exception ignored) {
            return false;
        }
    }

    static final class ExtraordinaryEvidence {
        private final ExtraordinaryReason reason;
        private final Double materialInvestment;
        private final MaterialSacrificeType sacrificeType;
        private final Integer earlyDepth;
        private final Integer earlyRank;
        private final int finalDepth;
        private final int finalRank;
        private final boolean givesCheck;
        private final Double earlyRegret;
        private final Double earlyStrength;
        private final Double finalStrength;

        ExtraordinaryEvidence(
                ExtraordinaryReason reason,
                Double materialInvestment,
                MaterialSacrificeType sacrificeType,
                Integer earlyDepth,
                Integer earlyRank,
                int finalDepth,
                int finalRank,
                boolean givesCheck,
                Double earlyRegret,
                Double earlyStrength,
                Double finalStrength) {
            this.reason = reason;
            this.materialInvestment = materialInvestment;
            this.sacrificeType = sacrificeType;
            this.earlyDepth = earlyDepth;
            this.earlyRank = earlyRank;
            this.finalDepth = finalDepth;
            this.finalRank = finalRank;
            this.givesCheck = givesCheck;
            this.earlyRegret = earlyRegret;
            this.earlyStrength = earlyStrength;
            this.finalStrength = finalStrength;
        }

        ExtraordinaryReason getReason() {
            return reason;
        }

        Double getMaterialInvestment() {
            return materialInvestment;
        }

        MaterialSacrificeType getSacrificeType() {
            return sacrificeType;
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

        boolean givesCheck() {
            return givesCheck;
        }

        Double getEarlyRegret() {
            return earlyRegret;
        }

        Double getEarlyStrength() {
            return earlyStrength;
        }

        Double getFinalStrength() {
            return finalStrength;
        }
    }
}
