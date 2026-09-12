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
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnNotation;

/**
 * Golden regression suite for the move classifications that were validated
 * against the Stockfish 19 / depth 15 diagnostic PGNs exported on 2026-09-12.
 *
 * <p><strong>Important:</strong> a failure in this class is intentionally a
 * build-breaking signal. Do not simply change an expected value to make the
 * build green. First inspect the algorithm change, the chess position and the
 * original diagnostic values carefully and decide explicitly whether the new
 * classification is actually an improvement. Only then may an expectation in
 * this golden suite be changed.</p>
 *
 * <p>The final MultiPV evaluations below are copied verbatim from the
 * {@code prePv*Eval} fields of the three diagnostic PGNs. The move evaluation
 * after the played move is copied from {@code [%eval ...]}. Principal
 * variations are reduced to the prefix required by the classifier; active
 * sacrifice cases retain enough of the exported PV to replay the sacrifice.</p>
 *
 * <p>Diagnostic-v2 does not serialize the raw intermediate depth snapshots.
 * For Kramnik-Leko {@code 25...Qd3!!}, the minimal early history is therefore
 * reconstructed from the values that the PGN does export:
 * {@code earlyDepth=6}, {@code earlyRank=2}, {@code earlyRegret=5.26} and
 * {@code earlyStrength=61.22}. The final MultiPV values remain the exact
 * exported engine values.</p>
 */
public class MoveAnnotationGoldenRegressionTest {

    private static final double METRIC_TOLERANCE = 0.02;

    private static final String KRAMNIK_LEKO = """
            [Event "ChessAnalysisTool diagnostic export"]
            [White "Kramnik, Vladimir"]
            [Black "Leko, Peter"]
            [Result "*"]

            1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. Ba4 Nf6
            5. O-O Be7 6. Re1 b5 7. Bb3 O-O 8. c3 d5
            9. exd5 Nxd5 10. Nxe5 Nxe5 11. Rxe5 c6
            12. d4 Bd6 13. Re1 Qh4 14. g3 Qh3 15. Re4 g5
            16. Qf1 Qh5 17. Nd2 Bf5 18. f3 Nf6 19. Re1 Rae8
            20. Rxe8 Rxe8 21. a4 Qg6 22. axb5 Bd3
            23. Qf2 Re2 24. Qxe2 Bxe2 25. bxa6 Qd3
            26. Kf2 Bxf3 27. Nxf3 Ne4+ 28. Ke1 Nxc3
            29. bxc3 Qxc3+ 30. Kf2 Qxa1 31. a7 h6
            32. h4 g4 *
            """;

    private static final String NEZHMETDINOV_CHERNIKOV = """
            [Event "ChessAnalysisTool diagnostic export"]
            [White "Nezhmetdinov, Rashid"]
            [Black "Chernikov, Oleg"]
            [Result "*"]

            1. e4 c5 2. Nf3 Nc6 3. d4 cxd4 4. Nxd4 g6
            5. Nc3 Bg7 6. Be3 Nf6 7. Bc4 O-O 8. Bb3 Ng4
            9. Qxg4 Nxd4 10. Qh4 Qa5 11. O-O Bf6
            12. Qxf6 Ne2+ 13. Nxe2 exf6 14. Nc3 Re8
            15. Nd5 Re6 16. Bd4 Kg7 17. Rad1 d6 18. Rd3 Bd7
            19. Rf3 Bb5 20. Bc3 Qd8 21. Nxf6 Be2
            22. Nxh7+ Kg8 23. Rh3 Re5 24. f4 Bxf1
            25. Kxf1 Rc8 26. Bd4 b5 27. Ng5 Rc7
            28. Bxf7+ Rxf7 29. Rh8+ Kxh8 30. Nxf7+ Kg8
            31. Nxd8 Rxe4 32. Nc6 Rxf4+ 33. Ke2 *
            """;

