package demo.chess.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;

/**
 * Regression tests for canonical legal move resolution.
 */
public class LegalMoveResolverTest {

    private final GameLoader gameLoader = new GameLoader();

    @Test
    public void resolvesNormalMoveByUciAndCoordinates() throws Exception {
        Simulation game = Simulation.createSimulation();

        Move uciMove = LegalMoveResolver.resolveUci(game, "E2E4");
        Move coordinateMove = LegalMoveResolver.resolveCoordinates(game, "E2", "E4", null);

        assertEquals("e2e4", uciMove.toString());
        assertEquals("e2e4", coordinateMove.toString());
    }

    @Test
    public void resolvesRequestedPromotionPiece() throws Exception {
        Simulation game = Simulation.createSimulation();
        gameLoader.loadGame(
                List.of("h2h4", "g7g5", "h4g5", "g8h6", "g5h6", "f8g7", "h6g7", "c7c6"),
                game);

        Move byUci = LegalMoveResolver.resolveUci(game, "G7H8R");
        Move byCoordinates = LegalMoveResolver.resolveCoordinates(game, "g7", "h8", "rook");

        assertEquals("g7h8r", byUci.toString());
        assertEquals("g7h8r", byCoordinates.toString());
        assertTrue(byCoordinates instanceof Promotion);
        assertEquals(
                PieceType.ROOK,
                ((Promotion) byCoordinates).getPromotedPiece().getType());
    }

    @Test
    public void rejectsIllegalAndUnsupportedPromotionMoves() throws Exception {
        Simulation game = Simulation.createSimulation();

        assertThrows(
                NoMoveFoundException.class,
                () -> LegalMoveResolver.resolveUci(game, "e2e5"));
        assertThrows(
                NoMoveFoundException.class,
                () -> LegalMoveResolver.resolveCoordinates(game, "e2", "e4", "queen"));
    }
}
