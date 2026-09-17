package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

public class MoveAnnotationClassifierResultingEvaluationTest {

    private final MoveAnnotationClassifier classifier =
            new MoveAnnotationClassifier();

    @Test
    public void resultingPositionEvaluationOverridesPlayedMultiPvLineForQuality() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult analysisBeforeMove = new DeepAnalysisResult(
                List.of(
                        line(0.5, "e2e4 e7e5"),
                        line(0.4, "d2d4 d7d5"),
                        line(0.3, "g1f3 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(
                root,
                "d2d4",
                analysisBeforeMove,
                -3.0);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.BLUNDER,
                annotation.getKind());
    }

    private EngineLine line(double evaluation, String moves) {
        return new EngineLine(
                evaluation,
                20,
                null,
                moves);
    }
}
