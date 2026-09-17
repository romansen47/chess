package demo.chess.load;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.DummyGame;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.PgnNotation;
import demo.chess.notation.UciMoveCodec;

public class GameLoader {

    private static final Pattern PGN_TAG_PATTERN = Pattern.compile(
            "(?m)^\\s*\\[([A-Za-z0-9_]+)\\s+\"((?:\\\\.|[^\"])*)\"\\]\\s*$");
    private static final Pattern PGN_BRACE_COMMENT_PATTERN = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);
    private static final Pattern PGN_LINE_COMMENT_PATTERN = Pattern.compile("(?m);[^\\r\\n]*$");
    private static final Pattern PGN_NAG_PATTERN = Pattern.compile("\\$\\d+");
    private static final Pattern MOVE_NUMBER_PREFIX_PATTERN = Pattern.compile("^\\d+\\.(?:\\.\\.)?");

    public void loadGame(String location, Game game) throws IOException, NoMoveFoundException {
        String content = Files.readString(Path.of(location), StandardCharsets.UTF_8);
        loadGame(parseMoveList(content), game);
    }

    public void loadGame(List<String> uciMoves, Game game) throws IOException, NoMoveFoundException {
        if (game == null) throw new NoMoveFoundException("game must not be null");
        if (uciMoves == null) return;

        int ply = 0;
        for (String rawMove : uciMoves) {
            if (rawMove == null || rawMove.isBlank()) continue;
            ply++;
            String uciMove = rawMove.trim().toLowerCase(Locale.ROOT);
            if (!uciMove.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
                throw new NoMoveFoundException("Invalid UCI move at ply " + ply + ": " + rawMove);
            }
            try {
                game.apply(LegalMoveResolver.resolveUci(game, uciMove));
            } catch (NoMoveFoundException e) {
                throw new NoMoveFoundException("No legal UCI move at ply " + ply + ": " + rawMove);
            }
        }
    }

    public List<String> parseMoveList(String content) throws NoMoveFoundException {
        List<String> moveList = new ArrayList<>();
        if (content == null || content.isBlank()) return moveList;
        String normalizedContent = stripBom(content).trim();
        if (normalizedContent.isEmpty()) return moveList;

        String[] tokens = normalizedContent.split("\\s+");
        int startIndex = 0;
        if (tokens.length > 0 && "position".equalsIgnoreCase(tokens[0])) {
            if (tokens.length < 2 || !"startpos".equalsIgnoreCase(tokens[1])) {
                throw new NoMoveFoundException(
                        "Raw UCI import currently requires 'position startpos'; use PGN/FEN for Chess960");
            }
            if (tokens.length == 2) return moveList;
            if (!"moves".equalsIgnoreCase(tokens[2])) {
                throw new NoMoveFoundException("Expected 'moves' after 'position startpos'");
            }
            startIndex = 3;
        } else if (tokens.length > 0 && "moves".equalsIgnoreCase(tokens[0])) {
            startIndex = 1;
        }

        for (int i = startIndex; i < tokens.length; i++) {
            String token = tokens[i].trim().toLowerCase(Locale.ROOT);
            if (token.isEmpty()) continue;
            if (!token.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
                throw new NoMoveFoundException("Invalid UCI token: " + tokens[i]);
            }
            moveList.add(token);
        }
        return moveList;
    }

    public List<String> splitPgnGames(String content) {
        List<String> games = new ArrayList<>();
        if (content == null || content.isBlank()) return games;

        String normalized = stripBom(content).replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder current = new StringBuilder();
        boolean currentFinished = false;
        for (String line : normalized.split("\n", -1)) {
            boolean tagLine = PGN_TAG_PATTERN.matcher(line).matches();
            boolean currentHasMovetext = containsPgnMovetext(current.toString());
            if (tagLine && currentHasMovetext) {
                addPgnGame(games, current);
                currentFinished = false;
            } else if (currentFinished && isMeaningfulPgnMovetext(line)) {
                addPgnGame(games, current);
                currentFinished = false;
            }
            current.append(line).append('\n');
            currentFinished = endsWithPgnResult(current.toString());
        }
        addPgnGame(games, current);
        return games;
    }

    /** Resolves the Chess960 start position encoded by PGN tags. */
    public ChessStartingPosition parsePgnStartingPosition(String content) throws NoMoveFoundException {
        Map<String, String> tags = parsePgnTags(content);
        String fen = tags.get("FEN");
        String variant = tags.get("Variant");
        boolean chess960 = variant != null
                && (variant.equalsIgnoreCase("Chess960")
                        || variant.equalsIgnoreCase("FischerRandom")
                        || variant.equalsIgnoreCase("Fischer Random"));

        if (fen != null && !fen.isBlank()) {
            try {
                return ChessStartingPosition.fromInitialFen(fen);
            } catch (IllegalArgumentException e) {
                throw new NoMoveFoundException("Unsupported PGN initial FEN: " + e.getMessage());
            }
        }
        if (chess960) {
            throw new NoMoveFoundException("Chess960 PGN requires a FEN tag for the initial position");
        }
        return ChessStartingPosition.STANDARD;
    }

    public List<String> parsePgnMoveList(String content) throws NoMoveFoundException, IOException {
        List<String> moveList = new ArrayList<>();
        if (content == null || content.isBlank()) return moveList;

        ChessStartingPosition startingPosition = parsePgnStartingPosition(content);
        String movetext = sanitizePgnMovetext(stripBom(content));
        DummyGame dummyGame = Simulation.createDummySimulation(startingPosition);
        int ply = 0;

        for (String rawToken : movetext.trim().split("\\s+")) {
            String token = stripMoveNumberPrefix(rawToken.trim());
            if (token.isEmpty() || "e.p.".equalsIgnoreCase(token) || "ep".equalsIgnoreCase(token)) continue;
            if (isResultToken(token)) break;
            ply++;
            try {
                Move move = PgnNotation.resolveSan(dummyGame, token);
                moveList.add(UciMoveCodec.encode(dummyGame, move));
                dummyGame.apply(move);
            } catch (NoMoveFoundException e) {
                throw new NoMoveFoundException(
                        "Invalid PGN move at ply " + ply + ": " + token + " (" + e.getMessage() + ")");
            }
        }
        return moveList;
    }

    public Map<String, String> parsePgnTags(String content) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (content == null || content.isBlank()) return tags;
        for (String line : stripBom(content).split("\\R", -1)) {
            if (line.isBlank()) {
                if (!tags.isEmpty()) break;
                continue;
            }
            Matcher matcher = PGN_TAG_PATTERN.matcher(line);
            if (!matcher.matches()) break;
            tags.put(matcher.group(1), unescapePgnTagValue(matcher.group(2)));
        }
        return tags;
    }

    public List<String> loadMoveList(String location) throws IOException {
        String content = Files.readString(Path.of(location), StandardCharsets.UTF_8);
        try {
            return parseMoveList(content);
        } catch (NoMoveFoundException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private void addPgnGame(List<String> games, StringBuilder current) {
        if (!containsPgnMovetext(current.toString())) {
            current.setLength(0);
            return;
        }
        String game = current.toString().trim();
        if (!game.isEmpty()) games.add(game);
        current.setLength(0);
    }

    private boolean containsPgnMovetext(String value) {
        String movetext = sanitizePgnMovetext(value).trim();
        if (movetext.isEmpty()) return false;
        for (String rawToken : movetext.split("\\s+")) {
            String token = stripMoveNumberPrefix(rawToken.trim());
            if (token.isEmpty() || "e.p.".equalsIgnoreCase(token) || "ep".equalsIgnoreCase(token)
                    || isResultToken(token)) continue;
            return true;
        }
        return false;
    }

    private boolean isMeaningfulPgnMovetext(String line) {
        return containsPgnMovetext(line);
    }

    private boolean endsWithPgnResult(String value) {
        String movetext = sanitizePgnMovetext(value).trim();
        if (movetext.isEmpty()) return false;
        String[] tokens = movetext.split("\\s+");
        for (int index = tokens.length - 1; index >= 0; index--) {
            String token = stripMoveNumberPrefix(tokens[index].trim());
            if (token.isEmpty() || "e.p.".equalsIgnoreCase(token) || "ep".equalsIgnoreCase(token)) continue;
            return isResultToken(token);
        }
        return false;
    }

    private String sanitizePgnMovetext(String content) {
        String movetext = content == null ? "" : content;
        movetext = PGN_TAG_PATTERN.matcher(movetext).replaceAll(" ");
        movetext = PGN_BRACE_COMMENT_PATTERN.matcher(movetext).replaceAll(" ");
        movetext = PGN_LINE_COMMENT_PATTERN.matcher(movetext).replaceAll(" ");
        movetext = removeVariations(movetext);
        return PGN_NAG_PATTERN.matcher(movetext).replaceAll(" ");
    }

    private String stripBom(String content) {
        return content != null && content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    private String stripMoveNumberPrefix(String token) {
        String result = token;
        Matcher matcher = MOVE_NUMBER_PREFIX_PATTERN.matcher(result);
        while (matcher.find()) {
            result = result.substring(matcher.end());
            matcher = MOVE_NUMBER_PREFIX_PATTERN.matcher(result);
        }
        return result;
    }

    private boolean isResultToken(String token) {
        return "1-0".equals(token) || "0-1".equals(token) || "1/2-1/2".equals(token) || "*".equals(token);
    }

    private String removeVariations(String value) {
        StringBuilder result = new StringBuilder(value.length());
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')') {
                if (depth > 0) depth--;
                continue;
            }
            if (depth == 0) result.append(current);
        }
        return result.toString();
    }

    private String unescapePgnTagValue(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaped) {
                result.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else {
                result.append(current);
            }
        }
        if (escaped) result.append('\\');
        return result.toString();
    }
}
