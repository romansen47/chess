package demo.chess.definitions.engines;

import java.util.ArrayList;
import java.util.List;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

/**
 * Immutable identity of a position as seen by UCI engines.
 *
 * <p>The starting-position id is part of the key, so equal move lists from
 * different Chess960 positions can never share evaluation cache entries.</p>
 */
public record UciPositionKey(
        int startingPositionId,
        List<String> moves) {

    public UciPositionKey {
        ChessStartingPosition.of(startingPositionId);
        moves = moves == null ? List.of() : List.copyOf(moves);
    }

    public static UciPositionKey from(Game game) {
        ChessStartingPosition startingPosition = game != null
                && game.getStartingPosition() != null
                ? game.getStartingPosition()
                : ChessStartingPosition.STANDARD;

        List<String> moves = new ArrayList<>();
        if (game != null) {
            for (Move move : game.getMoveList()) {
                moves.add(UciMoveCodec.encode(startingPosition, move));
            }
        }
        return new UciPositionKey(startingPosition.getId(), moves);
    }
}
