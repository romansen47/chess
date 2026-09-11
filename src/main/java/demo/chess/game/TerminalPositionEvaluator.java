package demo.chess.game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
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

        if (hasInsufficientMatingMaterial(game)) {
            return State.DRAW_BY_INSUFFICIENT_MATERIAL;
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
     * Returns whether neither side can ever deliver checkmate with the material
     * remaining on the board.
     *
     * <p>This intentionally recognizes only material configurations that are
     * unambiguously dead from material alone: bare kings, a single bishop or
     * knight against a bare king, and bishop-only positions where every bishop
     * is confined to the same square color. Positions with two knights, bishop
     * and knight, opposite-colored bishops, pawns, rooks or queens are not
     * declared drawn by this method.</p>
     *
     * @param game game or simulation to inspect
     * @return true when the remaining material is insufficient for any mate
     */
    public static boolean hasInsufficientMatingMaterial(Game game) {
        if (game == null
                || !hasKingPosition(game.getWhitePlayer())
                || !hasKingPosition(game.getBlackPlayer())) {
            return false;
        }

        List<Piece> nonKingPieces = new ArrayList<>();
        collectNonKingPieces(game.getWhitePlayer(), nonKingPieces);
        collectNonKingPieces(game.getBlackPlayer(), nonKingPieces);

        if (nonKingPieces.isEmpty()) {
            return true;
        }

        if (nonKingPieces.stream().anyMatch(piece ->
                piece.getType() == PieceType.PAWN
                        || piece.getType() == PieceType.ROOK
                        || piece.getType() == PieceType.QUEEN)) {
            return false;
        }

        if (nonKingPieces.size() == 1) {
            PieceType type = nonKingPieces.get(0).getType();
            return type == PieceType.BISHOP || type == PieceType.KNIGHT;
        }

        if (nonKingPieces.stream().allMatch(piece -> piece.getType() == PieceType.BISHOP)) {
            Color bishopSquareColor = nonKingPieces.get(0).getField().getColor();
            return nonKingPieces.stream()
                    .allMatch(piece -> piece.getField() != null
                            && piece.getField().getColor() == bishopSquareColor);
        }

        return false;
    }

    private static void collectNonKingPieces(Player player, List<Piece> target) {
        if (player == null || player.getPieces() == null) {
            return;
        }

        player.getPieces().stream()
                .filter(Objects::nonNull)
                .filter(piece -> piece.getField() != null)
                .filter(piece -> piece.getType() != PieceType.KING)
                .forEach(target::add);
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
