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
import demo.chess.definitions.players.Player;
import demo.chess.game.DummyGame;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

/**
 * Converts moves to and from standard PGN short algebraic notation (SAN).
 *
 * <p>SAN formatting is centralized here for live games, simulations and trusted
 * dummy replays. {@link DummyGame} deliberately skips the expensive king-safety
 * validation performed by normal players. Formatting therefore uses the dummy
 * state as a fast path and escalates to a normal {@link Simulation} only when a
 * SAN decision actually depends on full move legality, namely source
 * disambiguation conflicts and checkmate detection.</p>
 *
 * <p>SAN parsing keeps its dedicated {@link DummyGame} fast path because bulk
 * PGN imports intentionally resolve trusted historical moves without validating
 * every candidate through a complete simulation.</p>
 */
public final class PgnNotation {

    private PgnNotation() {
    }

    /**
     * Formats one move as SAN without changing the supplied game.
     *
     * <p>A replay copy is created because check and checkmate are properties of
     * the position after the move. Replay-oriented callers that already own a
     * disposable simulation should prefer {@link #toSanAndApply(Game, Move)} to
     * avoid rebuilding the current position for every move.</p>
     *
     * @param game position before the move
     * @param move move to format
     * @return standard SAN
     */
    public static String toSan(Game game, Move move) throws NoMoveFoundException, IOException {
        validateFormattingInput(game, move);
        Game replay = createReplayCopy(game);
        Move replayMove = replay.getPlayer().getMoveInSimulation(replay, move);
        if (replayMove == null) {
            throw new NoMoveFoundException("Could not map move to notation replay: " + move);
        }
        return toSanAndApply(replay, replayMove);
    }

    /**
     * Formats one move for the UI without changing the supplied game.
     *
     * @param game position before the move
     * @param move move to format
     * @return display notation with Unicode pieces and zero-based castling
     */
    public static String toDisplayNotation(Game game, Move move)
            throws NoMoveFoundException, IOException {
        return toDisplayNotation(move, toSan(game, move));
    }

    /**
     * Formats one move as SAN and applies it to the supplied replay game.
     *
     * <p>This method is intended for disposable simulations and trusted replay
     * contexts such as PGN export and engine principal variations. It performs
     * exactly one application of the supplied move.</p>
     *
     * @param game mutable replay position before the move
     * @param move move belonging to the supplied game
     * @return standard SAN
     */
    public static String toSanAndApply(Game game, Move move) throws NoMoveFoundException, IOException {
        validateFormattingInput(game, move);
        String baseSan = buildBaseSan(game, move);
        game.apply(move);
        return baseSan + checkSuffixAfterMove(game);
    }

    /**
     * Formats one move for the UI and applies it to the supplied replay game.
     *
     * @param game mutable replay position before the move
     * @param move move belonging to the supplied game
     * @return display notation with Unicode pieces and zero-based castling
     */
    public static String toDisplayNotationAndApply(Game game, Move move)
            throws NoMoveFoundException, IOException {
        validateFormattingInput(game, move);
        String san = toSanAndApply(game, move);
        return toDisplayNotation(move, san);
    }

