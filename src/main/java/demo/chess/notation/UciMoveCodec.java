package demo.chess.notation;

import java.util.Locale;

import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

/** Converts domain moves to the UCI dialect required for a game's start position. */
public final class UciMoveCodec {

    private UciMoveCodec() {
    }

    public static String encode(Game game, Move move) {
        if (move == null) return "";
        if (move instanceof Castling castling
                && game != null
                && game.getStartingPosition() != null
                && !game.getStartingPosition().isStandard()) {
            // Chess960 UCI encodes castling as king source -> original rook source.
            // Move.target intentionally keeps that rook source even after application.
            return castling.getSource().getName() + castling.getTarget().getName();
        }
        return move.toString();
    }

    public static boolean matches(Game game, Move candidate, String rawUci) {
        if (candidate == null || rawUci == null) return false;
        return encode(game, candidate).equalsIgnoreCase(rawUci.trim().toLowerCase(Locale.ROOT));
    }
}