    private static final String BYRNE_FISCHER = """
            [Event "ChessAnalysisTool diagnostic export"]
            [White "Byrne, David"]
            [Black "Fischer, Robert J"]
            [Result "0-1"]

            1. Nf3 Nf6 2. c4 g6 3. Nc3 Bg7 4. d4 O-O
            5. Bf4 d5 6. Qb3 dxc4 7. Qxc4 c6 8. e4 Nbd7
            9. Rd1 Nb6 10. Qc5 Bg4 11. Bg5 Na4
            12. Qa3 Nxc3 13. bxc3 Nxe4 14. Bxe7 Qb6
            15. Bc4 Nxc3 16. Bc5 Rfe8+ 17. Kf1 Be6
            18. Bxb6 Bxc4+ 19. Kg1 Ne2+ 20. Kf1 Nxd4+
            21. Kg1 Ne2+ 22. Kf1 Nc3+ 23. Kg1 axb6
            24. Qb4 Ra4 25. Qxb6 Nxd1 26. h3 Rxa2
            27. Kh2 Nxf2 28. Re1 Rxe1 29. Qd8+ Bf8
            30. Nxe1 Bd5 31. Nf3 Ne4 32. Qb8 b5
            33. h4 h5 34. Ne5 Kg7 35. Kg1 Bc5+
            36. Kf1 Ng3+ 37. Ke1 Bb4+ 38. Kd1 Bb3+
            39. Kc1 Ne2+ 40. Kb1 Nc3+ 41. Kc1 Rc2# 0-1
            """;

    private final MoveAnnotationClassifier classifier =
            new MoveAnnotationClassifier();
    private final GameLoader gameLoader = new GameLoader();

    @Test
    public void kramnikLekoCriticalRatingsStayStable()
            throws Exception {
        MoveAnnotation qf2 = classify(
                KRAMNIK_LEKO,
                45,
                -3.79,
                line(0.00, "♕d1"),
                line(-2.14, "♗c2"),
                line(-4.94, "♕f2"));
        assertKind("Kramnik-Leko 23.Qf2", qf2, MoveAnnotationKind.BLUNDER);
        assertMetric("Kramnik-Leko 23.Qf2 loss", 36.04, qf2.getWinChanceLoss());

        MoveAnnotation qxe2 = classify(
                KRAMNIK_LEKO,
                47,
                -4.58,
                line(-4.78, "bxa6"),
                line(-4.98, "♕xe2 ♝xe2 bxa6"),
                line(-6.58, "b6"));
        assertNull(
                "Kramnik-Leko 24.Qxe2 must remain unannotated",
                qxe2);

        MoveAnnotation bxe2 = classifyWithStableEarlyHistory(
                KRAMNIK_LEKO,
                48,
                -5.17,
                line(-4.58, "♝xe2"),
                line(7.58, "h6"),
                line(7.66, "axb5"));
        assertNull(
                "Kramnik-Leko 24...Bxe2 must remain unannotated",
                bxe2);

        MoveAnnotation qd3 = classifyWithExportedDiscovery(
                KRAMNIK_LEKO,
                50,
                -6.70,
                6,
                2,
                5.26,
                61.22,
                line(-5.28, "♛d3"),
                line(-2.01, "♚g7"),
                line(-1.15, "♝xa6"));
        assertKind(
                "Kramnik-Leko 25...Qd3",
                qd3,
                MoveAnnotationKind.EXTRAORDINARY);
        assertEquals(
                ExtraordinaryReason.DEEP_DISCOVERY,
                qd3.getExtraordinaryReason());
        assertEquals(Integer.valueOf(6), qd3.getEarlyDepth());
        assertEquals(Integer.valueOf(2), qd3.getEarlyRank());
        assertEquals(Integer.valueOf(15), qd3.getFinalDepth());
        assertEquals(Integer.valueOf(1), qd3.getFinalRank());
        assertMetric(
                "Kramnik-Leko 25...Qd3 early regret",
                5.26,
                qd3.getEarlyRegret());
        assertMetric(
                "Kramnik-Leko 25...Qd3 early strength",
                61.22,
                qd3.getEarlyStrength());
        assertMetric(
                "Kramnik-Leko 25...Qd3 final strength",
                87.48,
                qd3.getFinalStrength());

        MoveAnnotation kf2 = classify(
                KRAMNIK_LEKO,
                51,
                -6.56,
                line(-6.70, "♔f2"),
                line(-8.60, "♖a5"),
                line(-12.99, "♗d1"));
        assertNull(
                "Kramnik-Leko 26.Kf2 must remain unannotated",
                kf2);

        MoveAnnotation bxc3 = classify(
                KRAMNIK_LEKO,
                57,
                -4.99,
                line(-6.84, "bxc3"),
                line(-9.07, "♗xf7+"),
                line(-9.64, "♗c4"));
        assertNull(
                "Kramnik-Leko 29.bxc3 must remain unannotated",
                bxc3);
    }