    /**
     * Resolves SAN to one move in a trusted dummy replay.
     *
     * <p>This parser intentionally matches the SAN token against the cheaply
     * generated dummy move list instead of formatting every candidate back to
     * SAN. This is important for bulk PGN imports, where the source data is
     * replayed as trusted historical game data and full validation would be
     * prohibitively expensive.</p>
     *
     * @param game the trusted dummy replay
     * @param rawSan the raw SAN token
     * @return the matching move
     */
    public static Move resolveSan(DummyGame game, String rawSan) throws NoMoveFoundException, IOException {
        if (game == null) {
            throw new NoMoveFoundException("game must not be null");
        }

        String wanted = normalizeForComparison(rawSan);
        if (wanted.isEmpty()) {
            throw new NoMoveFoundException("SAN move must not be empty");
        }

        List<Move> moveCandidates = game.getPlayer().getValidMoves(game);

        if ("O-O".equals(wanted) || "O-O-O".equals(wanted)) {
            List<Move> matches = new ArrayList<>();
            boolean queenSide = "O-O-O".equals(wanted);
            for (Move candidate : moveCandidates) {
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
        for (Move candidate : moveCandidates) {
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

    private static void validateFormattingInput(Game game, Move move) throws NoMoveFoundException {
        if (game == null) {
            throw new NoMoveFoundException("game must not be null");
        }
        if (move == null || move.getPiece() == null || move.getSource() == null || move.getTarget() == null) {
            throw new NoMoveFoundException("move must not be null");
        }
    }

    private static Game createReplayCopy(Game game) throws NoMoveFoundException, IOException {
        if (game instanceof DummyGame) {
            return Simulation.forkDummyFrom(game.getMoveList());
        }
        return Simulation.forkSimulationFrom(game.getMoveList());
    }

    private static String buildBaseSan(Game game, Move move) throws NoMoveFoundException, IOException {
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

    private static String toDisplayNotation(Move move, String san) {
        String notation = san;

        if (move instanceof Castling) {
            return notation.replace('O', '0');
        }

        if (move.getPiece().getType() != PieceType.PAWN && !notation.isEmpty()) {
            notation = unicodePiece(move.getPiece().getType(), move.getPiece().getColor())
                    + notation.substring(1);
        }

        if (move instanceof EnPassant) {
            notation = insertBeforeCheckSuffix(notation, " e.p.");
        }

        return notation;
    }

    private static String insertBeforeCheckSuffix(String notation, String insertion) {
        if (notation.endsWith("+") || notation.endsWith("#")) {
            return notation.substring(0, notation.length() - 1)
                    + insertion
                    + notation.substring(notation.length() - 1);
        }
        return notation + insertion;
    }

    private static String checkSuffixAfterMove(Game game) throws NoMoveFoundException, IOException {
        Player checkedPlayer = game.getPlayer();
        if (checkedPlayer == null || checkedPlayer.getKing() == null || checkedPlayer.getKing().getField() == null) {
            return "";
        }

        Player attackingPlayer = checkedPlayer.getColor().equals(Color.WHITE)
                ? game.getBlackPlayer()
                : game.getWhitePlayer();

        boolean kingIsAttacked = attackingPlayer.getSimpleMoves().stream()
                .map(Move::getTarget)
                .filter(target -> target != null)
                .anyMatch(checkedPlayer.getKing().getField()::equals);
        if (!kingIsAttacked) {
            return "";
        }

        if (game instanceof DummyGame) {
            Simulation validationGame = Simulation.forkSimulationFrom(game.getMoveList());
            return validationGame.getPlayer().getValidMoves(validationGame).isEmpty() ? "#" : "+";
        }

        return checkedPlayer.getValidMoves(game).isEmpty() ? "#" : "+";
    }

    private static String sourceDisambiguation(Game game, Move move)
            throws NoMoveFoundException, IOException {
        List<Move> competingMoves = findCompetingMoves(game.getPlayer().getValidMoves(game), move);

        if (competingMoves.isEmpty()) {
            return "";
        }

        Move moveForDisambiguation = move;
        if (game instanceof DummyGame) {
            Simulation validationGame = Simulation.forkSimulationFrom(game.getMoveList());
            Move validationMove = validationGame.getPlayer().getMoveInSimulation(validationGame, move);
            if (validationMove == null) {
                throw new NoMoveFoundException("Could not map move to validation simulation: " + move);
            }
            competingMoves = findCompetingMoves(
                    validationGame.getPlayer().getValidMoves(validationGame),
                    validationMove);
            if (competingMoves.isEmpty()) {
                return "";
            }
            moveForDisambiguation = validationMove;
        }

        int sourceFile = moveForDisambiguation.getSource().getFile();
        int sourceRank = moveForDisambiguation.getSource().getRank();
        String sourceName = moveForDisambiguation.getSource().getName();

        boolean sameFileExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getFile() == sourceFile);
        boolean sameRankExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getRank() == sourceRank);

        if (sameFileExists && sameRankExists) {
            return sourceName;
        }
        if (sameFileExists) {
            return Integer.toString(sourceRank);
        }
        return sourceName.substring(0, 1);
    }

    private static List<Move> findCompetingMoves(List<Move> candidates, Move move) {
        List<Move> competingMoves = new ArrayList<>();
        if (candidates == null) {
            return competingMoves;
        }

        for (Move candidate : candidates) {
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
        return competingMoves;
    }

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

    private static boolean isSquare(String value) {
        return value != null
                && value.length() == 2
                && Character.toLowerCase(value.charAt(0)) >= 'a'
                && Character.toLowerCase(value.charAt(0)) <= 'h'
                && value.charAt(1) >= '1'
                && value.charAt(1) <= '8';
    }

    private static boolean isPieceLetter(char value) {
        return value == 'K'
                || value == 'Q'
                || value == 'R'
                || value == 'B'
                || value == 'N';
    }

    private static boolean isPromotionLetter(char value) {
        char normalized = Character.toUpperCase(value);
        return normalized == 'Q'
                || normalized == 'R'
                || normalized == 'B'
                || normalized == 'N';
    }

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

    private record SanDescriptor(
            PieceType pieceType,
            String targetSquare,
            boolean capture,
            Character sourceFile,
            Integer sourceRank,
            PieceType promotionType) {
    }
}
