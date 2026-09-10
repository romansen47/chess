package demo.chess.game;

import java.io.IOException;
import java.util.Locale;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.players.Player;

/**
 * Resolves external move descriptions to the canonical legal move object of a game.
 */
public final class LegalMoveResolver {

    private LegalMoveResolver() {
    }

    /**
     * Resolves one UCI move against the legal moves of the current player.
     *
     * @param game current game position
     * @param rawUci UCI move such as e2e4 or e7e8q
     * @return matching legal move
     * @throws NoMoveFoundException when no matching legal move exists
     * @throws IOException when legal move generation fails
     */
    public static Move resolveUci(Game game, String rawUci)
            throws NoMoveFoundException, IOException {
        Player player = requireCurrentPlayer(game);
        String wanted = rawUci != null ? rawUci.trim().toLowerCase(Locale.ROOT) : "";
        if (wanted.isEmpty()) {
            throw new NoMoveFoundException("UCI move must not be empty");
        }

        for (Move candidate : player.getValidMoves(game)) {
            if (candidate != null && wanted.equals(candidate.toString().toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }

        throw new NoMoveFoundException("No legal move for UCI " + rawUci);
    }

    /**
     * Resolves one coordinate move against the legal moves of the current player.
     *
     * @param game current game position
     * @param from source square
     * @param to target square
     * @param promotion optional promotion piece (q/queen, r/rook, b/bishop, n/knight)
     * @return matching legal move
     * @throws NoMoveFoundException when no matching legal move exists
     * @throws IOException when legal move generation fails
     */
    public static Move resolveCoordinates(
            Game game,
            String from,
            String to,
            String promotion) throws NoMoveFoundException, IOException {
        Player player = requireCurrentPlayer(game);
        if (from == null || to == null) {
            throw new NoMoveFoundException("from/to must not be null");
        }

        String normalizedFrom = from.trim().toLowerCase(Locale.ROOT);
        String normalizedTo = to.trim().toLowerCase(Locale.ROOT);
        String promotionLabel = normalizePromotion(promotion);
        boolean coordinatesMatched = false;

        for (Move candidate : player.getValidMoves(game)) {
            if (candidate == null || candidate.getSource() == null || candidate.getTarget() == null) {
                continue;
            }

            String sourceName = candidate.getSource().getName();
            String targetName = candidate.getTarget().getName();
            if (sourceName == null || targetName == null
                    || !normalizedFrom.equals(sourceName.toLowerCase(Locale.ROOT))
                    || !normalizedTo.equals(targetName.toLowerCase(Locale.ROOT))) {
                continue;
            }

            coordinatesMatched = true;
            if (promotionLabel == null) {
                return candidate;
            }

            if (candidate instanceof Promotion promotionMove
                    && promotionMove.getPromotedPiece() != null
                    && promotionMove.getPromotedPiece().getType() != null
                    && promotionLabel.equals(promotionMove.getPromotedPiece().getType().label)) {
                return candidate;
            }
        }

        if (coordinatesMatched && promotionLabel != null) {
            throw new NoMoveFoundException(
                    "No legal promotion move for " + from + " -> " + to + " with promotion " + promotion);
        }

        throw new NoMoveFoundException("No legal move for " + from + " -> " + to);
    }

    private static Player requireCurrentPlayer(Game game) throws NoMoveFoundException {
        if (game == null) {
            throw new NoMoveFoundException("game must not be null");
        }
        if (game.getPlayer() == null) {
            throw new NoMoveFoundException("game has no current player");
        }
        return game.getPlayer();
    }

    private static String normalizePromotion(String promotion) {
        if (promotion == null || promotion.isBlank()) {
            return null;
        }

        return switch (promotion.trim().toLowerCase(Locale.ROOT)) {
            case "q", "queen" -> "q";
            case "r", "rook" -> "r";
            case "b", "bishop" -> "b";
            case "n", "knight" -> "n";
            default -> promotion.trim().toLowerCase(Locale.ROOT);
        };
    }
}
