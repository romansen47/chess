package demo.chess.game;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import demo.chess.definitions.CastlingSide;
import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;

/**
 * Canonical identity of a chess position for rule decisions such as threefold repetition.
 *
 * <p>Piece placement alone is insufficient: the side to move, remaining castling
 * rights and a currently legal en-passant capture are all part of the position.
 * Keeping this definition in the chess core prevents higher layers from inventing
 * incompatible notions of position equality.</p>
 */
public record PositionIdentity(
        String piecePlacement,
        Color sideToMove,
        String castlingRights,
        String enPassantTargets) {

    /** Creates the canonical identity of the supplied game position. */
    public static PositionIdentity from(Game game) {
        Objects.requireNonNull(game, "game must not be null");
        return new PositionIdentity(
                piecePlacement(game),
                game.getPlayer() != null ? game.getPlayer().getColor() : null,
                castlingRights(game),
                legalEnPassantTargets(game));
    }

    private static String piecePlacement(Game game) {
        Board board = game.getChessBoard();
        StringBuilder value = new StringBuilder(64);
        for (int rank = 8; rank >= 1; rank--) {
            for (int file = 1; file <= 8; file++) {
                Field field = board != null ? board.getField(file, rank) : null;
                Piece piece = field != null ? field.getPiece() : null;
                value.append(pieceSymbol(piece));
            }
        }
        return value.toString();
    }

    private static char pieceSymbol(Piece piece) {
        if (piece == null || piece.getType() == null || piece.getColor() == null) return '.';
        char symbol = switch (piece.getType()) {
            case PAWN -> 'p';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case ROOK -> 'r';
            case QUEEN -> 'q';
            case KING -> 'k';
        };
        return piece.getColor() == Color.WHITE ? Character.toUpperCase(symbol) : symbol;
    }

    private static String castlingRights(Game game) {
        if (game.getCastlingRights() == null) return "-";
        StringBuilder value = new StringBuilder();
        appendCastlingRight(value, game, Color.WHITE, CastlingSide.KING_SIDE, "WK");
        appendCastlingRight(value, game, Color.WHITE, CastlingSide.QUEEN_SIDE, "WQ");
        appendCastlingRight(value, game, Color.BLACK, CastlingSide.KING_SIDE, "BK");
        appendCastlingRight(value, game, Color.BLACK, CastlingSide.QUEEN_SIDE, "BQ");
        return value.length() == 0 ? "-" : value.toString();
    }

    private static void appendCastlingRight(
            StringBuilder value,
            Game game,
            Color color,
            CastlingSide side,
            String label) {
        Integer rookFile = game.getCastlingRights().getRookFile(color, side);
        if (rookFile == null) return;
        if (value.length() > 0) value.append(',');
        value.append(label).append(rookFile);
    }

    private static String legalEnPassantTargets(Game game) {
        if (game.getPlayer() == null) return "-";
        Set<String> targets = new TreeSet<>();
        try {
            for (Move move : game.getPlayer().getValidMoves(game)) {
                if (move instanceof EnPassant && move.getTarget() != null && move.getTarget().getName() != null) {
                    targets.add(move.getTarget().getName());
                }
            }
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException("Could not determine legal en-passant state", e);
        }
        return targets.isEmpty() ? "-" : String.join(",", targets);
    }
}
