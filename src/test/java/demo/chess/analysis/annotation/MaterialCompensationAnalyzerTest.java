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

    private Game positionAfter(String... moves) throws Exception {
        Game game = Simulation.createSimulation();
        for (String uci : moves) {
            Move move = LegalMoveResolver.resolveUci(game, uci);
            game.apply(move);
        }
        return game;
    }
}
