package demo.chess.definitions.engines.impl;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

/**
 * Builds self-contained UCI position commands for every Scharnagl position.
 *
 * <p>The command always uses the starting position's FEN. Position 518 is not
 * translated to the UCI {@code startpos} shorthand; this keeps one protocol
 * path for all 960 positions.</p>
 */
final class UciPositionCommand {

    private UciPositionCommand() {
    }

    static String build(CharSequence moves) {
        return build(ChessStartingPosition.STANDARD, moves);
    }

    static String build(Game game) {
        ChessStartingPosition startingPosition = game != null
                && game.getStartingPosition() != null
                ? game.getStartingPosition()
                : ChessStartingPosition.STANDARD;

        StringBuilder moves = new StringBuilder();
        if (game != null) {
            for (Move move : game.getMoveList()) {
                if (!moves.isEmpty()) moves.append(' ');
                moves.append(UciMoveCodec.encode(startingPosition, move));
            }
        }
        return build(startingPosition, moves);
    }

    private static String build(
            ChessStartingPosition startingPosition,
            CharSequence moves) {
        ChessStartingPosition resolved = startingPosition != null
                ? startingPosition
                : ChessStartingPosition.STANDARD;
        StringBuilder command = new StringBuilder("position fen ")
                .append(resolved.initialFen());
        String moveText = moves == null ? "" : moves.toString().trim();
        if (!moveText.isEmpty()) {
            command.append(" moves ").append(moveText);
        }
        return command.toString();
    }
}
