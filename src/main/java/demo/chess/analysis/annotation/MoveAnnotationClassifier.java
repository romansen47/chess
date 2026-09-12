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
        EngineLine played =
                playedIndex >= 0
                        ? candidates.get(playedIndex)
                        : null;

        double bestScore =
                EvaluationScoring.moverScore(
                        best.getEvaluation(),
                        whiteMover);
        double playedScore =
                EvaluationScoring.moverScore(
                        played != null
                                ? played.getEvaluation()
                                : resultingEvaluation,
                        whiteMover);
        double bestWinPercent =
                EvaluationScoring.winPercentFromMoverScore(
                        bestScore);
        double playedWinPercent =
                EvaluationScoring.winPercentFromMoverScore(
                        playedScore);
        double winChanceLoss =
                Math.max(
                        0.0,
                        bestWinPercent - playedWinPercent);

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
                    extraordinary.getFinalStrength());
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
