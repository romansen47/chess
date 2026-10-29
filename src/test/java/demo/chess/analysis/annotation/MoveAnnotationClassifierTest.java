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
        assertEquals("e2e4", annotation.getBestMoveUci());
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
    public void materialSacrificeCanCarryForcedMateDiagnostic() throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "d1h5", "b8c6");

        MoveAnnotation annotation = classifier.classify(
                root,
                "h5f7",
                result(
                        List.of(
                                mateLine(99.0, 20, 3, "h5f7 e8f7"),
                                line(0.30, 20, "g1f3 g8f6"),
                                line(0.20, 20, "f1c4 g8f6")),
                        Map.of()),
                99.0);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.EXTRAORDINARY,
                annotation.getKind());
        assertEquals(Integer.valueOf(3), annotation.getForcedMateDistance());
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
    public void engineMultiPvOrderIsAuthoritativeForBlack()
            throws Exception {
        Game root = positionAfter("e2e4");

        MoveAnnotation annotation = classifier.classify(
                root,
                "e7e5",
                result(
                        List.of(
                                line(-0.5, 20, "e7e5 g1f3"),
                                line(2.0, 20, "c7c5 g1f3"),
                                line(2.5, 20, "g8f6 e4e5")),
                        Map.of()),
                -0.5);

        assertNotNull(annotation);
        assertEquals(
                MoveAnnotationKind.ONLY_MOVE,
                annotation.getKind());
    }

    @Test
    public void engineMultiPvOrderOverridesNonMonotonicLeelaScores() {
        Game root = Simulation.createSimulation();

        MoveAnnotation annotation = classifier.classify(
                root,
                "b2b4",
                result(
                        List.of(
                                line(-6.05, 16, "b2b4 a7a6"),
                                line(-4.28, 16, "e2e4 e7e5"),
                                line(-5.33, 16, "c2c4 c7c5")),
                        Map.of()),
                -6.05);

        assertNull(
                "The engine's MultiPV #1 must not be marked as a mistake merely "
                        + "because another PV has a numerically higher Leela score",
                annotation);
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

    @Test
    public void newlyAttackedPieceRetreatIsRoutine() throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "g1f3", "b8c6",
                "f1b5", "a7a6",
                "b5a4", "g8f6",
                "e1h1", "f8e7",
                "f1e1", "b7b5");

        MoveAnnotation annotation = classifier.classify(
                root,
                "a4b3",
                result(
                        List.of(
                                line(0.26, 20, "a4b3 e8h8"),
                                line(-3.28, 20, "d2d4 e5d4"),
                                line(-3.50, 20, "c2c3 b5a4")),
                        Map.of()),
                0.26);

        assertNull(annotation);
    }

    @Test
    public void evenImmediateRookRecaptureIsRoutine() throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5", "g1f3", "b8c6",
                "f1b5", "a7a6", "b5a4", "g8f6",
                "e1h1", "f8e7", "f1e1", "b7b5",
                "a4b3", "e8h8", "c2c3", "d7d5",
                "e4d5", "f6d5", "f3e5", "c6e5",
                "e1e5", "c7c6", "d2d4", "e7d6",
                "e5e1", "d8h4", "g2g3", "h4h3",
                "e1e4", "g7g5", "d1f1", "h3h5",
                "b1d2", "d6f5", "f2f3", "d5f6",
                "e4e1", "a8e8", "e1e8");

        MoveAnnotation annotation = classifier.classify(
                root,
                "f8e8",
                result(
                        List.of(
                                line(-1.0, 20, "f8e8 a2a4"),
                                line(2.0, 20, "g8h8 a2a4"),
                                line(2.5, 20, "f6e8 a2a4")),
                        Map.of()),
                -1.0);

        assertNull(annotation);
    }

    @Test
    public void humanDepthCertificateSurvivesDeeperRankChange() {
        Game root = Simulation.createSimulation();
        DeepAnalysisResult result = result(
                List.of(
                        line(2.25, 45, "d2d4 d7d5"),
                        line(2.20, 45, "e2e4 e7e5"),
                        line(0.60, 45, "g1f3 g8f6")),
                history(
                        depth(5,
                                line(0.60, 5, "d2d4"),
                                line(-1.00, 5, "e2e4"),
                                line(-1.20, 5, "g1f3")),
                        depth(8,
                                line(0.70, 8, "d2d4"),
                                line(-0.80, 8, "e2e4"),
                                line(-1.00, 8, "g1f3")),
                        depth(16,
                                line(2.10, 16, "e2e4"),
                                line(0.80, 16, "d2d4"),
                                line(0.60, 16, "g1f3")),
                        depth(18,
                                line(2.15, 18, "e2e4"),
                                line(0.85, 18, "d2d4"),
                                line(0.60, 18, "g1f3")),
                        depth(20,
                                line(2.20, 20, "e2e4"),
                                line(0.90, 20, "d2d4"),
                                line(0.60, 20, "g1f3"))));

        MoveAnnotation annotation = classifier.classify(
                root,
                "e2e4",
                result,
                2.20);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.EXTRAORDINARY, annotation.getKind());
        assertEquals(Integer.valueOf(20), annotation.getFinalDepth());
    }

    @Test
    public void deeperObjectiveRefutationOverridesHumanCertificate() {
        Game root = Simulation.createSimulation();
        DeepAnalysisResult result = result(
                List.of(
                        line(5.0, 45, "d2d4 d7d5"),
                        line(-5.0, 45, "e2e4 e7e5"),
                        line(-5.5, 45, "g1f3 g8f6")),
                history(
                        depth(5,
                                line(0.60, 5, "d2d4"),
                                line(-1.00, 5, "e2e4"),
                                line(-1.20, 5, "g1f3")),
                        depth(8,
                                line(0.70, 8, "d2d4"),
                                line(-0.80, 8, "e2e4"),
                                line(-1.00, 8, "g1f3")),
                        depth(16,
                                line(2.10, 16, "e2e4"),
                                line(0.80, 16, "d2d4"),
                                line(0.60, 16, "g1f3")),
                        depth(18,
                                line(2.15, 18, "e2e4"),
                                line(0.85, 18, "d2d4"),
                                line(0.60, 18, "g1f3")),
                        depth(20,
                                line(2.20, 20, "e2e4"),
                                line(0.90, 20, "d2d4"),
                                line(0.60, 20, "g1f3"))));

        MoveAnnotation annotation = classifier.classify(
                root,
                "e2e4",
                result,
                -5.0);

        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.BLUNDER, annotation.getKind());
    }


    @Test
    public void recoveredInvestmentFallsThroughToOnlyMove() throws Exception {
        Game root = positionAfter("g1f3", "g8f6", "c2c4", "g7g6");
        String pv = "b1c3 d7d5 c4d5 f6d5 h2h4 d5c3 h4h5 g6g5 b2c3";
        MoveAnnotation annotation = classifier.classify(root, "b1c3",
                result(List.of(line(3, 20, pv), line(0, 20, "d2d4"),
                        line(-1, 20, "h2h4")), Map.of()), 3);
        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.ONLY_MOVE, annotation.getKind());
        assertEquals("b1c3", annotation.getBestMoveUci());
        assertEquals("d2d4", annotation.getSecondBestMoveUci());
    }

    @Test
    public void recoveredInvestmentWithoutOtherReasonIsUnannotated() throws Exception {
        Game root = positionAfter("g1f3", "g8f6", "c2c4", "g7g6");
        MoveAnnotation annotation = classifier.classify(root, "b1c3",
                result(List.of(
                        line(0.4, 20, "b1c3 d7d5 c4d5 f6d5 h2h4 d5c3 h4h5 g6g5 b2c3"),
                        line(0.3, 20, "d2d4"), line(0.2, 20, "h2h4")), Map.of()), 0.4);
        assertNull(annotation);
    }

    @Test
    public void recoveredInvestmentPreservesIndependentDiscovery() throws Exception {
        Game root = positionAfter("g1f3", "g8f6", "c2c4", "g7g6");
        MoveAnnotation annotation = classifier.classify(root, "b1c3",
                result(List.of(
                        line(2.2, 20, "b1c3 d7d5 c4d5 f6d5 h2h4 d5c3 h4h5 g6g5 b2c3"),
                        line(0.8, 20, "d2d4"), line(0.6, 20, "h2h4")),
                        history(depth(5, line(0.6, 5, "d2d4"), line(-1, 5, "b1c3"), line(-1.2, 5, "h2h4")),
                                depth(8, line(0.7, 8, "d2d4"), line(-0.8, 8, "b1c3"), line(-1, 8, "h2h4")))),
                2.2);
        assertNotNull(annotation);
        assertEquals(MoveAnnotationKind.EXTRAORDINARY, annotation.getKind());
        assertEquals(ExtraordinaryReason.DEEP_DISCOVERY, annotation.getExtraordinaryReason());
        assertNull(annotation.getMaterialInvestment());
    }


    @Test
    public void deepDiscoveryWindowDoesNotMoveWithFinalSearchDepth() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(2.20, 30, "e2e4 e7e5"),
                        line(0.80, 30, "d2d4 d7d5"),
                        line(0.60, 30, "g1f3 g8f6")),
                history(
                        depth(5,
                                line(0.60, 5, "d2d4"),
                                line(-1.00, 5, "e2e4"),
                                line(-1.20, 5, "g1f3")),
                        depth(8,
                                line(0.70, 8, "d2d4"),
                                line(-0.80, 8, "e2e4"),
                                line(-1.00, 8, "g1f3")),
                        depth(16,
                                line(2.10, 16, "e2e4"),
                                line(0.80, 16, "d2d4"),
                                line(0.60, 16, "g1f3")),
                        depth(18,
                                line(2.15, 18, "e2e4"),
                                line(0.80, 18, "d2d4"),
                                line(0.60, 18, "g1f3")),
                        depth(20,
                                line(2.20, 20, "e2e4"),
                                line(0.80, 20, "d2d4"),
                                line(0.60, 20, "g1f3")),
                        depth(30,
                                line(2.20, 30, "e2e4"),
                                line(0.80, 30, "d2d4"),
                                line(0.60, 30, "g1f3"))));

        MoveAnnotationAssessment assessment =
                classifier.assess(root, "e2e4", result, 2.20);

        assertNotNull(assessment.annotation());
        assertEquals(
                MoveAnnotationKind.EXTRAORDINARY,
                assessment.annotation().getKind());
        assertNotNull(assessment.diagnostics());
        assertEquals(
                MoveAnnotationPolicy.EXTRAORDINARY_DISCOVERY_EARLY_START_DEPTH,
                assessment.diagnostics().getEarlyWindowStartDepth());
        assertEquals(
                MoveAnnotationPolicy.EXTRAORDINARY_DISCOVERY_EARLY_END_DEPTH,
                assessment.diagnostics().getEarlyWindowEndDepth());
        assertEquals(2, assessment.diagnostics().getEarlySampleCount());
    }

    @Test
    public void rejectedDeepDiscoveryKeepsDiagnosticReason() {
        Game root = Simulation.createSimulation();

        DeepAnalysisResult result = result(
                List.of(
                        line(0.70, 30, "e2e4 e7e5"),
                        line(0.60, 30, "d2d4 d7d5"),
                        line(0.50, 30, "g1f3 g8f6")),
                history(
                        depth(5,
                                line(0.40, 5, "d2d4"),
                                line(0.30, 5, "e2e4"),
                                line(0.20, 5, "g1f3")),
                        depth(8,
                                line(0.45, 8, "d2d4"),
                                line(0.35, 8, "e2e4"),
                                line(0.25, 8, "g1f3")),
                        depth(30,
                                line(0.70, 30, "e2e4"),
                                line(0.60, 30, "d2d4"),
                                line(0.50, 30, "g1f3"))));

        MoveAnnotationAssessment assessment =
                classifier.assess(root, "e2e4", result, 0.70);

        assertNull(assessment.annotation());
        assertNotNull(assessment.diagnostics());
        assertEquals(
                DeepDiscoveryRejectionReason.EARLY_REGRET_TOO_SMALL,
                assessment.diagnostics().getDeepDiscoveryRejectedBy());
        assertEquals(2, assessment.diagnostics().getEarlySampleCount());
        assertNotNull(assessment.diagnostics().getEarlyRegret());
        assertNotNull(assessment.diagnostics().getStrengthGain());
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

    private EngineLine mateLine(
            double evaluation,
            int depth,
            int mateDistance,
            String moves) {
        return new EngineLine(
                evaluation,
                depth,
                mateDistance,
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
