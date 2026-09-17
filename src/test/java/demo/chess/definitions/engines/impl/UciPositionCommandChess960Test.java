package demo.chess.definitions.engines.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.game.impl.Simulation;

public class UciPositionCommandChess960Test {

    @Test
    public void standardGameUsesStartpos() {
        assertEquals("position startpos", UciPositionCommand.build(Simulation.createSimulation()));
    }

    @Test
    public void chess960GameUsesInitialFen() {
        Simulation game = Simulation.createSimulation(ChessStartingPosition.of(0));
        assertEquals(
                "position fen bbqnnrkr/pppppppp/8/8/8/8/PPPPPPPP/BBQNNRKR w HFhf - 0 1",
                UciPositionCommand.build(game));
    }
}
