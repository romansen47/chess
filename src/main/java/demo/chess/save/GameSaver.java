package demo.chess.save;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.DummyGame;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.PgnNotation;

public class GameSaver {

    private static final DateTimeFormatter PGN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public void saveGame(MoveList moveList, String location) throws IOException {
        Files.writeString(Path.of(location), toUci(moveList), StandardCharsets.UTF_8);
    }

    /**
     * Serializes a game as a plain UCI move list, one move per line.
     */
    public String toUci(Iterable<Move> moveList) {
        if (moveList == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (Move move : moveList) {
            if (move == null) {
                continue;
            }
            result.append(move.toString()).append('\n');
        }
        return result.toString();
    }

    /**
     * Serializes a game as PGN. Move notation is generated exclusively by replaying
     * the supplied moves on a {@link DummyGame}.
     */
    public String toPgn(Iterable<Move> moveList, Map<String, String> suppliedTags)
            throws NoMoveFoundException, IOException {
        Map<String, String> tags = createTags(suppliedTags);
        String resultToken = normalizeResult(tags.get("Result"));
        tags.put("Result", resultToken);

        StringBuilder pgn = new StringBuilder();
        appendTags(pgn, tags);
        pgn.append('\n');

        DummyGame dummyGame = Simulation.createDummySimulation();
        int ply = 0;

        if (moveList != null) {
            for (Move originalMove : moveList) {
                if (originalMove == null) {
                    continue;
                }

                Move move = dummyGame.getPlayer().getMoveInSimulation(dummyGame, originalMove);
                if (move == null) {
                    throw new NoMoveFoundException("Could not map move to dummy game: " + originalMove);
                }

                if (ply % 2 == 0) {
                    if (ply > 0) {
                        pgn.append(' ');
                    }
                    pgn.append((ply / 2) + 1).append(". ");
                } else {
                    pgn.append(' ');
                }

                pgn.append(PgnNotation.toSan(dummyGame, move));
                dummyGame.apply(move);
                ply++;
            }
        }

        if (ply > 0) {
            pgn.append(' ');
        }
        pgn.append(resultToken).append('\n');
        return pgn.toString();
    }

    private Map<String, String> createTags(Map<String, String> suppliedTags) {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Event", "?");
        tags.put("Site", "?");
        tags.put("Date", LocalDate.now().format(PGN_DATE_FORMAT));
        tags.put("Round", "-");
        tags.put("White", "White");
        tags.put("Black", "Black");
        tags.put("Result", "*");

        if (suppliedTags != null) {
            suppliedTags.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    tags.put(key, value);
                }
            });
        }

        return tags;
    }

    private void appendTags(StringBuilder pgn, Map<String, String> tags) {
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            pgn.append('[')
                    .append(entry.getKey())
                    .append(" \"")
                    .append(escapePgnTagValue(entry.getValue()))
                    .append("\"]\n");
        }
    }

    private String normalizeResult(String result) {
        if ("1-0".equals(result) || "0-1".equals(result) || "1/2-1/2".equals(result)) {
            return result;
        }
        return "*";
    }

    private String escapePgnTagValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
