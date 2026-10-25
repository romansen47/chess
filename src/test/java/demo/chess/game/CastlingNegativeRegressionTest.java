package demo.chess.game;

import static org.junit.Assert.assertThrows;

import org.junit.Test;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.game.impl.Simulation;

public class CastlingNegativeRegressionTest {

    @Test
    public void cannotCastleOntoAnAttackedKingDestination() throws Exception {
        Simulation game = Simulation.createSimulation();
        apply(game,
                "e2e4", "e7e5",
                "g1f3", "f8c5",
                "f1e2", "d7d6",
                "f2f3", "a7a6");

        assertThrows(
                NoMoveFoundException.class,
                () -> LegalMoveResolver.resolveUci(game, "e1h1"));
    }

    private void apply(Game game, String... moves) throws Exception {
        for (String uci : moves) {
            game.apply(LegalMoveResolver.resolveUci(game, uci));
        }
    }
}
