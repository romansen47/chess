package demo.chess.analysis.annotation;

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
import demo.chess.load.GameLoader;

/**
 * Observation suite for the historical moves used while designing the
 * annotation model.
 *
 * <p>These tests intentionally contain no assertions about the resulting move
 * annotation. They are executable documentation: CI prints what the current
 * algorithm thinks, so changes can be reviewed without starting a real chess
 * engine and without freezing today's judgement into regression expectations.</p>
 */
public class ExtraordinaryMoveObservationTest {

    private static final String BYRNE_FISCHER = """
            [Event "Byrne - Fischer"]
            [White "Donald Byrne"]
            [Black "Robert James Fischer"]
            [Result "*"]

            1. Nf3 Nf6 2. c4 g6 3. Nc3 Bg7 4. d4 O-O
            5. Bf4 d5 6. Qb3 dxc4 7. Qxc4 c6 8. e4 Nbd7
            9. Rd1 Nb6 10. Qc5 Bg4 11. Bg5 Na4 12. Qa3 Nxc3
            13. bxc3 Nxe4 14. Bxe7 Qb6 15. Bc4 Nxc3
            16. Bc5 Rfe8+ 17. Kf1 Be6 *
            """;

    private static final String NEZHMETDINOV_CHERNIKOV = """
            [Event "Nezhmetdinov - Chernikov"]
            [White "Nezhmetdinov, Rashid"]
            [Black "Chernikov, Oleg"]
            [Result "*"]

            1. e4 c5 2. Nf3 Nc6 3. d4 cxd4 4. Nxd4 g6
            5. Nc3 Bg7 6. Be3 Nf6 7. Bc4 O-O 8. Bb3 Ng4
            9. Qxg4 Nxd4 10. Qh4 Qa5 11. O-O Bf6
            12. Qxf6 Ne2+ 13. Nxe2 exf6 14. Nc3 Re8
            15. Nd5 Re6 16. Bd4 Kg7 17. Rad1 d6 18. Rd3 Bd7
            19. Rf3 Bb5 20. Bc3 Qd8 21. Nxf6 Be2 22. Nxh7+ Kg8
            23. Rh3 Re5 24. f4 Bxf1 25. Kxf1 Rc8 26. Bd4 *
            """;

    private static final String KRAMNIK_LEKO = """
            [Event "Kramnik - Leko"]
            [White "Kramnik, Vladimir"]
            [Black "Leko, Peter"]
            [Result "*"]

            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6
            5. O-O Be7 6. Re1 b5 7. Bb3 O-O 8. c3 d5
            9. exd5 Nxd5 10. Nxe5 Nxe5 11. Rxe5 c6
            12. d4 Bd6 13. Re1 Qh4 14. g3 Qh3 15. Re4 g5
            16. Qf1 Qh5 17. Nd2 Bf5 18. f3 Nf6 19. Re1 Rae8
            20. Rxe8 Rxe8 21. a4 Qg6 22. axb5 Bd3 23. Qf2 Re2
            24. Qxe2 Bxe2 25. bxa6 Qd3 26. Kf2 Bxf3
            27. Nxf3 Ne4+ 28. Ke1 Nxc3 29. bxc3 *
            """;

    private final MoveAnnotationClassifier classifier =
            new MoveAnnotationClassifier();
    private final GameLoader gameLoader = new GameLoader();

