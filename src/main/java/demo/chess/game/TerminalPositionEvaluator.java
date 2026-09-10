package demo.chess.game;

import java.io.IOException;
import java.util.Objects;

import demo.chess.definitions.Color;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;

/**
 * Determines terminal position states without mutating the game.
 *
 * <p>An already explicit game state is returned unchanged. Otherwise this
 * evaluator detects checkmate or stalemate from legal move availability and
 * king attack status. It deliberately does not set a state, stop clocks or
 * invent history-dependent draw states for simulations.</p>
 */
public final class TerminalPositionEvaluator {

    private TerminalPositionEvaluator() {
    }

    /**
     * Determines the terminal state of the supplied position.
     *
     * @param game game or simulation to inspect
     * @return explicit state, detected mate/stalemate, or {@code null}
     */
    public static State determineState(Game game) {
        if (game == null) {
            return null;
        }

        if (game.getState() != null) {
            return game.getState();
        }

        Player playerToMove = game.getPlayer();
        if (!hasKingPosition(playerToMove)) {
            return null;
        }

        try {
            if (!playerToMove.getValidMoves(game).isEmpty()) {
                return null;
            }
        } catch (NoMoveFoundException | IOException e) {
            return null;
        }

        return determineStateWhenNoLegalMoves(game);
    }

    /**
     * Classifies a position that is already known to have no legal moves.
     *
     * @param game game or simulation to inspect
     * @return mate/stalemate state, or {@code null} when the position is incomplete
     */
    public static State determineStateWhenNoLegalMoves(Game game) {
        if (game == null) {
            return null;
        }

        Player playerToMove = game.getPlayer();
        if (!hasKingPosition(playerToMove)) {
            return null;
        }

        Player opponent = playerToMove.getColor() == Color.WHITE
                ? game.getBlackPlayer()
                : game.getWhitePlayer();
        if (opponent == null) {
            return null;
        }

        boolean kingIsAttacked = opponent.getSimpleMoves().stream()
                .map(Move::getTarget)
                .filter(Objects::nonNull)
                .anyMatch(playerToMove.getKing().getField()::equals);

        if (!kingIsAttacked) {
            return State.STALEMATE;
        }

        return playerToMove.getColor() == Color.WHITE
                ? State.WHITE_MATED
                : State.BLACK_MATED;
    }

    private static boolean hasKingPosition(Player player) {
        return player != null
                && player.getColor() != null
                && player.getKing() != null
                && player.getKing().getField() != null;
    }
}
