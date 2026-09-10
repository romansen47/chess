package demo.chess.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import demo.chess.definitions.states.State;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;

/**
 * Regression tests for side-effect-free terminal position detection.
 */
public class TerminalPositionEvaluatorTest {

    private final GameLoader gameLoader = new GameLoader();

    @Test
    public void ongoingPositionHasNoTerminalState() {
        Simulation game = Simulation.createSimulation();

        assertNull(TerminalPositionEvaluator.determineState(game));
        assertNull(game.getState());
    }

    @Test
    public void detectsMateInSimulationWithoutMutatingIt() throws Exception {
        Simulation game = Simulation.createSimulation();
        gameLoader.loadGame("src/test/resources/testfiles/mate1.demo", game);

        assertNull(game.getState());
        assertEquals(State.BLACK_MATED, TerminalPositionEvaluator.determineState(game));
        assertNull(game.getState());
    }

    @Test
    public void detectsStalemateInSimulationWithoutMutatingIt() throws Exception {
        Simulation game = Simulation.createSimulation();
        gameLoader.loadGame("src/test/resources/testfiles/stalemate.demo", game);

        assertNull(game.getState());
        assertEquals(State.STALEMATE, TerminalPositionEvaluator.determineState(game));
        assertNull(game.getState());
    }

    @Test
    public void preservesExplicitTerminalState() {
        Simulation game = Simulation.createSimulation();
        game.setState(State.DRAW_BY_THREEFOLD_REPETITION);

        assertEquals(
                State.DRAW_BY_THREEFOLD_REPETITION,
                TerminalPositionEvaluator.determineState(game));
    }
}