    @Test
    public void printHistoricalReferenceMoveClassifications()
            throws Exception {
        observe(
                "Byrne-Fischer 17...Be6",
                BYRNE_FISCHER,
                34,
                result(
                        List.of(
                                line(-3.01, 20,
                                        "g4e6 a3c3 b6c5 d4c5 g7c3 c4e6 e8e6"),
                                line(0.70, 20,
                                        "c3b5 c4f7"),
                                line(1.67, 20,
                                        "c3b1 a3c1")),
                        Map.of()),
                -3.01);

        observe(
                "Nezhmetdinov-Chernikov 12.Qxf6",
                NEZHMETDINOV_CHERNIKOV,
                23,
                result(
                        List.of(
                                line(0.40, 20,
                                        "c3d5 e7e6"),
                                line(0.35, 20,
                                        "h4f6 d4e2 c3e2 e7f6"),
                                line(0.20, 20,
                                        "a1d1 d7d6")),
                        Map.of()),
                0.35);

        observe(
                "Nezhmetdinov-Chernikov 26.Bd4",
                NEZHMETDINOV_CHERNIKOV,
                51,
                result(
                        List.of(
                                line(5.74, 15,
                                        "f4e5 d6e5 d3d8"),
                                line(2.08, 15,
                                        "c3d4 h8h5"),
                                line(-0.24, 15,
                                        "f3g5 c8c3")),
                        Map.of()),
                4.20);

        observe(
                "Kramnik-Leko 24.Qxe2",
                KRAMNIK_LEKO,
                47,
                result(
                        List.of(
                                line(-5.93, 20,
                                        "b5a6 e2f2"),
                                line(-6.21, 20,
                                        "f2e2 d3e2"),
                                line(-7.32, 20,
                                        "b5b6 e2f2")),
                        Map.of()),
                -6.28);

        observe(
                "Kramnik-Leko 24...Bxe2",
                KRAMNIK_LEKO,
                48,
                result(
                        List.of(
                                line(-6.28, 20,
                                        "d3e2 c1g5"),
                                line(7.68, 20,
                                        "g6f5 e2e3"),
                                line(7.81, 20,
                                        "a6b5 e2e3")),
                        history(
                                depth(5,
                                        line(-0.55, 5, "d3e2"),
                                        line(0.10, 5, "g6f5"),
                                        line(0.20, 5, "a6b5")),
                                depth(8,
                                        line(-0.70, 8, "d3e2"),
                                        line(0.20, 8, "g6f5"),
                                        line(0.30, 8, "a6b5")),
                                depth(20,
                                        line(-6.28, 20, "d3e2"),
                                        line(7.68, 20, "g6f5"),
                                        line(7.81, 20, "a6b5")))),
                -6.55);

        observe(
                "Kramnik-Leko 25...Qd3",
                KRAMNIK_LEKO,
                50,
                result(
                        List.of(
                                line(-6.64, 20,
                                        "g6d3 g1f2"),
                                line(-3.74, 20,
                                        "g8g7 g1f2"),
                                line(-0.70, 20,
                                        "e2a6 a1a6")),
                        history(
                                depth(5,
                                        line(-0.75, 5, "g8g7"),
                                        line(-0.45, 5, "g6d3"),
                                        line(-0.20, 5, "e2a6")),
                                depth(8,
                                        line(-0.95, 8, "g8g7"),
                                        line(-0.65, 8, "g6d3"),
                                        line(-0.25, 8, "e2a6")),
                                depth(20,
                                        line(-6.64, 20, "g6d3"),
                                        line(-3.74, 20, "g8g7"),
                                        line(-0.70, 20, "e2a6")))),
                -6.96);

        observe(
                "Kramnik-Leko 26.Kf2",
                KRAMNIK_LEKO,
                51,
                result(
                        List.of(
                                line(-6.79, 20,
                                        "g1f2 d3f3"),
                                line(-8.47, 20,
                                        "a1a5 d3e3"),
                                line(-12.66, 20,
                                        "c1d1 d3e3")),
                        history(
                                depth(5,
                                        line(-0.40, 5, "a1a5"),
                                        line(-1.60, 5, "g1f2"),
                                        line(-2.00, 5, "c1d1")),
                                depth(8,
                                        line(-0.60, 8, "a1a5"),
                                        line(-1.90, 8, "g1f2"),
                                        line(-2.20, 8, "c1d1")),
                                depth(20,
                                        line(-6.79, 20, "g1f2"),
                                        line(-8.47, 20, "a1a5"),
                                        line(-12.66, 20, "c1d1")))),
                -6.69);

        observe(
                "Kramnik-Leko 29.bxc3",
                KRAMNIK_LEKO,
                57,
                result(
                        List.of(
                                line(-6.56, 20,
                                        "b2c3 d3c3"),
                                line(-8.42, 20,
                                        "c1f7 e8f7"),
                                line(-9.01, 20,
                                        "c1c4 d3c4")),
                        Map.of()),
                -4.95);
    }

    private void observe(
            String label,
            String pgn,
            int ply,
            DeepAnalysisResult result,
            double resultingEvaluation)
            throws Exception {
        List<String> moves = gameLoader.parsePgnMoveList(pgn);
        Game position = Simulation.createSimulation();

        for (int index = 0; index < ply - 1; index++) {
            Move move =
                    LegalMoveResolver.resolveUci(
                            position,
                            moves.get(index));
            position.apply(move);
        }

        String playedMoveUci = moves.get(ply - 1);
        MoveAnnotation annotation =
                classifier.classify(
                        position,
                        playedMoveUci,
                        result,
                        resultingEvaluation);

        System.out.println(
                "OBSERVE | "
                        + label
                        + " | uci="
                        + playedMoveUci
                        + " | "
                        + describe(annotation));
    }

    private String describe(MoveAnnotation annotation) {
        if (annotation == null) {
            return "annotation=none";
        }

        return "kind="
                + annotation.getKind()
                + ", reason="
                + annotation.getExtraordinaryReason()
                + ", sacrificeType="
                + annotation.getSacrificeType()
                + ", investment="
                + annotation.getMaterialInvestment()
                + ", earlyDepth="
                + annotation.getEarlyDepth()
                + ", earlyRank="
                + annotation.getEarlyRank()
                + ", finalRank="
                + annotation.getFinalRank()
                + ", earlyRegret="
                + annotation.getEarlyRegret()
                + ", earlyStrength="
                + annotation.getEarlyStrength()
                + ", finalStrength="
                + annotation.getFinalStrength();
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