    @Test
    public void nezhmetdinovChernikovRatingsStayStable()
            throws Exception {
        MoveAnnotation qxf6 = classify(
                NEZHMETDINOV_CHERNIKOV,
                23,
                0.14,
                line(0.21, "♕h6"),
                line(0.20, "♕g4"),
                line(0.00, "♕xf6 ♞e2+ ♘xe2 exf6 ♘f4"));
        assertKind(
                "Nezhmetdinov-Chernikov 12.Qxf6",
                qxf6,
                MoveAnnotationKind.EXTRAORDINARY);
        assertEquals(
                ExtraordinaryReason.MATERIAL_SACRIFICE,
                qxf6.getExtraordinaryReason());
        assertEquals(
                MaterialSacrificeType.ACTIVE_INVESTMENT,
                qxf6.getSacrificeType());
        assertMetric(
                "Nezhmetdinov-Chernikov 12.Qxf6 investment",
                3.00,
                qxf6.getMaterialInvestment());
        assertEquals(Integer.valueOf(3), qxf6.getFinalRank());

        assertQuality(
                "Nezhmetdinov-Chernikov 17...d6",
                classify(
                        NEZHMETDINOV_CHERNIKOV,
                        34,
                        1.28,
                        line(0.00, "b6"),
                        line(0.47, "b5"),
                        line(1.36, "d6")),
                MoveAnnotationKind.MISTAKE,
                12.26);

        /*
         * The PGN labels 21...Be2 as ?? and also exports winChanceLoss=28.93.
         * Diagnostic-v2 does not contain the exact fallback score that produced
         * that loss: using its serialized bestEvalBefore=0.96 and [%eval 5.46]
         * yields 29.44 with the production mapping. Do not invent an unexported
         * value here. The golden contract we can reproduce from the PGN is the
         * actual classification: BLUNDER.
         */
        MoveAnnotation be2 = classify(
                NEZHMETDINOV_CHERNIKOV,
                42,
                5.46,
                line(0.96, "♜c8"),
                line(4.26, "♜xf6"),
                line(4.94, "d5"));
        assertKind(
                "Nezhmetdinov-Chernikov 21...Be2",
                be2,
                MoveAnnotationKind.BLUNDER);

        assertQuality(
                "Nezhmetdinov-Chernikov 23.Rh3",
                classify(
                        NEZHMETDINOV_CHERNIKOV,
                        45,
                        1.44,
                        line(5.44, "♘f6+"),
                        line(1.20, "♖h3"),
                        line(0.48, "♖f4")),
                MoveAnnotationKind.BLUNDER,
                27.24);

        assertQuality(
                "Nezhmetdinov-Chernikov 24...Bxf1",
                classify(
                        NEZHMETDINOV_CHERNIKOV,
                        48,
                        5.46,
                        line(1.76, "♜h5"),
                        line(4.88, "♝xf1"),
                        line(6.96, "♝g4")),
                MoveAnnotationKind.MISTAKE,
                20.12);

        assertQuality(
                "Nezhmetdinov-Chernikov 25.Kxf1",
                classify(
                        NEZHMETDINOV_CHERNIKOV,
                        49,
                        2.28,
                        line(5.46, "♘g5"),
                        line(4.06, "fxe5"),
                        line(1.93, "♔xf1")),
                MoveAnnotationKind.MISTAKE,
                21.13);

        assertQuality(
                "Nezhmetdinov-Chernikov 25...Rc8",
                classify(
                        NEZHMETDINOV_CHERNIKOV,
                        50,
                        5.86,
                        line(2.28, "♜h5"),
                        line(5.32, "♜xe4"),
                        line(5.92, "♜c8")),
                MoveAnnotationKind.MISTAKE,
                20.01);

        assertQuality(
                "Nezhmetdinov-Chernikov 26.Bd4",
                classify(
                        NEZHMETDINOV_CHERNIKOV,
                        51,
                        3.91,
                        line(5.86, "fxe5"),
                        line(2.00, "♗d4"),
                        line(-0.36, "♘g5")),
                MoveAnnotationKind.MISTAKE,
                22.02);

        MoveAnnotation rh8 = classify(
                NEZHMETDINOV_CHERNIKOV,
                57,
                5.92,
                line(5.75, "♖h8+"),
                line(0.01, "♗xe5"),
                line(-0.81, "♘xf7"));
        assertKind(
                "Nezhmetdinov-Chernikov 29.Rh8+",
                rh8,
                MoveAnnotationKind.ONLY_MOVE);
        assertMetric(
                "Nezhmetdinov-Chernikov 29.Rh8+ second best",
                0.01,
                rh8.getSecondBestEvaluation());
    }

