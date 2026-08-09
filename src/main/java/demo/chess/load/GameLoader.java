package demo.chess.load;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

public class GameLoader {

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

        String normalizedContent = content.trim();
        if (normalizedContent.startsWith("\uFEFF")) {
            normalizedContent = normalizedContent.substring(1).trim();
        }
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

    public List<String> loadMoveList(String location) throws IOException {
        String content = Files.readString(Path.of(location), StandardCharsets.UTF_8);
        try {
            return parseMoveList(content);
        } catch (NoMoveFoundException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
