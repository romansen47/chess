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
        MoveAssessmentContext context = MoveAssessmentContext.from(
                positionBeforeMove,
                playedMoveUci,
                analysisBeforeMove);
        if (context == null) {
            return emptyAssessment();
        }

        MoveAnnotation objectiveError = classifyObjectiveError(
                context,
                resultingEvaluation,
                forcedReplyEvaluation);
        if (objectiveError != null) {
            return annotationAssessment(objectiveError, null);
        }

        return classifyPositiveAnnotation(context);
    }

    private MoveAnnotation classifyObjectiveError(
            MoveAssessmentContext context,
            double resultingEvaluation,
            Double forcedReplyEvaluation) {
        EngineLine best = context.objectiveBest();
        int playedIndex = context.objectivePlayedIndex();

        double bestWinPercent = EvaluationScoring.winningChance(
                best,
                context.whiteMover());

        double resultingScore =
                EvaluationScoring.moverScore(
                        resultingEvaluation,
                        context.whiteMover());
        if (forcedReplyEvaluation != null
                && Double.isFinite(forcedReplyEvaluation)) {
            double forcedReplyScore =
                    EvaluationScoring.moverScore(
                            forcedReplyEvaluation,
                            context.whiteMover());
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
            double rootWinChanceLoss = EvaluationScoring.regret(
                    best,
                    context.objectiveCandidates().get(playedIndex),
                    context.whiteMover());
            winChanceLoss =
                    Math.min(
                            rootWinChanceLoss,
                            resultingWinChanceLoss);
        }

        ObjectiveMoveQuality quality =
                qualityEvaluator.evaluate(winChanceLoss);
        if (quality == ObjectiveMoveQuality.BLUNDER) {
            return MoveAnnotation.blunder(
                    best.getEvaluation(),
                    winChanceLoss,
                    context.objectiveBestMoveUci(),
                    context.objectiveSecondBestMoveUci());
        }

        if (quality == ObjectiveMoveQuality.MISTAKE) {
            return MoveAnnotation.mistake(
                    best.getEvaluation(),
                    winChanceLoss,
                    context.objectiveBestMoveUci(),
                    context.objectiveSecondBestMoveUci());
        }

        return null;
    }

    private MoveAnnotationAssessment classifyPositiveAnnotation(
            MoveAssessmentContext context) {
        HumanAnnotationHorizon.DecisionPoint humanDecision =
                humanAnnotationHorizon.select(
                        context.analysisBeforeMove(),
                        context.playedMoveUci(),
                        context.whiteMover());
        List<EngineLine> humanCandidates = humanDecision.lines();
        if (humanCandidates.isEmpty()) {
            return annotationAssessment(null, null);
        }

        RoutineMoveReason routineReason = routineMoveDetector.detect(
                context.positionBeforeMove(),
                context.playedMoveUci(),
                context.whiteMover());

        ExtraordinaryMoveDetector.Assessment extraordinaryAssessment =
                extraordinaryMoveDetector.assess(
                        context.positionBeforeMove(),
                        context.analysisBeforeMove(),
                        humanCandidates,
                        context.playedMoveUci(),
                        context.whiteMover());
        ExtraordinaryMoveDetector.ExtraordinaryEvidence extraordinary =
                extraordinaryAssessment.evidence();
        MoveAnnotationDiagnostics diagnostics =
                extraordinaryAssessment.diagnostics();

        MoveAnnotation extraordinaryAnnotation = classifyExtraordinaryMove(
                humanDecision,
                humanCandidates,
                routineReason,
                extraordinary);
        if (extraordinaryAnnotation != null) {
            return annotationAssessment(
                    extraordinaryAnnotation,
                    diagnostics);
        }

        MoveAnnotation onlyMoveAnnotation = classifyOnlyMove(
                context,
                humanDecision,
                humanCandidates,
                routineReason);
        if (onlyMoveAnnotation != null) {
            return annotationAssessment(onlyMoveAnnotation, diagnostics);
        }

        return annotationAssessment(null, diagnostics);
    }

    private MoveAnnotation classifyExtraordinaryMove(
            HumanAnnotationHorizon.DecisionPoint humanDecision,
            List<EngineLine> humanCandidates,
            RoutineMoveReason routineReason,
            ExtraordinaryMoveDetector.ExtraordinaryEvidence extraordinary) {
        if (!humanDecision.stable()
                || extraordinary == null
                || (routineReason.isRoutine()
                        && extraordinary.getMaterialInvestment() == null)) {
            return null;
        }

        EngineLine best = humanCandidates.get(0);
        EngineLine second = humanCandidates.size() > 1
                ? humanCandidates.get(1)
                : null;
        return MoveAnnotation.extraordinary(
                best.getEvaluation(),
                extraordinary,
                EvaluationScoring.firstMove(best),
                EvaluationScoring.firstMove(second));
    }

    private MoveAnnotation classifyOnlyMove(
            MoveAssessmentContext context,
            HumanAnnotationHorizon.DecisionPoint humanDecision,
            List<EngineLine> humanCandidates,
            RoutineMoveReason routineReason) {
        if (!humanDecision.stable()
                || routineReason.isRoutine()
                || humanCandidates.size() < 2
                || EvaluationScoring.findMoveIndex(
                        humanCandidates,
                        context.playedMoveUci()) != 0) {
            return null;
        }

        EngineLine best = humanCandidates.get(0);
        EngineLine second = humanCandidates.get(1);
        double winPercentGap = EvaluationScoring.gap(
                best,
                second,
                context.whiteMover());
        if (winPercentGap
                < MoveAnnotationPolicy.ONLY_MOVE_FINAL_WIN_PERCENT_GAP
                || onlyMoveDetector.isTrivial(
                        context.analysisBeforeMove(),
                        context.playedMoveUci(),
                        context.whiteMover(),
                        humanDecision.depth())) {
            return null;
        }

        return MoveAnnotation.onlyMove(
                best.getEvaluation(),
                second.getEvaluation(),
                EvaluationScoring.firstMove(best),
                EvaluationScoring.firstMove(second));
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