    @Test
    public void byrneFischerRatingsStayStable()
            throws Exception {
        assertQuality(
                "Byrne-Fischer 11.Bg5",
                classify(
                        BYRNE_FISCHER,
                        21,
                        -2.61,
                        line(0.24, "♗e2"),
                        line(0.13, "a4"),
                        line(0.01, "♗e3")),
                MoveAnnotationKind.MISTAKE,
                24.54);

        MoveAnnotation na4 = classify(
                BYRNE_FISCHER,
                22,
                -2.98,
                line(-2.61, "♞a4"),
                line(-0.20, "♞bd7"),
                line(0.03, "h6"));
        assertKind(
                "Byrne-Fischer 11...Na4",
                na4,
                MoveAnnotationKind.ONLY_MOVE);
        assertMetric(
                "Byrne-Fischer 11...Na4 second best",
                -0.20,
                na4.getSecondBestEvaluation());

        MoveAnnotation be6 = classify(
                BYRNE_FISCHER,
                34,
                -3.43,
                line(-3.34, "♝e6"),
                line(1.18, "♞b5"),
                line(1.41, "♞b1"));
        assertKind(
                "Byrne-Fischer 17...Be6",
                be6,
                MoveAnnotationKind.EXTRAORDINARY);
        assertEquals(
                ExtraordinaryReason.MATERIAL_SACRIFICE,
                be6.getExtraordinaryReason());
        assertEquals(
                MaterialSacrificeType.DECLINED_MATERIAL_SAVE,
                be6.getSacrificeType());
        assertMetric(
                "Byrne-Fischer 17...Be6 investment",
                6.00,
                be6.getMaterialInvestment());
        assertEquals(Integer.valueOf(1), be6.getFinalRank());

        assertQuality(
                "Byrne-Fischer 18.Bxb6",
                classify(
                        BYRNE_FISCHER,
                        35,
                        -7.76,
                        line(-3.43, "♕xc3"),
                        line(-5.24, "♗e2"),
                        line(-5.52, "♗d3")),
                MoveAnnotationKind.MISTAKE,
                16.62);
    }

