package demo.chess.analysis.annotation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

public class MaterialInvestmentDetectorTest {

    private final MaterialInvestmentDetector detector =
            new MaterialInvestmentDetector();

    @Test
    public void nezhmetdinovQueenSacrificeHasThreePointNetInvestment()
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

        EngineLine line = line(
                0.0,
                20,
                "h4f6 d4e2 c3e2 e7f6");

        double investment = detector.calculate(root, line, true, 6);

        assertEquals(3.0, investment, 0.001);
    }

    @Test
    public void blackBishopWinningExchangeIsNotMaterialInvestment()
            throws Exception {
        Game root = positionBeforeBlackBishopTakesF1();

        EngineLine line = line(
                0.0,
                20,
                "e2f1 g1f1");

        double investment = detector.calculate(root, line, false, 6);

        assertEquals(0.0, investment, 0.001);
    }

    @Test
    public void blackRookSacrificeAfterBxf7HasTwoPointNetInvestment()
            throws Exception {
        Game root = positionBeforeBlackRookTakesF7();

        EngineLine line = line(
                0.0,
                20,
                "c7f7 h3h8 g8h8 g5f7");

        double investment = detector.calculate(root, line, false, 6);

        assertEquals(2.0, investment, 0.001);
    }

    private Game positionBeforeBlackBishopTakesF1() throws Exception {
        return positionAfter(
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
                "e1g1", "g7f6",
                "h4f6", "d4e2",
                "c3e2", "e7f6",
                "e2c3", "f8e8",
                "c3d5", "e8e6",
                "e3d4", "g8g7",
                "a1d1", "d7d6",
                "d1d3", "c8d7",
                "d3f3", "d7b5",
                "d4c3", "a5d8",
                "d5f6", "b5e2",
                "f6h7", "g7g8",
                "f3h3", "e6e5",
                "f2f4");
    }

    private Game positionBeforeBlackRookTakesF7() throws Exception {
        Game game = positionBeforeBlackBishopTakesF1();

        play(game, "e2f1");
        play(game, "g1f1");
        play(game, "a8c8");
        play(game, "c3d4");
        play(game, "b7b5");
        play(game, "h7g5");
        play(game, "c8c7");
        play(game, "d4f7");

        return game;
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

    private EngineLine line(double evaluation, int depth, String moves) {
        return new EngineLine(evaluation, depth, null, moves);
    }
}
