package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

public class MoveAnnotationClassifierTest {

    private final MoveAnnotationClassifier classifier = new MoveAnnotationClassifier();

    @Test
    public void obviousEarlyBestMoveDoesNotReceiveExclamationMark() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.5, 20, "e2e4 e7e5"),
                        line(-2.5, 20, "d2d4 d7d5"),
                        line(-3.0, 20, "g1f3 g8f6")),
                history(
                        depth(6,
                                line(0.5, 6, "e2e4"),
                                line(-2.5, 6, "d2d4"),
                                line(-3.0, 6, "g1f3")),
                        depth(8,
                                line(-3.0, 8, "g1f3"),
                                line(0.5, 8, "e2e4"),
                                line(-2.5, 8, "d2d4")),
                        depth(10,
                                line(-2.5, 10, "d2d4"),
                                line(-3.0, 10, "g1f3"),
                                line(0.5, 10, "e2e4")),
                        depth(20,
                                line(0.5, 20, "e2e4"),
                                line(-2.5, 20, "d2d4"),
                                line(-3.0, 20, "g1f3"))));

        MoveAnnotation annotation = classifier.classify(root, "e2e4", result, 0.5);

        assertNull(annotation);
    }

    @Test
    public void criticalNonTrivialBestMoveReceivesExclamationMark() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                // Deliberately shuffled: MultiPV emission order must not matter.
                List.of(
                        line(-2.0, 20, "d2d4 d7d5"),
                        line(0.5, 20, "e2e4 e7e5"),
                        line(-2.5, 20, "g1f3 g8f6")),
                history(
                        depth(6,
                                line(0.5, 6, "e2e4"),
                                line(0.4, 6, "d2d4"),
                                line(0.3, 6, "g1f3")),
                        depth(8,
                                line(0.4, 8, "d2d4"),
                                line(0.5, 8, "e2e4"),
                                line(0.3, 8, "g1f3")),
                        depth(10,
                                line(0.5, 10, "e2e4"),
                                line(0.4, 10, "d2d4"),
                                line(0.3, 10, "g1f3")),
                        depth(15,
                                line(0.5, 15, "e2e4"),
                                line(-1.8, 15, "d2d4"),
                                line(-2.0, 15, "g1f3")),
                        depth(18,
                                line(0.5, 18, "e2e4"),
                                line(-1.9, 18, "d2d4"),
                                line(-2.2, 18, "g1f3")),
                        depth(20,
                                line(0.5, 20, "e2e4"),
                                line(-2.0, 20, "d2d4"),
                                line(-2.5, 20, "g1f3"))));

        MoveAnnotation annotation = classifier.classify(root, "e2e4", result, 0.5);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.ONLY_MOVE, annotation.getKind());
    }

    @Test
    public void moveDiscoveredOnlyAtHigherDepthReceivesBrilliantMark() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(1.5, 20, "e2e4 e7e5"),
                        line(1.0, 20, "d2d4 d7d5"),
                        line(0.8, 20, "g1f3 g8f6")),
                history(
                        depth(6,
                                line(0.4, 6, "d2d4"),
                                line(0.3, 6, "g1f3"),
                                line(0.2, 6, "c2c4"),
                                line(-1.0, 6, "e2e4")),
                        depth(10,
                                line(0.5, 10, "d2d4"),
                                line(0.4, 10, "g1f3"),
                                line(0.3, 10, "c2c4"),
                                line(-1.0, 10, "e2e4")),
                        depth(15,
                                line(1.2, 15, "e2e4"),
                                line(1.0, 15, "d2d4"),
                                line(0.8, 15, "g1f3")),
                        depth(18,
                                line(1.4, 18, "e2e4"),
                                line(1.0, 18, "d2d4"),
                                line(0.8, 18, "g1f3")),
                        depth(20,
                                line(1.5, 20, "e2e4"),
                                line(1.0, 20, "d2d4"),
                                line(0.8, 20, "g1f3"))));

        MoveAnnotation annotation = classifier.classify(root, "e2e4", result, 1.5);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BRILLIANT, annotation.getKind());
        assertEquals(BrilliantReason.DEEP_DISCOVERY, annotation.getBrilliantReason());
    }

    @Test
    public void temporaryQueenInvestmentCanReceiveBrilliantMarkWithoutEngineDependency()
            throws Exception {
        Game root = positionAfter("e2e4", "e7e5", "d1h5", "b8c6");

        DeepAnalysisResult result = result(
                List.of(
                        line(0.4, 20, "g1f3 g8f6"),
                        line(-4.0, 20, "h5e5 c6e5"),
                        line(-5.0, 20, "f1c4 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "h5e5", result, -4.0);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BRILLIANT, annotation.getKind());
        assertEquals(
                BrilliantReason.MATERIAL_INVESTMENT,
                annotation.getBrilliantReason());
        assertEquals(8.0, annotation.getMaterialInvestment(), 0.001);
    }

    @Test
    public void ordinaryDevelopmentDoesNotCountAsMaterialInvestment() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.4, 20, "e2e4 e7e5 g1f3"),
                        line(0.3, 20, "d2d4 d7d5 c2c4"),
                        line(0.2, 20, "g1f3 g8f6 d2d4")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "e2e4", result, 0.4);

        assertNull(annotation);
    }

    @Test
    public void onePawnPlusLossReceivesMistakeMark() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.5, 20, "e2e4 e7e5"),
                        line(-1.0, 20, "d2d4 d7d5"),
                        line(-2.0, 20, "g1f3 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "d2d4", result, -1.0);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.MISTAKE, annotation.getKind());
    }

    @Test
    public void threePawnPlusLossReceivesBlunderMark() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.5, 20, "e2e4 e7e5"),
                        line(-3.0, 20, "d2d4 d7d5"),
                        line(-4.0, 20, "g1f3 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "d2d4", result, -3.0);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BLUNDER, annotation.getKind());
    }

    @Test
    public void blackCandidateRankingUsesBlackPointOfView() throws Exception {
        Game root = positionAfter("e2e4");

        DeepAnalysisResult result = result(
                List.of(
                        line(2.0, 20, "c7c5 g1f3"),
                        line(-0.5, 20, "e7e5 g1f3"),
                        line(2.5, 20, "g8f6 e4e5")),
                history(
                        depth(6,
                                line(-0.5, 6, "e7e5"),
                                line(-0.4, 6, "c7c5"),
                                line(-0.3, 6, "g8f6")),
                        depth(8,
                                line(-0.5, 8, "e7e5"),
                                line(-0.4, 8, "c7c5"),
                                line(-0.3, 8, "g8f6")),
                        depth(10,
                                line(-0.5, 10, "e7e5"),
                                line(-0.4, 10, "c7c5"),
                                line(-0.3, 10, "g8f6")),
                        depth(20,
                                line(-0.5, 20, "e7e5"),
                                line(2.0, 20, "c7c5"),
                                line(2.5, 20, "g8f6"))));

        MoveAnnotation annotation = classifier.classify(root, "e7e5", result, -0.5);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.ONLY_MOVE, annotation.getKind());
    }

    private Game positionAfter(String... moves) throws Exception {
        Game game = Simulation.createSimulation();
        for (String uci : moves) {
            Move move = LegalMoveResolver.resolveUci(game, uci);
            game.apply(move);
        }
        return game;
    }

    private EngineLine line(double evaluation, int depth, String moves) {
        return new EngineLine(evaluation, depth, null, moves);
    }

    private DeepAnalysisResult result(
            List<EngineLine> finalLines,
            Map<Integer, List<EngineLine>> history) {
        return new DeepAnalysisResult(finalLines, history);
    }

    @SafeVarargs
    private final Map<Integer, List<EngineLine>> history(
            Map.Entry<Integer, List<EngineLine>>... depths) {
        Map<Integer, List<EngineLine>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<EngineLine>> depth : depths) {
            result.put(depth.getKey(), depth.getValue());
        }
        return result;
    }

    private Map.Entry<Integer, List<EngineLine>> depth(
            int depth,
            EngineLine... lines) {
        return Map.entry(depth, List.of(lines));
    }
}
