package demo.chess.notation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.game.DummyGame;

/**
 * Converts moves to and from standard PGN short algebraic notation (SAN).
 *
 * <p>The conversion deliberately operates on {@link DummyGame} only. This keeps
 * notation parsing/formatting independent from live-game clocks and other
 * {@code ChessGame} side effects.</p>
 */
public final class PgnNotation {

    private PgnNotation() {
    }

    public static String toSan(DummyGame game, Move move) throws NoMoveFoundException, IOException {
        if (game == null) {
            throw new NoMoveFoundException("game must not be null");
        }
        if (move == null || move.getPiece() == null || move.getSource() == null || move.getTarget() == null) {
            throw new NoMoveFoundException("move must not be null");
        }

        if (move instanceof Castling) {
            String uci = move.toString().toLowerCase(Locale.ROOT);
            return uci.endsWith("c1") || uci.endsWith("c8") ? "O-O-O" : "O-O";
        }

        PieceType pieceType = move.getPiece().getType();
        boolean capture = move instanceof EnPassant || move.getTarget().getPiece() != null;
        StringBuilder san = new StringBuilder();

        if (pieceType == PieceType.PAWN) {
            if (capture) {
                san.append(move.getSource().getName().charAt(0));
            }
        } else {
            san.append(pieceLetter(pieceType));
            san.append(sourceDisambiguation(game, move));
        }

        if (capture) {
            san.append('x');
        }

        san.append(move.getTarget().getName());

        if (move instanceof Promotion) {
            PieceType promotedType = ((Promotion) move).getPromotedPiece().getType();
            san.append('=').append(pieceLetter(promotedType));
        }

        return san.toString();
    }

    /**
     * Formats a move for the GUI move list using the historic Unicode piece
     * symbols while keeping the actual conversion on a {@link DummyGame}.
     * PGN export must continue to use {@link #toSan(DummyGame, Move)}.
     */
    public static String toDisplayNotation(DummyGame game, Move move)
            throws NoMoveFoundException, IOException {
        String notation = toSan(game, move);

        if (move instanceof Castling) {
            return notation.replace('O', '0');
        }

        if (move.getPiece().getType() != PieceType.PAWN && !notation.isEmpty()) {
            notation = unicodePiece(move.getPiece().getType(), move.getPiece().getColor())
                    + notation.substring(1);
        }

        if (move instanceof EnPassant) {
            notation += " e.p.";
        }

        return notation;
    }

    public static Move resolveSan(DummyGame game, String rawSan) throws NoMoveFoundException, IOException {
        if (game == null) {
            throw new NoMoveFoundException("game must not be null");
        }

        String wanted = normalizeForComparison(rawSan);
        if (wanted.isEmpty()) {
            throw new NoMoveFoundException("SAN move must not be empty");
        }

        Map<String, Move> matches = new LinkedHashMap<>();
        for (Move candidate : game.getPlayer().getValidMoves(game)) {
            String candidateSan = normalizeForComparison(toSan(game, candidate));
            if (candidateSan.equals(wanted)) {
                matches.putIfAbsent(candidate.toString().toLowerCase(Locale.ROOT), candidate);
            }
        }

        if (matches.size() == 1) {
            return matches.values().iterator().next();
        }
        if (matches.isEmpty()) {
            throw new NoMoveFoundException("No matching SAN move: " + rawSan);
        }
        throw new NoMoveFoundException("Ambiguous SAN move: " + rawSan);
    }

    public static String normalizeForComparison(String san) {
        if (san == null) {
            return "";
        }

        String normalized = san.trim()
                .replace('0', 'O')
                .replace("♔", "K")
                .replace("♕", "Q")
                .replace("♖", "R")
                .replace("♗", "B")
                .replace("♘", "N")
                .replace("♚", "K")
                .replace("♛", "Q")
                .replace("♜", "R")
                .replace("♝", "B")
                .replace("♞", "N");

        normalized = normalized.replaceAll("(?i)\\s*e\\.p\\.$", "");
        normalized = normalized.replaceAll("[!?]+$", "");
        normalized = normalized.replaceAll("[+#]+$", "");
        normalized = normalized.replaceAll("[!?]+$", "");
        return normalized;
    }

    private static String sourceDisambiguation(DummyGame game, Move move)
            throws NoMoveFoundException, IOException {
        List<Move> competingMoves = new ArrayList<>();

        for (Move candidate : game.getPlayer().getValidMoves(game)) {
            if (candidate == null
                    || candidate.getPiece() == null
                    || candidate.getSource() == null
                    || candidate.getTarget() == null
                    || candidate instanceof Castling) {
                continue;
            }

            if (candidate.getSource().equals(move.getSource())) {
                continue;
            }

            if (candidate.getTarget().equals(move.getTarget())
                    && candidate.getPiece().getType() == move.getPiece().getType()) {
                competingMoves.add(candidate);
            }
        }

        if (competingMoves.isEmpty()) {
            return "";
        }

        boolean sameFileExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getFile() == move.getSource().getFile());
        boolean sameRankExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getRank() == move.getSource().getRank());

        if (sameFileExists && sameRankExists) {
            return move.getSource().getName();
        }
        if (sameFileExists) {
            return Integer.toString(move.getSource().getRank());
        }
        return move.getSource().getName().substring(0, 1);
    }

    private static String unicodePiece(PieceType pieceType, Color color) {
        if (pieceType == null || color == null) {
            return "";
        }

        switch (color) {
            case WHITE:
                switch (pieceType) {
                    case KING:
                        return "♔";
                    case QUEEN:
                        return "♕";
                    case ROOK:
                        return "♖";
                    case BISHOP:
                        return "♗";
                    case KNIGHT:
                        return "♘";
                    default:
                        return "";
                }
            case BLACK:
                switch (pieceType) {
                    case KING:
                        return "♚";
                    case QUEEN:
                        return "♛";
                    case ROOK:
                        return "♜";
                    case BISHOP:
                        return "♝";
                    case KNIGHT:
                        return "♞";
                    default:
                        return "";
                }
            default:
                return "";
        }
    }

    private static char pieceLetter(PieceType pieceType) {
        if (pieceType == null) {
            return '?';
        }

        switch (pieceType) {
            case KING:
                return 'K';
            case QUEEN:
                return 'Q';
            case ROOK:
                return 'R';
            case BISHOP:
                return 'B';
            case KNIGHT:
                return 'N';
            default:
                return '?';
        }
    }
}
