package demo.chess.analysis.annotation;

import demo.chess.definitions.engines.EngineLine;

/**
 * Context rules deciding whether otherwise valid brilliance evidence is
 * eligible for a "!!" annotation.
 *
 * <p>Evidence detectors answer factual questions such as "was material
 * deliberately invested?". This policy answers the separate contextual
 * question "is that investment brilliance-worthy in this position?".</p>
 */
final class BrillianceEligibilityPolicy {

    boolean allowsMaterialSacrifice(
            EngineLine bestAvailableMove,
            boolean whiteMover) {
        if (bestAvailableMove == null) {
            return false;
        }

        double moverScore = EvaluationScoring.moverScore(
                bestAvailableMove.getEvaluation(),
                whiteMover);
        double bestWinPercent =
                EvaluationScoring.winPercentFromMoverScore(moverScore);

        return bestWinPercent
                >= MoveAnnotationPolicy.BRILLIANT_MATERIAL_MIN_BEST_WIN_PERCENT;
    }
}
