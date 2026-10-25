package demo.chess.notation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/** Focused regression coverage for the domain-to-UCI protocol boundary. */
public class UciMoveCodecTest {

    /**
     * Verifies that ordinary moves keep their normal UCI coordinate notation.
     *
     * @throws Exception when the legal move cannot be resolved
     */
    @Test
    public void encodesOrdinaryMoveWithoutProtocolTranslation() throws Exception {
        Game game = Simulation.createSimulation();
        Move move = LegalMoveResolver.resolveUci(game, "e2e4");

        assertEquals("e2e4", UciMoveCodec.encode(game, move));
        assertTrue(UciMoveCodec.matches(game, move, "E2E4"));
    }

    /**
     * Verifies that Scharnagl 518 castling uses the Chess960 king-to-rook form.
     *
     * @throws Exception when the preparation sequence cannot be applied
     */
    @Test
    public void encodesStandardCastlingWithChess960RookSquare() throws Exception {
        Game game = Simulation.createSimulation();
        apply(game, "e2e4", "e7e5", "g1f3", "b8c6", "f1e2", "g8f6");

        Move castling = LegalMoveResolver.resolveUci(game, "e1h1");

        assertEquals("e1h1", UciMoveCodec.encode(game, castling));
        assertTrue(UciMoveCodec.matches(game, castling, "e1h1"));
        assertFalse(UciMoveCodec.matches(game, castling, "e1g1"));
    }

    private void apply(Game game, String... moves) throws Exception {
        for (String uci : moves) {
            game.apply(LegalMoveResolver.resolveUci(game, uci));
        }
    }
}
