package demo.chess.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import demo.chess.admin.impl.ChessAdmin;
import demo.chess.definitions.Color;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.states.State;

/**
 * Regression tests for time-management ownership in the chess core.
 */
public class ChessGameTimeManagementTest {

    @Test
    public void configuresCompleteTimeControlInCore() {
        Game game = new ChessAdmin().chessGame(300);

        game.configureTimeControl(2, 3, 30);

        assertEquals(2, game.getIncrementForWhite());
        assertEquals(3, game.getIncrementForBlack());
        assertEquals(30, game.getWhitePlayer().getAdditionalTime());
        assertEquals(30, game.getBlackPlayer().getAdditionalTime());
        assertNull(game.getTimedOutColor());
    }

    @Test
    public void expiredClockRejectsMoveAndRecordsTimedOutColor() throws Exception {
        Game game = new ChessAdmin().chessGame(300);
        game.configureTimeControl(0, 0, 0);
        game.getWhitePlayer().getChessClock().setTargetTimeMillis(0L);

        Move move = LegalMoveResolver.resolveUci(game, "e2e4");
        game.apply(move);

        assertEquals(State.LOST_ON_TIME, game.getState());
        assertEquals(Color.WHITE, game.getTimedOutColor());
        assertTrue(game.getMoveList().isEmpty());
        assertTrue(game.getSanMoveList().isEmpty());
    }

    @Test
    public void clockCallbackEndsGameInsideCore() throws Exception {
        Game game = new ChessAdmin().chessGame(300);
        game.configureTimeControl(0, 0, 0);
        game.getWhitePlayer().getChessClock().setTargetTimeMillis(0L);

        game.getWhitePlayer().getChessClock().start();

        long deadline = System.currentTimeMillis() + 1_000L;
        while (game.getState() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }

        assertEquals(State.LOST_ON_TIME, game.getState());
        assertEquals(Color.WHITE, game.getTimedOutColor());
        assertTrue(!game.getWhitePlayer().getChessClock().isRunning());
        assertTrue(!game.getBlackPlayer().getChessClock().isRunning());
    }
}
