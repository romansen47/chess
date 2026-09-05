package demo.chess.notation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /**
     * Creates a new PgnNotation instance.
     */
    private PgnNotation() {
    }

    /**
     * Performs the to san operation.
     * @param game the game
     * @param move the move
     * @return the result of the operation
     */
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
     * Performs the to display notation operation.
     * @param game the game
     * @param move the move
     * @return the result of the operation
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

    /**
     * Resolves SAN to one legal move.
     *
     * <p>This parser intentionally matches the SAN token against the already
     * generated legal move list instead of formatting every legal candidate back
     * to SAN. Besides avoiding unnecessary string work, this is important for
     * bulk PGN imports because SAN formatting may itself need legal-move
     * generation for source disambiguation.</p>
     *
     * @param game the game
     * @param rawSan the raw SAN token
     * @return the matching legal move
     */
    public static Move resolveSan(DummyGame game, String rawSan) throws NoMoveFoundException, IOException {
        if (game == null) {
            throw new NoMoveFoundException("game must not be null");
        }

        String wanted = normalizeForComparison(rawSan);
        if (wanted.isEmpty()) {
            throw new NoMoveFoundException("SAN move must not be empty");
        }

        List<Move> validMoves = game.getPlayer().getValidMoves(game);

        if ("O-O".equals(wanted) || "O-O-O".equals(wanted)) {
            List<Move> matches = new ArrayList<>();
            boolean queenSide = "O-O-O".equals(wanted);
            for (Move candidate : validMoves) {
                if (!(candidate instanceof Castling)) {
                    continue;
                }
                String uci = candidate.toString().toLowerCase(Locale.ROOT);
                boolean candidateQueenSide = uci.endsWith("c1") || uci.endsWith("c8");
                if (candidateQueenSide == queenSide) {
                    matches.add(candidate);
                }
            }
            return requireSingleMatch(matches, rawSan);
        }

        SanDescriptor descriptor = parseSanDescriptor(wanted, rawSan);
        List<Move> matches = new ArrayList<>();
        for (Move candidate : validMoves) {
            if (matchesDescriptor(candidate, descriptor)) {
                matches.add(candidate);
            }
        }
        return requireSingleMatch(matches, rawSan);
    }

    /**
     * Normalizes SAN for comparison and parsing.
     * @param san SAN text
     * @return normalized SAN
     */
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

    /**
     * Parses the structural parts of one non-castling SAN token.
     *
     * @param wanted normalized SAN token
     * @param rawSan original token used for error reporting
     * @return parsed descriptor
     * @throws NoMoveFoundException when the token is structurally invalid
     */
    private static SanDescriptor parseSanDescriptor(String wanted, String rawSan)
            throws NoMoveFoundException {
        String san = wanted;
        PieceType promotionType = null;

        int promotionSeparator = san.lastIndexOf('=');
        if (promotionSeparator >= 0) {
            if (promotionSeparator != san.length() - 2) {
                throw new NoMoveFoundException("Invalid SAN move: " + rawSan);
            }
            promotionType = pieceTypeFromLetter(san.charAt(san.length() - 1));
            if (promotionType == PieceType.KING || promotionType == PieceType.PAWN) {
                throw new NoMoveFoundException("Invalid SAN promotion: " + rawSan);
            }
            san = san.substring(0, promotionSeparator);
        } else if (san.length() >= 3
                && isPromotionLetter(san.charAt(san.length() - 1))
                && isSquare(san.substring(san.length() - 3, san.length() - 1))) {
            promotionType = pieceTypeFromLetter(san.charAt(san.length() - 1));
            san = san.substring(0, san.length() - 1);
        }

        if (san.length() < 2) {
            throw new NoMoveFoundException("Invalid SAN move: " + rawSan);
        }

        String targetSquare = san.substring(san.length() - 2).toLowerCase(Locale.ROOT);
        if (!isSquare(targetSquare)) {
            throw new NoMoveFoundException("Invalid SAN target square: " + rawSan);
        }

        String prefix = san.substring(0, san.length() - 2);
        boolean capture = prefix.indexOf('x') >= 0 || prefix.indexOf('X') >= 0;
        prefix = prefix.replace("x", "").replace("X", "");

        PieceType pieceType = PieceType.PAWN;
        if (!prefix.isEmpty() && isPieceLetter(prefix.charAt(0))) {
            pieceType = pieceTypeFromLetter(prefix.charAt(0));
            prefix = prefix.substring(1);
        }

        Character sourceFile = null;
        Integer sourceRank = null;
        if (!prefix.isEmpty()) {
            if (prefix.length() > 2) {
                throw new NoMoveFoundException("Invalid SAN source disambiguation: " + rawSan);
            }
            for (int index = 0; index < prefix.length(); index++) {
                char value = Character.toLowerCase(prefix.charAt(index));
                if (value >= 'a' && value <= 'h') {
                    if (sourceFile != null) {
                        throw new NoMoveFoundException("Invalid SAN source file: " + rawSan);
                    }
                    sourceFile = value;
                } else if (value >= '1' && value <= '8') {
                    if (sourceRank != null) {
                        throw new NoMoveFoundException("Invalid SAN source rank: " + rawSan);
                    }
                    sourceRank = value - '0';
                } else {
                    throw new NoMoveFoundException("Invalid SAN source disambiguation: " + rawSan);
                }
            }
        }

        if (pieceType == PieceType.PAWN && !capture && (sourceFile != null || sourceRank != null)) {
            throw new NoMoveFoundException("Invalid pawn SAN move: " + rawSan);
        }

        return new SanDescriptor(
                pieceType,
                targetSquare,
                capture,
                sourceFile,
                sourceRank,
                promotionType);
    }

    /**
     * Returns whether a legal move matches a parsed SAN descriptor.
     *
     * @param candidate legal candidate
     * @param descriptor parsed SAN
     * @return true when the candidate matches
     */
    private static boolean matchesDescriptor(Move candidate, SanDescriptor descriptor) {
        if (candidate == null
                || candidate.getPiece() == null
                || candidate.getSource() == null
                || candidate.getTarget() == null
                || candidate instanceof Castling) {
            return false;
        }

        if (candidate.getPiece().getType() != descriptor.pieceType()) {
            return false;
        }
        if (!candidate.getTarget().getName().equalsIgnoreCase(descriptor.targetSquare())) {
            return false;
        }

        boolean candidateCapture = candidate instanceof EnPassant || candidate.getTarget().getPiece() != null;
        if (candidateCapture != descriptor.capture()) {
            return false;
        }

        if (descriptor.sourceFile() != null
                && Character.toLowerCase(candidate.getSource().getName().charAt(0)) != descriptor.sourceFile()) {
            return false;
        }
        if (descriptor.sourceRank() != null
                && candidate.getSource().getRank() != descriptor.sourceRank()) {
            return false;
        }

        if (descriptor.promotionType() == null) {
            return !(candidate instanceof Promotion);
        }
        if (!(candidate instanceof Promotion promotion)) {
            return false;
        }
        return promotion.getPromotedPiece().getType() == descriptor.promotionType();
    }

    /**
     * Returns exactly one semantically distinct move or reports a normal SAN
     * resolution error. The engine move generator can expose the same legal move
     * more than once as separate objects, so equivalent UCI moves are collapsed
     * before ambiguity is evaluated.
     *
     * @param matches candidate matches
     * @param rawSan source SAN token
     * @return unique move
     */
    private static Move requireSingleMatch(List<Move> matches, String rawSan) throws NoMoveFoundException {
        Move uniqueMove = null;
        String uniqueUci = null;

        for (Move candidate : matches) {
            if (candidate == null) {
                continue;
            }

            String candidateUci = candidate.toString().toLowerCase(Locale.ROOT);
            if (uniqueMove == null) {
                uniqueMove = candidate;
                uniqueUci = candidateUci;
                continue;
            }

            if (!candidateUci.equals(uniqueUci)) {
                throw new NoMoveFoundException("Ambiguous SAN move: " + rawSan);
            }
        }

        if (uniqueMove == null) {
            throw new NoMoveFoundException("No matching SAN move: " + rawSan);
        }
        return uniqueMove;
    }

    /**
     * Returns whether a string is a chess square.
     *
     * @param value candidate square
     * @return true for a1 through h8
     */
    private static boolean isSquare(String value) {
        return value != null
                && value.length() == 2
                && Character.toLowerCase(value.charAt(0)) >= 'a'
                && Character.toLowerCase(value.charAt(0)) <= 'h'
                && value.charAt(1) >= '1'
                && value.charAt(1) <= '8';
    }

    /**
     * Returns whether the character names a normal non-pawn piece.
     *
     * @param value piece letter
     * @return true for K, Q, R, B or N
     */
    private static boolean isPieceLetter(char value) {
        char normalized = Character.toUpperCase(value);
        return normalized == 'K'
                || normalized == 'Q'
                || normalized == 'R'
                || normalized == 'B'
                || normalized == 'N';
    }

    /**
     * Returns whether the character can name a promotion piece.
     *
     * @param value piece letter
     * @return true for Q, R, B or N
     */
    private static boolean isPromotionLetter(char value) {
        char normalized = Character.toUpperCase(value);
        return normalized == 'Q'
                || normalized == 'R'
                || normalized == 'B'
                || normalized == 'N';
    }

    /**
     * Maps a SAN piece letter to a piece type.
     *
     * @param value SAN piece letter
     * @return piece type
     * @throws NoMoveFoundException for unsupported letters
     */
    private static PieceType pieceTypeFromLetter(char value) throws NoMoveFoundException {
        return switch (Character.toUpperCase(value)) {
            case 'K' -> PieceType.KING;
            case 'Q' -> PieceType.QUEEN;
            case 'R' -> PieceType.ROOK;
            case 'B' -> PieceType.BISHOP;
            case 'N' -> PieceType.KNIGHT;
            default -> throw new NoMoveFoundException("Invalid SAN piece: " + value);
        };
    }

    /**
     * Performs the source disambiguation operation.
     * @param game the game
     * @param move the move
     * @return the result of the operation
     */
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

    /**
     * Performs the unicode piece operation.
     * @param pieceType the piece type
     * @param color the color
     * @return the result of the operation
     */
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

    /**
     * Performs the piece letter operation.
     * @param pieceType the piece type
     * @return the result of the operation
     */
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

    /**
     * Parsed structural parts of one non-castling SAN token.
     *
     * @param pieceType moving piece type
     * @param targetSquare target square
     * @param capture whether SAN contains a capture marker
     * @param sourceFile optional source-file disambiguation
     * @param sourceRank optional source-rank disambiguation
     * @param promotionType optional promotion type
     */
    private record SanDescriptor(
            PieceType pieceType,
            String targetSquare,
            boolean capture,
            Character sourceFile,
            Integer sourceRank,
            PieceType promotionType) {
    }
}
