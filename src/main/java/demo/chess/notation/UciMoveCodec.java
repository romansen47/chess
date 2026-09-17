package demo.chess.notation;

import java.util.Locale;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

/**
 * Encodes and matches domain moves in the UCI move dialect.
 *
 * <p>This class is the protocol boundary between the domain model and UCI.
 * Domain code must not rely on {@link Move#toString()} being identical to a
 * UCI move, because Chess960 castling has a protocol-specific representation.</p>
 *
 * <p>For standard chess, castling is encoded conventionally (for example
 * {@code e1g1}). In Chess960 UCI, castling is encoded as king source to the
 * original castling rook square (for example {@code g1h1}).</p>
 */
public final class UciMoveCodec {

    private UciMoveCodec() {
    }

    /**
     * Encodes a move using the starting-position context of a game.
     *
     * @param game game providing the variant context
     * @param move domain move
     * @return UCI move text, or an empty string for a null move
     */
    public static String encode(Game game, Move move) {
        ChessStartingPosition startingPosition = game != null
                ? game.getStartingPosition()
                : ChessStartingPosition.STANDARD;
        return encode(startingPosition, move);
    }

    /**
     * Encodes a move using an explicit starting-position context.
     *
     * @param startingPosition initial position; null is treated as standard chess
     * @param move domain move
     * @return UCI move text, or an empty string for a null move
     */
    public static String encode(ChessStartingPosition startingPosition, Move move) {
        if (move == null) return "";
        ChessStartingPosition position = startingPosition != null
                ? startingPosition
                : ChessStartingPosition.STANDARD;
        if (move instanceof Castling castling && !position.isStandard()) {
            return castling.getSource().getName() + castling.getRookSource().getName();
        }
        return move.toString();
    }

    /**
     * Tests whether raw UCI text denotes a candidate move in a game's context.
     */
    public static boolean matches(Game game, Move candidate, String rawUci) {
        ChessStartingPosition startingPosition = game != null
                ? game.getStartingPosition()
                : ChessStartingPosition.STANDARD;
        return matches(startingPosition, candidate, rawUci);
    }

    /**
     * Tests whether raw UCI text denotes a candidate move in an explicit
     * starting-position context.
     */
    public static boolean matches(
            ChessStartingPosition startingPosition,
            Move candidate,
            String rawUci) {
        if (candidate == null || rawUci == null) return false;
        return encode(startingPosition, candidate)
                .equalsIgnoreCase(rawUci.trim().toLowerCase(Locale.ROOT));
    }
}
