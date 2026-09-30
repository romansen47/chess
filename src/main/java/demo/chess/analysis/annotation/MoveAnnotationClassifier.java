package demo.chess.analysis.annotation;

import java.util.List;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.Game;

/**
 * Classifies one played move from the finite DeepAnalysis result of the
 * position before that move.
 *
 * <p>No engine is invoked here. This class is deterministic domain logic and
 * can therefore be tested with synthetic DeepAnalysisResult fixtures.</p>
 */
public final class MoveAnnotationClassifier {

    private final ObjectiveMoveQualityEvaluator qualityEvaluator =
            new ObjectiveMoveQualityEvaluator();
    private final OnlyMoveDetector onlyMoveDetector =
            new OnlyMoveDetector();
    private final ExtraordinaryMoveDetector extraordinaryMoveDetector =
            new ExtraordinaryMoveDetector();

    public MoveAnnotation classify(
            Game positionBeforeMove,
            String playedMoveUci,
            DeepAnalysisResult analysisBeforeMove,
            double resultingEvaluation) {
        if (positionBeforeMove == null
                || playedMoveUci == null
                || playedMoveUci.isBlank()
                || analysisBeforeMove == null
                || analysisBeforeMove.getFinalLines().isEmpty()) {
            return null;
        }

        boolean whiteMover =
                positionBeforeMove.getMoveList().size() % 2 == 0;
        List<EngineLine> candidates =
                EvaluationScoring.rankLines(
                        analysisBeforeMove.getFinalLines(),
                        whiteMover);
        if (candidates.isEmpty()) {
            return null;
        }

        EngineLine best = candidates.get(0);
        int playedIndex =
                EvaluationScoring.findMoveIndex(
                        candidates,
                        playedMoveUci);

        double bestScore =
                EvaluationScoring.moverScore(
                        best.getEvaluation(),
                        whiteMover);
        double bestWinPercent =
                EvaluationScoring.winPercentFromMoverScore(
                        bestScore);

        double resultingScore =
                EvaluationScoring.moverScore(
                        resultingEvaluation,
                        whiteMover);
        double resultingWinPercent =
                EvaluationScoring.winPercentFromMoverScore(
                        resultingScore);
        double resultingWinChanceLoss =
                Math.max(
                        0.0,
                        bestWinPercent - resultingWinPercent);

        /*
         * The resulting-position search may discover that the played move was
         * better than its finite root MultiPV score suggested. In that case it
         * is useful evidence and may reduce the move-quality loss.
         *
         * It must not make a move worse than the common root search did,
         * however. A separate search after the move has a different search
         * tree/horizon, so treating a later drop as additional player error can
         * produce contradictions such as the root best move being marked "?".
         *
         * If the played move is outside MultiPV, the resulting-position score
         * remains the only available quality estimate and therefore the
         * fallback.
         */
        double winChanceLoss = resultingWinChanceLoss;
        if (playedIndex >= 0) {
            double rootPlayedScore =
                    EvaluationScoring.moverScore(
                            candidates.get(playedIndex).getEvaluation(),
                            whiteMover);
            double rootPlayedWinPercent =
                    EvaluationScoring.winPercentFromMoverScore(
                            rootPlayedScore);
            double rootWinChanceLoss =
                    Math.max(
                            0.0,
                            bestWinPercent - rootPlayedWinPercent);
            winChanceLoss =
                    Math.min(
                            rootWinChanceLoss,
                            resultingWinChanceLoss);
        }

        ObjectiveMoveQuality quality =
                qualityEvaluator.evaluate(winChanceLoss);
        if (quality == ObjectiveMoveQuality.BLUNDER) {
            return new MoveAnnotation(
                    MoveAnnotationKind.BLUNDER,
                    best.getEvaluation(),
                    winChanceLoss,
                    null);
        }

        if (quality == ObjectiveMoveQuality.MISTAKE) {
            return new MoveAnnotation(
                    MoveAnnotationKind.MISTAKE,
                    best.getEvaluation(),
                    winChanceLoss,
                    null);
        }

        ExtraordinaryMoveDetector.ExtraordinaryEvidence extraordinary =
                extraordinaryMoveDetector.find(
                        positionBeforeMove,
                        analysisBeforeMove,
                        candidates,
                        playedMoveUci,
                        whiteMover);

        if (extraordinary != null) {
            return new MoveAnnotation(
                    MoveAnnotationKind.EXTRAORDINARY,
                    best.getEvaluation(),
                    null,
                    null,
                    extraordinary.getReason(),
                    extraordinary.getMaterialInvestment(),
                    extraordinary.getSacrificeType(),
                    extraordinary.getEarlyDepth(),
                    extraordinary.getEarlyRank(),
                    extraordinary.getFinalDepth(),
                    extraordinary.getFinalRank(),
                    extraordinary.givesCheck(),
                    extraordinary.getEarlyRegret(),
                    extraordinary.getEarlyStrength(),
                    extraordinary.getFinalStrength(),
                    extraordinary.getShortTermMaterialCompensated(),
                    extraordinary.getMaterialCompensationPlies(),
                    extraordinary.getForcedMateDistance());
        }

        if (playedIndex == 0 && candidates.size() > 1) {
            EngineLine second = candidates.get(1);
            double secondScore =
                    EvaluationScoring.moverScore(
                            second.getEvaluation(),
                            whiteMover);
            double winPercentGap =
                    EvaluationScoring
                            .winPercentFromMoverScore(bestScore)
                    - EvaluationScoring
                            .winPercentFromMoverScore(secondScore);

            if (winPercentGap
                    >= MoveAnnotationPolicy
                            .ONLY_MOVE_FINAL_WIN_PERCENT_GAP
                    && !onlyMoveDetector.isTrivial(
                            analysisBeforeMove,
                            playedMoveUci,
                            whiteMover)) {
                return new MoveAnnotation(
                        MoveAnnotationKind.ONLY_MOVE,
                        best.getEvaluation(),
                        null,
                        second.getEvaluation());
            }
        }

        return null;
    }
}
