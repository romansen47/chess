package demo.chess.game;

import demo.chess.definitions.Color;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.definitions.players.Player;

/**
 * Validates the geometry and attack constraints of castling moves.
 *
 * <p>The validator is Chess960-aware: king and rook start files are taken from
 * the move and the game's castling rights, while final squares are always the
 * standard Chess960 destinations (king on g/c, rook on f/d). It also supports
 * cases where either piece is already on its destination square.</p>
 */
public final class CastlingValidator {

    private CastlingValidator() {
    }

    /**
     * Checks whether a candidate castling move is legal in the current position.
     *
     * @param game current game
     * @param player player attempting to castle
     * @param castling castling candidate
     * @return {@code true} when the castling move satisfies all castling-specific rules
     */
    public static boolean isLegal(Game game, Player player, Castling castling) {
        if (game == null || player == null || castling == null) return false;

        Piece king = player.getKing();
        Rook rook = castling.getRook();
        if (king == null || rook == null || king.getField() == null || rook.getField() == null) return false;
        if (rook.getColor() != player.getColor()) return false;

        Integer expectedRookFile = game.getCastlingRights().getRookFile(player.getColor(), castling.getSide());
        if (expectedRookFile == null || expectedRookFile != castling.getRookSource().getFile()) return false;

        Field kingSource = king.getField();
        Field rookSource = castling.getRookSource();
        Field kingTarget = castling.getKingTarget();
        Field rookTarget = castling.getRookTarget();
        if (kingSource.getRank() != rookSource.getRank()
                || kingSource.getRank() != kingTarget.getRank()
                || kingSource.getRank() != rookTarget.getRank()) {
            return false;
        }

        if (!pathClear(game, kingSource, kingTarget, kingSource, rookSource)
                || !pathClear(game, rookSource, rookTarget, kingSource, rookSource)) {
            return false;
        }

        Player opponent = player.getColor() == Color.WHITE
                ? game.getBlackPlayer()
                : game.getWhitePlayer();
        int direction = Integer.compare(kingTarget.getFile(), kingSource.getFile());
        if (direction == 0) return !AttackDetector.isAttacked(kingSource, opponent);

        for (int file = kingSource.getFile(); ; file += direction) {
            Field traversed = game.getChessBoard().getField(file, kingSource.getRank());
            if (AttackDetector.isAttacked(traversed, opponent)) return false;
            if (file == kingTarget.getFile()) break;
        }
        return true;
    }

    private static boolean pathClear(
            Game game,
            Field from,
            Field to,
            Field kingSource,
            Field rookSource) {
        int min = Math.min(from.getFile(), to.getFile());
        int max = Math.max(from.getFile(), to.getFile());
        int rank = from.getRank();
        for (int file = min; file <= max; file++) {
            Field field = game.getChessBoard().getField(file, rank);
            if (field.equals(kingSource) || field.equals(rookSource)) continue;
            if (field.getPiece() != null) return false;
        }
        return true;
    }
}
