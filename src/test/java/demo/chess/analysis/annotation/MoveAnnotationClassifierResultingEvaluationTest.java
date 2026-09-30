package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

public class MoveAnnotationClassifierResultingEvaluationTest {

    private final MoveAnnotationClassifier classifier =
            new MoveAnnotationClassifier();

    @Test
    public void rootBestMoveCannotBecomeMistakeFromPostMoveSearchDrop() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult analysisBeforeMove = new DeepAnalysisResult(
                List.of(
                        line(0.5, "e2e4 e7e5"),
                        line(0.4, "d2d4 d7d5"),
                        line(0.3, "g1f3 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(
                root,
                "e2e4",
                analysisBeforeMove,
                -3.0);

        assertNull(annotation);
    }

    @Test
    public void postMoveSearchCannotIncreaseLossBeyondPlayedMultiPvLine() {
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

        assertNull(annotation);
    }

    @Test
    public void postMoveSearchCanReduceLossFromPlayedMultiPvLine() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult analysisBeforeMove = new DeepAnalysisResult(
                List.of(
                        line(3.0, "e2e4 e7e5"),
                        line(0.0, "d2d4 d7d5"),
                        line(-1.0, "g1f3 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(
                root,
                "d2d4",
                analysisBeforeMove,
                2.9);

        assertNull(annotation);
    }

    @Test
    public void forcedReplyExtensionCanRevealMateBeyondShallowRoot() throws Exception {
        Game root = Simulation.createSimulation();
        root.apply(LegalMoveResolver.resolveUci(root, "e2e4"));

        DeepAnalysisResult analysisBeforeMove = new DeepAnalysisResult(
                List.of(
                        line(-2.88, "g8f6"),
                        line(-0.20, "b8c6"),
                        line(1.70, "h7h5")),
                Map.of());

        MoveAnnotation shallowAnnotation = classifier.classify(
                root,
                "h7h5",
                analysisBeforeMove,
                1.58);

        assertNotNull(shallowAnnotation);
        assertEquals(
                MoveAnnotationKind.BLUNDER,
                shallowAnnotation.getKind());

        MoveAnnotation extendedAnnotation = classifier.classify(
                root,
                "h7h5",
                analysisBeforeMove,
                1.58,
                -99.0);

        assertNull(extendedAnnotation);
    }

    @Test
    public void resultingPositionRemainsFallbackOutsideMultiPv() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult analysisBeforeMove = new DeepAnalysisResult(
                List.of(
                        line(0.5, "e2e4 e7e5"),
                        line(0.3, "g1f3 g8f6"),
                        line(0.2, "c2c4 e7e5")),
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
