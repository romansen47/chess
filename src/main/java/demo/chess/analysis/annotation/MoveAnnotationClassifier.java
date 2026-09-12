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
    private final OnlyMoveDetector onlyMoveDetector = new OnlyMoveDetector();
    private final BrilliantMoveDetector brilliantMoveDetector =
            new BrilliantMoveDetector();

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

        boolean whiteMover = positionBeforeMove.getMoveList().size() % 2 == 0;
        List<EngineLine> candidates = EvaluationScoring.rankLines(
                analysisBeforeMove.getFinalLines(),
                whiteMover);
        if (candidates.isEmpty()) {
            return null;
        }

        EngineLine best = candidates.get(0);
        int playedIndex = EvaluationScoring.findMoveIndex(candidates, playedMoveUci);
        EngineLine played = playedIndex >= 0 ? candidates.get(playedIndex) : null;

        double bestScore =
                EvaluationScoring.moverScore(best.getEvaluation(), whiteMover);
        double playedScore = EvaluationScoring.moverScore(
                played != null ? played.getEvaluation() : resultingEvaluation,
                whiteMover);
        double bestWinPercent =
                EvaluationScoring.winPercentFromMoverScore(bestScore);
        double playedWinPercent =
                EvaluationScoring.winPercentFromMoverScore(playedScore);
        double winChanceLoss = Math.max(
                0.0,
                bestWinPercent - playedWinPercent);

        // Layer 1: objective quality is based on practical winning-chance loss,
        // not on raw pawn-evaluation loss. This avoids marking a move as a
        // blunder merely because +18 becomes +13 while the game remains
        // overwhelmingly won.
        ObjectiveMoveQuality quality = qualityEvaluator.evaluate(winChanceLoss);
        if (quality == ObjectiveMoveQuality.BLUNDER) {
            return new MoveAnnotation(
                    MoveAnnotationKind.BLUNDER,
                    best.getEvaluation(),
                    winChanceLoss,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        if (quality == ObjectiveMoveQuality.MISTAKE) {
            return new MoveAnnotation(
                    MoveAnnotationKind.MISTAKE,
                    best.getEvaluation(),
                    winChanceLoss,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        // Layer 2: human-difficulty evidence. Only objectively acceptable
        // moves can become brilliant.
        BrilliantMoveDetector.BrilliantEvidence brilliant =
                brilliantMoveDetector.find(
                        positionBeforeMove,
                        analysisBeforeMove,
                        candidates,
                        playedMoveUci,
                        whiteMover);
        if (brilliant != null) {
            return new MoveAnnotation(
                    MoveAnnotationKind.BRILLIANT,
                    best.getEvaluation(),
                    null,
                    null,
                    brilliant.getReason(),
                    brilliant.getMaterialInvestment(),
                    brilliant.getSacrificeType(),
                    brilliant.getEarlyDepth(),
                    brilliant.getEarlyRank(),
                    brilliant.getFinalDepth(),
                    brilliant.getFinalRank(),
                    brilliant.givesCheck(),
                    brilliant.getEarlyRegret(),
                    brilliant.getMiddleRegret(),
                    brilliant.getLateRegret(),
                    brilliant.getEarlyStrength(),
                    brilliant.getMiddleStrength(),
                    brilliant.getLateStrength());
        }

        if (playedIndex == 0 && candidates.size() > 1) {
            EngineLine second = candidates.get(1);
            double secondScore = EvaluationScoring.moverScore(
                    second.getEvaluation(),
                    whiteMover);
            double winPercentGap =
                    EvaluationScoring.winPercentFromMoverScore(bestScore)
                    - EvaluationScoring.winPercentFromMoverScore(secondScore);

            if (winPercentGap >= MoveAnnotationPolicy.ONLY_MOVE_FINAL_WIN_PERCENT_GAP
                    && !onlyMoveDetector.isTrivial(
                            analysisBeforeMove,
                            playedMoveUci,
                            whiteMover)) {
                return new MoveAnnotation(
                        MoveAnnotationKind.ONLY_MOVE,
                        best.getEvaluation(),
                        null,
                        second.getEvaluation(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
            }
        }

        return null;
    }
}
