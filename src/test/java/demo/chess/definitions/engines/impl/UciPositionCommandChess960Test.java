package demo.chess.definitions.engines.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.game.impl.Simulation;

public class UciPositionCommandChess960Test {

    @Test
    public void position518UsesInitialFenLikeEveryOtherPosition() {
        Simulation game = Simulation.createSimulation(ChessStartingPosition.STANDARD);
        assertEquals(
                "position fen rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w HAha - 0 1",
                UciPositionCommand.build(game));
    }

    @Test
    public void position0UsesInitialFen() {
        Simulation game = Simulation.createSimulation(ChessStartingPosition.of(0));
        assertEquals(
                "position fen bbqnnrkr/pppppppp/8/8/8/8/PPPPPPPP/BBQNNRKR w HFhf - 0 1",
                UciPositionCommand.build(game));
    }
}