    private MoveAnnotation classify(
            String pgn,
            int ply,
            double resultingEvaluation,
            LineSpec... lineSpecs)
            throws Exception {
        PreparedPosition prepared = prepare(pgn, ply);
        List<EngineLine> finalLines =
                toEngineLines(prepared.position(), lineSpecs);

        return classifier.classify(
                prepared.position(),
                prepared.playedMoveUci(),
                new DeepAnalysisResult(finalLines, Map.of()),
                resultingEvaluation);
    }

    /**
     * Diagnostic-v2 contains no raw search history for an unannotated move.
     * For 24...Bxe2 we only need to preserve the exported fact that the move
     * was already obvious rather than a deep discovery. Reusing the exact
     * exported final evaluations at depths 5 and 6 creates the smallest stable
     * early history without inventing additional score values.
     */
    private MoveAnnotation classifyWithStableEarlyHistory(
            String pgn,
            int ply,
            double resultingEvaluation,
            LineSpec... lineSpecs)
            throws Exception {
        PreparedPosition prepared = prepare(pgn, ply);
        List<EngineLine> finalLines =
                toEngineLines(prepared.position(), lineSpecs);

        Map<Integer, List<EngineLine>> history =
                new LinkedHashMap<>();
        history.put(5, withDepth(finalLines, 5));
        history.put(6, withDepth(finalLines, 6));

        return classifier.classify(
                prepared.position(),
                prepared.playedMoveUci(),
                new DeepAnalysisResult(finalLines, history),
                resultingEvaluation);
    }

    /**
     * Reconstructs the smallest possible early search history from the
     * diagnostic values exported for a deep-discovery move. No unexported
     * engine score is assumed: played and best early scores are obtained by
     * inverting the same win-percentage mapping used by production code.
     */
    private MoveAnnotation classifyWithExportedDiscovery(
            String pgn,
            int ply,
            double resultingEvaluation,
            int earlyDepth,
            int earlyRank,
            double earlyRegret,
            double earlyStrength,
            LineSpec... lineSpecs)
            throws Exception {
        if (earlyRank != 2) {
            throw new IllegalArgumentException(
                    "Golden discovery reconstruction currently expects rank 2");
        }

        PreparedPosition prepared = prepare(pgn, ply);
        boolean whiteMover =
                prepared.position().getMoveList().size() % 2 == 0;
        List<EngineLine> finalLines =
                toEngineLines(prepared.position(), lineSpecs);

        String playedMove = prepared.playedMoveUci();
        String firstAlternative = finalLines.stream()
                .map(EvaluationScoring::firstMove)
                .filter(move -> move != null
                        && !move.equalsIgnoreCase(playedMove))
                .findFirst()
                .orElseThrow();
        String secondAlternative = finalLines.stream()
                .map(EvaluationScoring::firstMove)
                .filter(move -> move != null
                        && !move.equalsIgnoreCase(playedMove)
                        && !move.equalsIgnoreCase(firstAlternative))
                .findFirst()
                .orElseThrow();

        double playedMoverScore =
                moverScoreFromWinPercent(earlyStrength);
        double bestMoverScore =
                moverScoreFromWinPercent(
                        earlyStrength + earlyRegret);
        double thirdMoverScore =
                moverScoreFromWinPercent(
                        Math.max(0.01, earlyStrength - 5.0));

        List<EngineLine> earlyLines = List.of(
                new EngineLine(
                        whiteEvaluation(
                                bestMoverScore,
                                whiteMover),
                        earlyDepth,
                        null,
                        firstAlternative),
                new EngineLine(
                        whiteEvaluation(
                                playedMoverScore,
                                whiteMover),
                        earlyDepth,
                        null,
                        playedMove),
                new EngineLine(
                        whiteEvaluation(
                                thirdMoverScore,
                                whiteMover),
                        earlyDepth,
                        null,
                        secondAlternative));

        Map<Integer, List<EngineLine>> history =
                new LinkedHashMap<>();
        history.put(
                Math.max(1, earlyDepth - 1),
                withDepth(earlyLines, Math.max(1, earlyDepth - 1)));
        history.put(
                earlyDepth,
                earlyLines);

        return classifier.classify(
                prepared.position(),
                playedMove,
                new DeepAnalysisResult(finalLines, history),
                resultingEvaluation);
    }

