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

    private final MoveAnnotationClassifier classifier =
            new MoveAnnotationClassifier();

    @Test
    public void twentyFivePointPracticalWinChanceLossIsBlunder() {
        Game root = Simulation.createSimulation();

        MoveAnnotation annotation = classifier.classify(
                root,
                "d2d4",
                result(
                        List.of(
                                line(0.5, 20, "e2e4 e7e5"),
                                line(-3.0, 20, "d2d4 d7d5"),
                                line(-4.0, 20, "g1f3 g8f6")),
                        Map.of()),
                -3.0);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.BLUNDER,
                annotation.getKind());
    }

    @Test
    public void tenPointPracticalWinChanceLossIsMistake() {
        Game root = Simulation.createSimulation();

        MoveAnnotation annotation = classifier.classify(
                root,
                "d2d4",
                result(
                        List.of(
                                line(0.5, 20, "e2e4 e7e5"),
                                line(-1.0, 20, "d2d4 d7d5"),
                                line(-2.0, 20, "g1f3 g8f6")),
                        Map.of()),
                -1.0);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.MISTAKE,
                annotation.getKind());
    }

    @Test
    public void materialSacrificeCanBeExtraordinary() throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "d1h5", "b8c6");

        MoveAnnotation annotation = classifier.classify(
                root,
                "h5f7",
                result(
                        List.of(
                                line(0.40, 20, "h5f7 e8f7"),
                                line(0.30, 20, "g1f3 g8f6"),
                                line(0.20, 20, "f1c4 g8f6")),
                        Map.of()),
                0.40);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.EXTRAORDINARY,
                annotation.getKind());
        assertEquals(
                ExtraordinaryReason.MATERIAL_SACRIFICE,
                annotation.getExtraordinaryReason());
    }

    @Test
    public void genuineStrengthDiscoveryCanBeExtraordinary() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(2.20, 20, "e2e4 e7e5"),
                        line(0.80, 20, "d2d4 d7d5"),
                        line(0.60, 20, "g1f3 g8f6")),
                history(
                        depth(5,
                                line(0.60, 5, "d2d4"),
                                line(-1.00, 5, "e2e4"),
                                line(-1.20, 5, "g1f3")),
                        depth(8,
                                line(0.70, 8, "d2d4"),
                                line(-0.80, 8, "e2e4"),
                                line(-1.00, 8, "g1f3")),
                        depth(20,
                                line(2.20, 20, "e2e4"),
                                line(0.80, 20, "d2d4"),
                                line(0.60, 20, "g1f3"))));

        MoveAnnotation annotation =
                classifier.classify(root, "e2e4", result, 2.20);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.EXTRAORDINARY,
                annotation.getKind());
        assertEquals(
                ExtraordinaryReason.DEEP_DISCOVERY,
                annotation.getExtraordinaryReason());
    }

    @Test
    public void obviousEarlyBestMoveIsNotDeepDiscovery() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(2.20, 20, "e2e4 e7e5"),
                        line(0.80, 20, "d2d4 d7d5"),
                        line(0.60, 20, "g1f3 g8f6")),
                history(
                        depth(5,
                                line(-1.00, 5, "e2e4"),
                                line(-1.20, 5, "d2d4"),
                                line(-1.40, 5, "g1f3")),
                        depth(8,
                                line(-0.80, 8, "e2e4"),
                                line(-1.00, 8, "d2d4"),
                                line(-1.20, 8, "g1f3")),
                        depth(20,
                                line(2.20, 20, "e2e4"),
                                line(0.80, 20, "d2d4"),
                                line(0.60, 20, "g1f3"))));

        MoveAnnotation annotation =
                classifier.classify(root, "e2e4", result, 2.20);

        assertNull(annotation);
    }

    @Test
    public void blackCandidateRankingUsesBlackPointOfView()
            throws Exception {
        Game root = positionAfter("e2e4");

        MoveAnnotation annotation = classifier.classify(
                root,
                "e7e5",
                result(
                        List.of(
                                line(2.0, 20, "c7c5 g1f3"),
                                line(-0.5, 20, "e7e5 g1f3"),
                                line(2.5, 20, "g8f6 e4e5")),
                        Map.of()),
                -0.5);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.ONLY_MOVE,
                annotation.getKind());
    }

    @Test
    public void ordinaryDevelopmentHasNoAnnotation() {
        Game root = Simulation.createSimulation();

        MoveAnnotation annotation = classifier.classify(
                root,
                "e2e4",
                result(
                        List.of(
                                line(0.4, 20, "e2e4 e7e5"),
                                line(0.3, 20, "d2d4 d7d5"),
                                line(0.2, 20, "g1f3 g8f6")),
                        Map.of()),
                0.4);

        assertNull(annotation);
    }

    private Game positionAfter(String... moves) throws Exception {
        Game game = Simulation.createSimulation();
        for (String uci : moves) {
            Move move = LegalMoveResolver.resolveUci(game, uci);
            game.apply(move);
        }
        return game;
    }

    private EngineLine line(
            double evaluation,
            int depth,
            String moves) {
        return new EngineLine(
                evaluation,
                depth,
                null,
                moves);
    }

    private DeepAnalysisResult result(
            List<EngineLine> finalLines,
            Map<Integer, List<EngineLine>> history) {
        return new DeepAnalysisResult(finalLines, history);
    }

    @SafeVarargs
    private final Map<Integer, List<EngineLine>> history(
            Map.Entry<Integer, List<EngineLine>>... depths) {
        Map<Integer, List<EngineLine>> result =
                new LinkedHashMap<>();
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
