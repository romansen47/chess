package demo.chess.game;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.impl.Simulation;

/**
 * Reference perft regression for the classical starting position.
 *
 * <p>Perft exercises the complete legal-move pipeline recursively and therefore
 * protects interactions between piece movement, king safety, captures and move
 * simulation better than isolated happy-path examples.</p>
 */
public class ClassicalPerftTest {

    /** Verifies the standard reference perft node counts through depth three. */
    @Test
    public void matchesReferenceNodeCountsThroughDepthThree() throws Exception {
        Simulation game = Simulation.createSimulation();

        assertEquals(20L, perft(game, 1));
        assertEquals(400L, perft(game, 2));
        assertEquals(8902L, perft(game, 3));
    }

    private long perft(Game game, int depth)
            throws NoMoveFoundException, IOException {
        if (depth == 0) return 1L;

        long nodes = 0L;
        for (Move move : game.getPlayer().getValidMoves(game)) {
            Simulation child = Simulation.forkSimulationFrom(game.getMoveList());
            Move mappedMove = MoveSimulationMapper.map(child, move);
            if (mappedMove == null) {
                throw new IllegalStateException("Could not map legal move into perft child: " + move);
            }
            child.apply(mappedMove);
            nodes += perft(child, depth - 1);
        }
        return nodes;
    }
}
