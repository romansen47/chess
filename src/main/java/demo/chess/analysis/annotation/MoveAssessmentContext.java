package demo.chess.analysis.annotation;

import java.util.List;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.Game;

/**
 * Immutable facts shared by every rule that assesses one played move.
 *
 * <p>Creating this context once keeps validation, mover orientation and the
 * engine-authoritative MultiPV order out of the individual annotation rules.</p>
 */
record MoveAssessmentContext(
        Game positionBeforeMove,
        String playedMoveUci,
        DeepAnalysisResult analysisBeforeMove,
        boolean whiteMover,
        List<EngineLine> objectiveCandidates) {

    static MoveAssessmentContext from(
            Game positionBeforeMove,
            String playedMoveUci,
            DeepAnalysisResult analysisBeforeMove) {
        if (positionBeforeMove == null
                || playedMoveUci == null
                || playedMoveUci.isBlank()
                || analysisBeforeMove == null
                || analysisBeforeMove.getFinalLines().isEmpty()) {
            return null;
        }

        List<EngineLine> candidates = EvaluationScoring.orderedLines(
                analysisBeforeMove.getFinalLines());
        if (candidates.isEmpty()) {
            return null;
        }

        boolean whiteMover = positionBeforeMove.getMoveList().size() % 2 == 0;
        return new MoveAssessmentContext(
                positionBeforeMove,
                playedMoveUci,
                analysisBeforeMove,
                whiteMover,
                candidates);
    }

    EngineLine objectiveBest() {
        return objectiveCandidates.get(0);
    }

    EngineLine objectiveSecondBest() {
        return objectiveCandidates.size() > 1
                ? objectiveCandidates.get(1)
                : null;
    }

    int objectivePlayedIndex() {
        return EvaluationScoring.findMoveIndex(
                objectiveCandidates,
                playedMoveUci);
    }

    String objectiveBestMoveUci() {
        return EvaluationScoring.firstMove(objectiveBest());
    }

    String objectiveSecondBestMoveUci() {
        return EvaluationScoring.firstMove(objectiveSecondBest());
    }
}