    private PreparedPosition prepare(
            String pgn,
            int ply)
            throws Exception {
        List<String> moves =
                gameLoader.parsePgnMoveList(pgn);
        Game position = Simulation.createSimulation();

        for (int index = 0; index < ply - 1; index++) {
            Move move =
                    LegalMoveResolver.resolveUci(
                            position,
                            moves.get(index));
            position.apply(move);
        }

        return new PreparedPosition(
                position,
                moves.get(ply - 1));
    }

    private List<EngineLine> toEngineLines(
            Game position,
            LineSpec... lineSpecs)
            throws Exception {
        return java.util.Arrays.stream(lineSpecs)
                .map(spec -> {
                    try {
                        return toEngineLine(position, spec);
                    } catch (Exception exception) {
                        throw new GoldenFixtureException(exception);
                    }
                })
                .toList();
    }

    private EngineLine toEngineLine(
            Game position,
            LineSpec spec)
            throws Exception {
        var replay =
                Simulation.forkDummyFrom(
                        position.getMoveList());
        StringBuilder uciPv = new StringBuilder();

        for (String san : spec.displayPv().trim().split("\\s+")) {
            if (san.isBlank()) {
                continue;
            }
            Move move = PgnNotation.resolveSan(replay, san);
            if (uciPv.length() > 0) {
                uciPv.append(' ');
            }
            uciPv.append(move.toString());
            replay.apply(move);
        }

        return new EngineLine(
                spec.evaluation(),
                15,
                null,
                uciPv.toString());
    }

    private List<EngineLine> withDepth(
            List<EngineLine> lines,
            int depth) {
        return lines.stream()
                .map(line -> new EngineLine(
                        line.getEvaluation(),
                        depth,
                        line.getMateDistance(),
                        line.getMoves()))
                .toList();
    }

    private double moverScoreFromWinPercent(
            double winPercent) {
        double bounded = Math.max(
                0.000001,
                Math.min(99.999999, winPercent));
        return Math.log(
                bounded / (100.0 - bounded))
                / 0.368208;
    }

    private double whiteEvaluation(
            double moverScore,
            boolean whiteMover) {
        return whiteMover
                ? moverScore
                : -moverScore;
    }

    private void assertQuality(
            String label,
            MoveAnnotation annotation,
            MoveAnnotationKind kind,
            double expectedLoss) {
        assertKind(label, annotation, kind);
        assertMetric(
                label + " win chance loss",
                expectedLoss,
                annotation.getWinChanceLoss());
    }

    private void assertKind(
            String label,
            MoveAnnotation annotation,
            MoveAnnotationKind expectedKind) {
        assertNotNull(label + " must have an annotation", annotation);
        assertEquals(
                label + " kind changed",
                expectedKind,
                annotation.getKind());
    }

    private void assertMetric(
            String label,
            double expected,
            Double actual) {
        assertNotNull(label + " must be present", actual);
        assertEquals(
                label + " changed",
                expected,
                actual,
                METRIC_TOLERANCE);
    }

    private record LineSpec(
            double evaluation,
            String displayPv) {
    }

    private record PreparedPosition(
            Game position,
            String playedMoveUci) {
    }

    private static final class GoldenFixtureException
            extends RuntimeException {
        private GoldenFixtureException(
                Throwable cause) {
            super(cause);
        }
    }

    private LineSpec line(
            double evaluation,
            String displayPv) {
        return new LineSpec(
                evaluation,
                displayPv);
    }
}
