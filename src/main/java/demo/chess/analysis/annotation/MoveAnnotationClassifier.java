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
    private final HumanAnnotationHorizon humanAnnotationHorizon =
            new HumanAnnotationHorizon();
    private final RoutineMoveDetector routineMoveDetector =
            new RoutineMoveDetector();

    public MoveAnnotation classify(
            Game positionBeforeMove,
            String playedMoveUci,
            DeepAnalysisResult analysisBeforeMove,
            double resultingEvaluation) {
        return assess(
                positionBeforeMove,
                playedMoveUci,
                analysisBeforeMove,
                resultingEvaluation).annotation();
    }

    public MoveAnnotationAssessment assess(
            Game positionBeforeMove,
            String playedMoveUci,
            DeepAnalysisResult analysisBeforeMove,
            double resultingEvaluation) {
        return assess(
                positionBeforeMove,
                playedMoveUci,
                analysisBeforeMove,
                resultingEvaluation,
                null);
    }

    /**
     * Classifies a move and optionally includes the evaluation after one
     * forced reply. Some engines stop a one-legal-move root before seeing the
     * tactic beyond that reply. The extension is additional evidence only:
     * it may improve the played move's score, but never make it worse.
     */
    public MoveAnnotation classify(
            Game positionBeforeMove,
            String playedMoveUci,
            DeepAnalysisResult analysisBeforeMove,
            double resultingEvaluation,
            Double forcedReplyEvaluation) {
        return assess(
                positionBeforeMove,
                playedMoveUci,
                analysisBeforeMove,
                resultingEvaluation,
                forcedReplyEvaluation).annotation();
    }

    /**
     * Returns both the user-facing annotation and debug diagnostics explaining
     * why a sound best move did or did not qualify for extraordinary status.
     */
    public MoveAnnotationAssessment assess(
            Game positionBeforeMove,
            String playedMoveUci,
            DeepAnalysisResult analysisBeforeMove,
            double resultingEvaluation,
            Double forcedReplyEvaluation) {
        if (positionBeforeMove == null
                || playedMoveUci == null
                || playedMoveUci.isBlank()
                || analysisBeforeMove == null
                || analysisBeforeMove.getFinalLines().isEmpty()) {
            return emptyAssessment();
        }

        boolean whiteMover =
                positionBeforeMove.getMoveList().size() % 2 == 0;
        List<EngineLine> candidates =
                EvaluationScoring.orderedLines(
                        analysisBeforeMove.getFinalLines());
        if (candidates.isEmpty()) {
            return emptyAssessment();
        }

        EngineLine best = candidates.get(0);
        String bestMoveUci = EvaluationScoring.firstMove(best);
        EngineLine secondBest = candidates.size() > 1 ? candidates.get(1) : null;
        String secondBestMoveUci =
                secondBest != null ? EvaluationScoring.firstMove(secondBest) : null;
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
        if (forcedReplyEvaluation != null
                && Double.isFinite(forcedReplyEvaluation)) {
            double forcedReplyScore =
                    EvaluationScoring.moverScore(
                            forcedReplyEvaluation,
                            whiteMover);
            resultingScore = Math.max(
                    resultingScore,
                    forcedReplyScore);
        }
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
            return annotationAssessment(
                    new MoveAnnotation(
                            MoveAnnotationKind.BLUNDER,
                            best.getEvaluation(),
                            winChanceLoss,
                            null,
                            bestMoveUci,
                            secondBestMoveUci),
                    null);
        }

        if (quality == ObjectiveMoveQuality.MISTAKE) {
            return annotationAssessment(
                    new MoveAnnotation(
                            MoveAnnotationKind.MISTAKE,
                            best.getEvaluation(),
                            winChanceLoss,
                            null,
                            bestMoveUci,
                            secondBestMoveUci),
                    null);
        }

        HumanAnnotationHorizon.Selection humanSelection =
                humanAnnotationHorizon.select(
                        analysisBeforeMove,
                        playedMoveUci,
                        whiteMover);
        List<EngineLine> humanCandidates = humanSelection.lines();
        if (humanCandidates.isEmpty()) {
            return annotationAssessment(null, null);
        }

        EngineLine humanBest = humanCandidates.get(0);
        EngineLine humanSecond = humanCandidates.size() > 1
                ? humanCandidates.get(1)
                : null;
        String humanBestMoveUci = EvaluationScoring.firstMove(humanBest);
        String humanSecondBestMoveUci = humanSecond != null
                ? EvaluationScoring.firstMove(humanSecond)
                : null;
        int humanPlayedIndex = EvaluationScoring.findMoveIndex(
                humanCandidates,
                playedMoveUci);
        boolean routineMove = routineMoveDetector.isRoutine(
                positionBeforeMove,
                playedMoveUci,
                whiteMover);

        ExtraordinaryMoveDetector.Assessment extraordinaryAssessment =
                extraordinaryMoveDetector.assess(
                        positionBeforeMove,
                        analysisBeforeMove,
                        humanCandidates,
                        playedMoveUci,
                        whiteMover);
        ExtraordinaryMoveDetector.ExtraordinaryEvidence extraordinary =
                extraordinaryAssessment.evidence();
        MoveAnnotationDiagnostics diagnostics =
                extraordinaryAssessment.diagnostics();

        if (humanSelection.certified()
                && extraordinary != null
                && (!routineMove
                        || extraordinary.getMaterialInvestment() != null)) {
            return annotationAssessment(
                    new MoveAnnotation(
                            MoveAnnotationKind.EXTRAORDINARY,
                            humanBest.getEvaluation(),
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
                            extraordinary.getForcedMateDistance(),
                            humanBestMoveUci,
                            humanSecondBestMoveUci),
                    diagnostics);
        }

        if (humanSelection.certified()
                && !routineMove
                && humanPlayedIndex == 0
                && humanCandidates.size() > 1) {
            EngineLine second = humanCandidates.get(1);
            double humanBestScore =
                    EvaluationScoring.moverScore(
                            humanBest.getEvaluation(),
                            whiteMover);
            double secondScore =
                    EvaluationScoring.moverScore(
                            second.getEvaluation(),
                            whiteMover);
            double winPercentGap =
                    EvaluationScoring
                            .winPercentFromMoverScore(humanBestScore)
                    - EvaluationScoring
                            .winPercentFromMoverScore(secondScore);

            if (winPercentGap
                    >= MoveAnnotationPolicy
                            .ONLY_MOVE_FINAL_WIN_PERCENT_GAP
                    && !onlyMoveDetector.isTrivial(
                            analysisBeforeMove,
                            playedMoveUci,
                            whiteMover,
                            humanSelection.depth())) {
                return annotationAssessment(
                        new MoveAnnotation(
                                MoveAnnotationKind.ONLY_MOVE,
                                humanBest.getEvaluation(),
                                null,
                                second.getEvaluation(),
                                humanBestMoveUci,
                                humanSecondBestMoveUci),
                        diagnostics);
            }
        }

        return annotationAssessment(null, diagnostics);
    }

    private MoveAnnotationAssessment emptyAssessment() {
        return new MoveAnnotationAssessment(null, null);
    }

    private MoveAnnotationAssessment annotationAssessment(
            MoveAnnotation annotation,
            MoveAnnotationDiagnostics diagnostics) {
        return new MoveAnnotationAssessment(annotation, diagnostics);
    }
}
