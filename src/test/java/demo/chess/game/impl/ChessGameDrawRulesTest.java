package demo.chess.game.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Test;

import demo.chess.admin.impl.ChessAdmin;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;

public class ChessGameDrawRulesTest {

    private Game game;

    @After
    public void stopClocks() {
        if (game == null) return;
        if (game.getWhitePlayer().getChessClock() != null) game.getWhitePlayer().getChessClock().stop();
        if (game.getBlackPlayer().getChessClock() != null) game.getBlackPlayer().getChessClock().stop();
    }

    @Test
    public void thirdOccurrenceOfTheSameCompletePositionEndsTheGame() throws Exception {
        game = new ChessAdmin().chessGame(300);

        apply("g1f3", "g8f6", "f3g1", "f6g8");
        assertNull(game.getState());

        apply("g1f3", "g8f6", "f3g1", "f6g8");

        assertEquals(State.DRAW_BY_THREEFOLD_REPETITION, game.getState());
    }

    private void apply(String... moves) throws Exception {
        for (String uci : moves) {
            game.apply(LegalMoveResolver.resolveUci(game, uci));
        }
    }
}
