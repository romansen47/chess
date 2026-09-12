package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

public class MaterialOfferDetectorTest {

    private final MaterialOfferDetector detector = new MaterialOfferDetector();

    @Test
    public void byrneFischerBe6CreatesNewQueenOfferForSixNetPoints()
            throws Exception {
        Game root = positionAfter(
                "g1f3", "g8f6",
                "c2c4", "g7g6",
                "b1c3", "f8g7",
                "d2d4", "e8g8",
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

        MaterialSacrificeEvidence evidence =
                detector.find(root, "g4e6", false);

        assertNotNull(evidence);
        assertEquals(
                MaterialSacrificeType.NEW_MATERIAL_OFFER,
                evidence.getType());
        assertEquals(6.0, evidence.getValue(), 0.001);
    }

    @Test
    public void deliberatelyIgnoringAttackedQueenIsDeclinedSave()
            throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "f1c4", "b8c6",
                "d1h5", "g8f6");

        MaterialSacrificeEvidence evidence =
                detector.find(root, "d2d3", true);

        assertNotNull(evidence);
        assertEquals(
                MaterialSacrificeType.DECLINED_MATERIAL_SAVE,
                evidence.getType());
        assertEquals(9.0, evidence.getValue(), 0.001);
    }

    @Test
    public void forcedCheckReplyDoesNotGetCreditForAlreadyHangingQueen()
            throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "d1h5", "b8c6",
                "g1f3", "g8f6",
                "d2d3", "f8b4");

        MaterialSacrificeEvidence evidence =
                detector.find(root, "c2c3", true);

        /*
         * The queen on h5 was already attacked by Nf6. Because White is in
         * check from Bb4+, the queen has no legal move that can save itself
         * while also answering the check. Blocking with c3 therefore must not
         * receive sacrifice credit for an unavoidable queen threat.
         */
        assertNull(evidence);
    }

    @Test
    public void checkmateDoesNotInventPseudoLegalMaterialOffer()
            throws Exception {
        Game root = positionAfter(
                "e2e4", "e7e5",
                "f1c4", "b8c6",
                "d1h5", "g8f6");

        MaterialSacrificeEvidence evidence =
                detector.find(root, "h5f7", true);

        assertNull(evidence);
    }

    @Test
    public void ordinaryDevelopmentDoesNotOfferMaterialImmediately()
            throws Exception {
        Game root = positionAfter(
                "g1f3", "g8f6",
                "c2c4", "g7g6");

        MaterialSacrificeEvidence evidence =
                detector.find(root, "b1c3", true);

        assertNull(evidence);
    }

    private Game positionAfter(String... moves) throws Exception {
        Game game = Simulation.createSimulation();
        for (String uci : moves) {
            play(game, uci);
        }
        return game;
    }

    private void play(Game game, String uci) throws Exception {
        Move move = LegalMoveResolver.resolveUci(game, uci);
        game.apply(move);
    }
}
