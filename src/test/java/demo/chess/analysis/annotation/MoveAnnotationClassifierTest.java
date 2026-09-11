package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
    public void closeOpeningRankShuffleDoesNotCountAsDeepDiscovery() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.45, 20, "d2d4 d7d5"),
                        line(0.42, 20, "e2e4 e7e5"),
                        line(0.40, 20, "g1f3 g8f6")),
                history(
                        depth(6,
                                line(0.40, 6, "d2d4"),
                                line(0.39, 6, "g1f3"),
                                line(0.38, 6, "c2c4"),
                                line(0.37, 6, "e2e4")),
                        depth(10,
                                line(0.41, 10, "d2d4"),
                                line(0.40, 10, "g1f3"),
                                line(0.39, 10, "c2c4"),
                                line(0.38, 10, "e2e4")),
                        depth(15,
                                line(0.44, 15, "d2d4"),
                                line(0.42, 15, "e2e4"),
                                line(0.40, 15, "g1f3")),
                        depth(18,
                                line(0.45, 18, "d2d4"),
                                line(0.42, 18, "e2e4"),
                                line(0.40, 18, "g1f3")),
                        depth(20,
                                line(0.45, 20, "d2d4"),
                                line(0.42, 20, "e2e4"),
                                line(0.40, 20, "g1f3"))));

        MoveAnnotation annotation = classifier.classify(root, "e2e4", result, 0.42);

        assertNull(annotation);
    }

    @Test
    public void moveWithLargeRegretReductionReceivesBrilliantMark() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(1.5, 20, "e2e4 e7e5"),
                        line(1.0, 20, "d2d4 d7d5"),
                        line(0.8, 20, "g1f3 g8f6")),
                history(
                        // Early phase (25-40%): e4 is substantially behind.
                        depth(5,
                                line(0.5, 5, "d2d4"),
                                line(0.3, 5, "g1f3"),
                                line(-1.0, 5, "e2e4")),
                        depth(6,
                                line(0.5, 6, "d2d4"),
                                line(0.3, 6, "g1f3"),
                                line(-1.0, 6, "e2e4")),
                        depth(8,
                                line(0.6, 8, "d2d4"),
                                line(0.4, 8, "g1f3"),
                                line(-0.9, 8, "e2e4")),
                        // Middle phase (45-65%): the regret is shrinking.
                        depth(10,
                                line(0.7, 10, "d2d4"),
                                line(-0.1, 10, "e2e4"),
                                line(-0.2, 10, "g1f3")),
                        depth(12,
                                line(0.8, 12, "d2d4"),
                                line(0.0, 12, "e2e4"),
                                line(-0.2, 12, "g1f3")),
                        // Late phase (75-100%): e4 is stable in the Top 3 and
                        // finally becomes the best move.
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
    public void kramnikLekoQd3IsStrengthDiscoveryBrilliant() throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "g1f3", "b8c6",
                "f1b5", "a7a6",
                "b5a4", "g8f6",
                "e1g1", "f8e7",
                "f1e1", "b7b5",
                "a4b3", "e8g8",
                "c2c3", "d7d5",
                "e4d5", "f6d5",
                "f3e5", "c6e5",
                "e1e5", "c7c6",
                "d2d4", "e7d6",
                "e5e1", "d8h4",
                "g2g3", "h4h3",
                "e1e4", "g7g5",
                "d1f1", "h3h5",
                "b1d2", "c8f5",
                "f2f3", "d5f6",
                "e4e1", "a8e8",
                "e1e8", "f8e8",
                "a2a4", "h5g6",
                "a4b5", "f5d3",
                "f1f2", "e8e2",
                "f2e2", "d3e2",
                "b5a6");

        DeepAnalysisResult result = result(
                List.of(
                        line(-5.83, 20, "g6d3 g1f2"),
                        line(-3.15, 20, "g8g7 a1a5"),
                        line(-0.92, 20, "e2a6 a1a6")),
                history(
                        // Actual Stockfish-19 shape from the supplied Qd3 log:
                        // already a plausible candidate early, but its
                        // practical strength grows enormously with search.
                        depth(5,
                                line(-0.54, 5, "g6d3"),
                                line(-0.54, 5, "d6b8"),
                                line(-0.15, 5, "g8g7")),
                        depth(6,
                                line(-1.90, 6, "d6b8"),
                                line(-1.04, 6, "g6d3"),
                                line(-0.55, 6, "g8g7")),
                        depth(7,
                                line(-1.23, 7, "d6b8"),
                                line(-1.08, 7, "e2a6"),
                                line(-0.97, 7, "g6d3")),
                        depth(8,
                                line(-1.37, 8, "g6d3"),
                                line(-1.17, 8, "e2a6"),
                                line(-0.93, 8, "g8g7")),
                        depth(10,
                                line(-1.91, 10, "g6d3"),
                                line(-1.45, 10, "e2a6"),
                                line(-1.39, 10, "g8g7")),
                        depth(12,
                                line(-3.12, 12, "g6d3"),
                                line(-1.57, 12, "g8g7"),
                                line(-0.87, 12, "e2a6")),
                        depth(15,
                                line(-5.31, 15, "g6d3"),
                                line(-2.54, 15, "g8g7"),
                                line(-1.14, 15, "e2a6")),
                        depth(16,
                                line(-6.25, 16, "g6d3"),
                                line(-2.44, 16, "g8g7"),
                                line(-0.96, 16, "e2a6")),
                        depth(18,
                                line(-6.73, 18, "g6d3"),
                                line(-1.96, 18, "g8g7"),
                                line(-0.76, 18, "e2a6")),
                        depth(20,
                                line(-5.83, 20, "g6d3"),
                                line(-3.15, 20, "g8g7"),
                                line(-0.92, 20, "e2a6"))));

        MoveAnnotation annotation = classifier.classify(root, "g6d3", result, -5.83);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BRILLIANT, annotation.getKind());
        assertEquals(BrilliantReason.DEEP_DISCOVERY, annotation.getBrilliantReason());
    }

    @Test
    public void stableBestMoveWithSmallStrengthGrowthIsNotBrilliant() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.65, 20, "e2e4 e7e5"),
                        line(0.60, 20, "d2d4 d7d5"),
                        line(0.55, 20, "g1f3 g8f6")),
                history(
                        depth(5,
                                line(0.40, 5, "e2e4"),
                                line(0.38, 5, "d2d4"),
                                line(0.35, 5, "g1f3")),
                        depth(8,
                                line(0.45, 8, "e2e4"),
                                line(0.42, 8, "d2d4"),
                                line(0.40, 8, "g1f3")),
                        depth(10,
                                line(0.50, 10, "e2e4"),
                                line(0.47, 10, "d2d4"),
                                line(0.45, 10, "g1f3")),
                        depth(12,
                                line(0.55, 12, "e2e4"),
                                line(0.52, 12, "d2d4"),
                                line(0.50, 12, "g1f3")),
                        depth(15,
                                line(0.60, 15, "e2e4"),
                                line(0.57, 15, "d2d4"),
                                line(0.55, 15, "g1f3")),
                        depth(18,
                                line(0.63, 18, "e2e4"),
                                line(0.59, 18, "d2d4"),
                                line(0.56, 18, "g1f3")),
                        depth(20,
                                line(0.65, 20, "e2e4"),
                                line(0.60, 20, "d2d4"),
                                line(0.55, 20, "g1f3"))));

        MoveAnnotation annotation = classifier.classify(root, "e2e4", result, 0.65);

        assertNull(annotation);
    }

    @Test
    public void soundQueenInvestmentCanReceiveBrilliantMarkWithoutEngineDependency()
            throws Exception {
        Game root = positionAfter("e2e4", "e7e5", "d1h5", "b8c6");

        DeepAnalysisResult result = result(
                List.of(
                        line(0.4, 20, "g1f3 g8f6"),
                        line(0.3, 20, "h5e5 c6e5"),
                        line(0.2, 20, "f1c4 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "h5e5", result, 0.3);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BRILLIANT, annotation.getKind());
        assertEquals(
                BrilliantReason.MATERIAL_INVESTMENT,
                annotation.getBrilliantReason());
        assertEquals(8.0, annotation.getMaterialInvestment(), 0.001);
    }

    @Test
    public void nezhmetdinovQueenSacrificeWithThreePointNetInvestmentIsBrilliant()
            throws Exception {
        Game root = positionAfter(
                "e2e4", "c7c5",
                "g1f3", "b8c6",
                "d2d4", "c5d4",
                "f3d4", "g7g6",
                "b1c3", "f8g7",
                "c1e3", "g8f6",
                "f1c4", "e8g8",
                "c4b3", "f6g4",
                "d1g4", "c6d4",
                "g4h4", "d8a5",
                "e1g1", "g7f6");

        DeepAnalysisResult result = result(
                List.of(
                        line(0.40, 20, "c3d5 e7e6"),
                        line(0.35, 20, "h4f6 d4e2 c3e2 e7f6"),
                        line(0.20, 20, "a1d1 d7d6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "h4f6", result, 0.35);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BRILLIANT, annotation.getKind());
        assertEquals(
                BrilliantReason.MATERIAL_INVESTMENT,
                annotation.getBrilliantReason());
        assertEquals(3.0, annotation.getMaterialInvestment(), 0.001);
    }

    @Test
    public void objectivelyBadQueenSacrificeRemainsBlunder() throws Exception {
        Game root = positionAfter("e2e4", "e7e5", "d1h5", "b8c6");

        DeepAnalysisResult result = result(
                List.of(
                        line(0.4, 20, "g1f3 g8f6"),
                        line(-4.0, 20, "h5e5 c6e5"),
                        line(-5.0, 20, "f1c4 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "h5e5", result, -4.0);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BLUNDER, annotation.getKind());
    }

    @Test
    public void unrelatedLaterMaterialLossDoesNotMakeRootMoveBrilliant() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.4, 20, "e2e4 e7e5 d1h5 b8c6 h5e5 c6e5"),
                        line(0.3, 20, "d2d4 d7d5"),
                        line(0.2, 20, "g1f3 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "e2e4", result, 0.4);

        assertNull(annotation);
    }

    @Test
    public void ordinaryEqualExchangeDoesNotCountAsMaterialSacrifice() throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "g1f3", "b8c6",
                "f1b5", "a7a6");

        DeepAnalysisResult result = result(
                List.of(
                        line(0.3, 20, "b5c6 d7c6"),
                        line(0.2, 20, "b5a4 g8f6"),
                        line(0.1, 20, "b5e2 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "b5c6", result, 0.3);

        assertNull(annotation);
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
    public void largePawnEvaluationDropInWonPositionIsNotAutomaticallyABlunder() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(17.77, 20, "e2e4 e7e5"),
                        line(12.82, 20, "d2d4 d7d5"),
                        line(12.00, 20, "g1f3 g8f6")),
                Map.of());

        MoveAnnotation annotation = classifier.classify(root, "d2d4", result, 12.82);

        // Roughly +17.77 -> +12.82 is almost five pawns of raw engine
        // evaluation, but less than one percentage point of practical winning
        // chance. It must therefore not become ? or ??.
        assertNull(annotation);
    }

    @Test
    public void tenPointPracticalWinChanceLossReceivesMistakeMark() {
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
    public void twentyFivePointPracticalWinChanceLossReceivesBlunderMark() {
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
