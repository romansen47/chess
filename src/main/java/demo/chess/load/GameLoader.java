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

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.DummyGame;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.PgnNotation;

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

    /**
     * Replays a list of UCI moves on the supplied game. The move is always resolved
     * against the legal moves of the current position so castling, en passant and
     * promotion keep using the normal chess rules.
     */
    public void loadGame(List<String> uciMoves, Game game) throws IOException, NoMoveFoundException {
        if (game == null) {
            throw new NoMoveFoundException("game must not be null");
        }

        if (uciMoves == null) {
            return;
        }

        int ply = 0;
        for (String rawMove : uciMoves) {
            if (rawMove == null || rawMove.isBlank()) {
                continue;
            }

            ply++;
            String uciMove = rawMove.trim().toLowerCase(Locale.ROOT);
            if (!uciMove.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
                throw new NoMoveFoundException("Invalid UCI move at ply " + ply + ": " + rawMove);
            }

            Move finalMove = null;
            List<Move> moves = game.getPlayer().getValidMoves(game);
            for (Move move : moves) {
                if (move.toString().equalsIgnoreCase(uciMove)) {
                    finalMove = move;
                    break;
                }
            }

            if (finalMove == null) {
                throw new NoMoveFoundException("No legal UCI move at ply " + ply + ": " + rawMove);
            }

            game.apply(finalMove);
        }
    }

    /**
     * Parses either a plain whitespace-separated UCI move list or the UCI command
     * form "position startpos moves ...".
     */
    public List<String> parseMoveList(String content) throws NoMoveFoundException {
        List<String> moveList = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return moveList;
        }

        String normalizedContent = stripBom(content).trim();
        if (normalizedContent.isEmpty()) {
            return moveList;
        }

        String[] tokens = normalizedContent.split("\\s+");
        int startIndex = 0;

        if (tokens.length > 0 && "position".equalsIgnoreCase(tokens[0])) {
            if (tokens.length < 2 || !"startpos".equalsIgnoreCase(tokens[1])) {
                throw new NoMoveFoundException("Only UCI games starting from 'position startpos' are supported");
            }

            if (tokens.length == 2) {
                return moveList;
            }

            if (!"moves".equalsIgnoreCase(tokens[2])) {
                throw new NoMoveFoundException("Expected 'moves' after 'position startpos'");
            }
            startIndex = 3;
        } else if (tokens.length > 0 && "moves".equalsIgnoreCase(tokens[0])) {
            startIndex = 1;
        }

        for (int i = startIndex; i < tokens.length; i++) {
            String token = tokens[i].trim().toLowerCase(Locale.ROOT);
            if (token.isEmpty()) {
                continue;
            }
            if (!token.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
                throw new NoMoveFoundException("Invalid UCI token: " + tokens[i]);
            }
            moveList.add(token);
        }

        return moveList;
    }

    /**
     * Parses the first PGN game in the supplied content and returns its move list as
     * UCI coordinates. SAN-to-move conversion is performed exclusively on a
     * {@link DummyGame}.
     */
    public List<String> parsePgnMoveList(String content) throws NoMoveFoundException, IOException {
        List<String> moveList = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return moveList;
        }

        String movetext = stripBom(content);
        movetext = PGN_TAG_PATTERN.matcher(movetext).replaceAll(" ");
        movetext = PGN_BRACE_COMMENT_PATTERN.matcher(movetext).replaceAll(" ");
        movetext = PGN_LINE_COMMENT_PATTERN.matcher(movetext).replaceAll(" ");
        movetext = removeVariations(movetext);
        movetext = PGN_NAG_PATTERN.matcher(movetext).replaceAll(" ");

        DummyGame dummyGame = Simulation.createDummySimulation();
        int ply = 0;

        for (String rawToken : movetext.trim().split("\\s+")) {
            String token = stripMoveNumberPrefix(rawToken.trim());
            if (token.isEmpty() || "e.p.".equalsIgnoreCase(token) || "ep".equalsIgnoreCase(token)) {
                continue;
            }
            if (isResultToken(token)) {
                break;
            }

            ply++;
            try {
                Move move = PgnNotation.resolveSan(dummyGame, token);
                moveList.add(move.toString());
                dummyGame.apply(move);
            } catch (NoMoveFoundException e) {
                throw new NoMoveFoundException("Invalid PGN move at ply " + ply + ": " + token + " (" + e.getMessage() + ")");
            }
        }

        return moveList;
    }

    public Map<String, String> parsePgnTags(String content) {
        Map<String, String> tags = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return tags;
        }

        for (String line : stripBom(content).split("\\R", -1)) {
            if (line.isBlank()) {
                if (!tags.isEmpty()) {
                    break;
                }
                continue;
            }

            Matcher matcher = PGN_TAG_PATTERN.matcher(line);
            if (!matcher.matches()) {
                break;
            }
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

    private String stripBom(String content) {
        if (content != null && content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
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
        return "1-0".equals(token)
                || "0-1".equals(token)
                || "1/2-1/2".equals(token)
                || "*".equals(token);
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
                if (depth > 0) {
                    depth--;
                }
                continue;
            }
            if (depth == 0) {
                result.append(current);
            }
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
        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }
}
