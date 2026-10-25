package demo.chess.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import demo.chess.game.impl.Simulation;

public class PositionIdentityTest {

    @Test
    public void reversibleKnightCycleReturnsToTheSamePositionIdentity() throws Exception {
        Simulation game = Simulation.createSimulation();
        PositionIdentity initial = PositionIdentity.from(game);

        apply(game, "g1f3", "g8f6", "f3g1", "f6g8");

        assertEquals(initial, PositionIdentity.from(game));
    }

    @Test
    public void equalPiecePlacementWithDifferentCastlingRightsIsNotTheSamePosition() throws Exception {
        Simulation rookMoved = Simulation.createSimulation();
        apply(rookMoved, "h2h3", "a7a6", "h1h2", "a6a5", "h2h1", "a5a4");

        Simulation rookUntouched = Simulation.createSimulation();
        apply(rookUntouched, "h2h3", "a7a6", "g1f3", "a6a5", "f3g1", "a5a4");

        assertNotEquals(PositionIdentity.from(rookMoved), PositionIdentity.from(rookUntouched));
    }

    private void apply(Game game, String... moves) throws Exception {
        for (String uci : moves) {
            game.apply(LegalMoveResolver.resolveUci(game, uci));
        }
    }
}
