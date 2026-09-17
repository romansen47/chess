package demo.chess.definitions.engines.impl;

import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

/** Builds UCI position commands for classical chess and Chess960. */
final class UciPositionCommand {

    private UciPositionCommand() {
    }

    static String build(CharSequence moves) {
        String moveText = moves == null ? "" : moves.toString().trim();
        if (moveText.isEmpty()) return "position startpos";
        return "position startpos moves " + moveText;
    }

    static String build(Game game) {
        if (game == null || game.getStartingPosition() == null || game.getStartingPosition().isStandard()) {
            StringBuilder moves = new StringBuilder();
            if (game != null) {
                for (Move move : game.getMoveList()) {
                    if (!moves.isEmpty()) moves.append(' ');
                    moves.append(UciMoveCodec.encode(game, move));
                }
            }
            return build(moves);
        }

        StringBuilder command = new StringBuilder("position fen ")
                .append(game.getStartingPosition().initialFen());
        if (!game.getMoveList().isEmpty()) {
            command.append(" moves");
            for (Move move : game.getMoveList()) {
                command.append(' ').append(UciMoveCodec.encode(game, move));
            }
        }
        return command.toString();
    }
}
