package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

public class MaterialCompensationAnalyzerTest {

    private final MaterialOfferDetector offerDetector =
            new MaterialOfferDetector();
    private final MaterialCompensationAnalyzer analyzer =
            new MaterialCompensationAnalyzer();

    @Test
    public void nxe6DetectsShortTermCompensationForDeclinedQueenSave()
            throws Exception {
        Game root = positionAfter(
                "d2d4", "d7d5",
                "c2c4", "e7e6",
                "b1c3", "f8b4",
                "c1d2", "g8f6",
                "d1a4", "b8c6",
                "e2e3", "d5c4",
                "f1c4", "e8h8",
                "g1f3", "a7a6",
                "a4c2", "b4d6",
                "c3e4", "e6e5",
                "d4e5", "c6e5",
                "e4d6", "c7d6",
                "c4e2", "c8e6",
                "f3d4", "a8c8");

        MaterialSacrificeEvidence sacrifice =
                offerDetector.find(root, "d4e6", true);

        assertNotNull(sacrifice);
        assertEquals(
                MaterialSacrificeType.DECLINED_MATERIAL_SAVE,
                sacrifice.getType());
        assertEquals(6.0, sacrifice.getValue(), 0.001);
        assertEquals("c8c2", sacrifice.getAcceptanceMoveUci());

        MaterialCompensationAnalyzer.Evidence compensation =
                analyzer.find(root, "d4e6", sacrifice, true);

        assertNotNull(compensation);
        assertEquals(2, compensation.plies());
        var acceptedPv = new demo.chess.definitions.engines.EngineLine(
                0, 20, null, "d4e6 c8c2 e6d8 f8d8");
        var trajectory = new MaterialTrajectoryAnalyzer().analyze(root, acceptedPv, true);
        assertNotNull(trajectory);
        assertEquals(6.0, trajectory.maximumDeficit(), 0.001);
        assertEquals(0.0, trajectory.persistentDeficit(), 0.001);
        assertEquals(Integer.valueOf(3), trajectory.recoveryPly(2));
        assertNull(new MaterialSacrificeDetector().find(root, acceptedPv, "d4e6", true));
        // Even when the principal variation declines the offer (or is only
        // a root prefix), the bounded proof rules out sacrifice credit.
        assertNull(new MaterialSacrificeDetector().find(root,
                new demo.chess.definitions.engines.EngineLine(0, 20, null, "d4e6"),
                "d4e6", true));
    }

    @Test
    public void byrneFischerBe6RemainsUncompensatedInShortDiagnosticWindow()
            throws Exception {
        Game root = positionAfter(
                "g1f3", "g8f6",
                "c2c4", "g7g6",
                "b1c3", "f8g7",
                "d2d4", "e8h8",
                "c1f4", "d7d5",
                "d1b3", "d5c4",
                "b3c4", "c7c6",
                "e2e4", "b8d7",
                "a1d1", "d7b6",
                "c4c5", "c8g4",
                "f4g5", "b6a4",
                "c5a3", "a4c3",
                "b2c3", "f6e4",
                "g5e7", "d8b6",
                "f1c4", "e4c3",
                "e7c5", "f8e8",
                "e1f1");

        MaterialSacrificeEvidence sacrifice =
                offerDetector.find(root, "g4e6", false);

        assertNotNull(sacrifice);
        assertEquals(
                MaterialSacrificeType.DECLINED_MATERIAL_SAVE,
                sacrifice.getType());
        assertEquals(6.0, sacrifice.getValue(), 0.001);

        assertNull(
                analyzer.find(root, "g4e6", sacrifice, false));
    }

    @Test
    public void qd5CounterattackCompensatesOfferedKnight()
            throws Exception {
        Game root = positionAfter(
                "c2c4", "c7c6",
                "g1f3", "g8f6",
                "d2d4", "d7d5",
                "d1c2", "g7g6",
                "b1c3", "d5c4",
                "e2e4", "b7b5",
                "a2a4", "b5b4",
                "c3d1", "f8g7",
                "f1c4", "e8h8",
                "d1e3", "e7e6",
                "e4e5", "f6d5",
                "h2h4", "b8d7",
                "h4h5", "d7b6",
                "c4d3", "b4b3",
                "c2b1", "d5f4",
                "e3g4", "b6d5",
                "g2g3", "f4d3",
                "b1d3", "d5b4",
                "d3b3");

        MaterialSacrificeEvidence sacrifice =
                offerDetector.find(root, "d8d5", false);

        assertNotNull(sacrifice);
        assertEquals(
                MaterialSacrificeType.DECLINED_MATERIAL_SAVE,
                sacrifice.getType());
        assertEquals(3.0, sacrifice.getValue(), 0.001);
        assertEquals("b3b4", sacrifice.getAcceptanceMoveUci());

        MaterialCompensationAnalyzer.Evidence compensation =
                analyzer.find(root, "d8d5", sacrifice, false);

        assertNotNull(compensation);

        var playedLine = new demo.chess.definitions.engines.EngineLine(
                0.45,
                20,
                null,
                "d8d5 g4h6 g8h8 b3d5 e6d5");
        assertNull(
                new MaterialSacrificeDetector().find(
                        root,
                        playedLine,
                        "d8d5",
                        false));
    }

    private Game positionAfter(String... moves) throws Exception {
        Game game = Simulation.createSimulation();
        for (String uci : moves) {
            Move move = LegalMoveResolver.resolveUci(game, uci);
            game.apply(move);
        }
        return game;
    }
}
