package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

public class MaterialOfferDetectorTest {

    private final MaterialOfferDetector detector = new MaterialOfferDetector();

    @Test
    public void byrneFischerBe6OffersQueenForSixNetPoints()
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

        double offer = detector.calculate(root, "g4e6", false);

        // 17...Be6 deliberately leaves the queen on b6 to Bxb6.
        // After Bxb6 Black can recover the bishop with axb6:
        // queen 9 - bishop 3 = six net points deliberately offered.
        assertEquals(6.0, offer, 0.001);
    }

    @Test
    public void byrneFischerNc3DoesNotOfferMaterialImmediately()
            throws Exception {
        Game root = positionAfter(
                "g1f3", "g8f6",
                "c2c4", "g7g6");

        double offer = detector.calculate(root, "b1c3", true);

        assertEquals(0.0, offer, 0.001);
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
