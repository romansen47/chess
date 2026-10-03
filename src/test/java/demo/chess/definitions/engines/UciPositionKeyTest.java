package demo.chess.definitions.engines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.game.impl.Simulation;

public class UciPositionKeyTest {

    @Test
    public void startingPositionIsPartOfPositionIdentity() throws Exception {
        UciPositionKey standard = UciPositionKey.from(
                Simulation.createSimulation(ChessStartingPosition.of(518)));
        UciPositionKey chess960 = UciPositionKey.from(
                Simulation.createSimulation(ChessStartingPosition.of(0)));

        assertNotEquals(standard, chess960);
        assertEquals(518, standard.startingPositionId());
        assertEquals(0, chess960.startingPositionId());
    }
}
