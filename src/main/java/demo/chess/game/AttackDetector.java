package demo.chess.game;

import demo.chess.definitions.Color;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.pieces.impl.Pawn;
import demo.chess.definitions.players.Player;

/**
 * Stateless helper for determining whether a board field is attacked by a player.
 *
 * <p>Attack detection is deliberately separated from legal-move generation.
 * In particular, pawn attacks are diagonal even when the target square is empty,
 * while castling is never an attack. This distinction is required when checking
 * check status and Chess960 castling paths.</p>
 */
public final class AttackDetector {

    private AttackDetector() {
    }

    /**
     * Returns whether any piece of {@code attacker} attacks {@code target} in the
     * current board position.
     *
     * @param target field to inspect
     * @param attacker player whose pieces form the attack map
     * @return {@code true} if the field is attacked
     */
    public static boolean isAttacked(Field target, Player attacker) {
        if (target == null || attacker == null) return false;

        for (Piece piece : attacker.getPieces()) {
            if (piece == null || piece.getField() == null) continue;

            if (piece instanceof Pawn pawn) {
                int direction = pawn.getColor() == Color.WHITE ? 1 : -1;
                if (target.getRank() == pawn.getField().getRank() + direction
                        && Math.abs(target.getFile() - pawn.getField().getFile()) == 1) {
                    return true;
                }
                continue;
            }

            for (Move attack : piece.getSimpleUnvalidatedMoves()) {
                if (attack instanceof Castling || attack.getTarget() == null) continue;
                if (attack.getTarget().equals(target)) return true;
            }
        }
        return false;
    }
}
