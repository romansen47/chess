package demo.chess.save;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.DummyGame;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.PgnMoveAnnotation;
import demo.chess.notation.PgnNotation;
import demo.chess.notation.UciMoveCodec;

/**
 * Serializes game histories into lightweight UCI move lists or PGN.
 *
 * <p>PGN is the self-describing persistence format and therefore carries the
 * Chess960 setup tags and FEN for every Scharnagl position, including 518. The
 * UCI export is intentionally only a move list;
 * consumers must already know the initial position. Move encoding itself is
 * still variant-aware through {@link UciMoveCodec}.</p>
 */
public class GameSaver {

    private static final DateTimeFormatter PGN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** Writes a variant-aware UCI move list to a UTF-8 file. */
    public void saveGame(MoveList moveList, String location) throws IOException {
        Files.writeString(Path.of(location), toUci(moveList), StandardCharsets.UTF_8);
    }

    /**
     * Serializes moves as one UCI move per line.
     *
     * <p>If {@code moveList} is a {@link MoveList}, its starting-position metadata
     * is used so Chess960 castling is encoded as king-source to rook-source.
     * For a generic iterable, classical chess is assumed.</p>
     */
    public String toUci(Iterable<Move> moveList) {
        if (moveList == null) return "";
        ChessStartingPosition startingPosition = startingPositionOf(moveList);
        StringBuilder result = new StringBuilder();
        for (Move move : moveList) {
            if (move != null) {
                result.append(UciMoveCodec.encode(startingPosition, move)).append('\n');
            }
        }
        return result.toString();
    }

    public String toPgn(Iterable<Move> moveList, Map<String, String> suppliedTags)
            throws NoMoveFoundException, IOException {
        return toPgn(moveList, suppliedTags, Map.of());
    }

    /**
     * Serializes a move history as a self-describing Chess960 PGN.
     *
     * <p>Variant, setup and FEN tags are written for all 960 positions. This
     * intentionally includes position 518 so files produced by this application
     * never depend on an implicit starting-position convention.</p>
     */
    public String toPgn(
            Iterable<Move> moveList,
            Map<String, String> suppliedTags,
            Map<Integer, PgnMoveAnnotation> annotations)
            throws NoMoveFoundException, IOException {
        ChessStartingPosition startingPosition = startingPositionOf(moveList);
        Map<String, String> tags = createTags(suppliedTags);
        tags.put("Variant", "Chess960");
        tags.put("SetUp", "1");
        tags.put("FEN", startingPosition.initialFen());
        String resultToken = normalizeResult(tags.get("Result"));
        tags.put("Result", resultToken);

        StringBuilder pgn = new StringBuilder();
        appendTags(pgn, tags);
        pgn.append('\n');

        DummyGame dummyGame = Simulation.createDummySimulation(startingPosition);
        int ply = 0;
        if (moveList != null) {
            for (Move originalMove : moveList) {
                if (originalMove == null) continue;
                Move move = dummyGame.getPlayer().getMoveInSimulation(dummyGame, originalMove);
                if (move == null) {
                    throw new NoMoveFoundException("Could not map move to dummy game: " + originalMove);
                }
                if (ply % 2 == 0) {
                    if (ply > 0) pgn.append(' ');
                    pgn.append((ply / 2) + 1).append(". ");
                } else {
                    pgn.append(' ');
                }
                pgn.append(PgnNotation.toSanAndApply(dummyGame, move));
                ply++;
                appendAnnotation(pgn, annotations != null ? annotations.get(ply) : null);
            }
        }
        if (ply > 0) pgn.append(' ');
        pgn.append(resultToken).append('\n');
        return pgn.toString();
    }

    private ChessStartingPosition startingPositionOf(Iterable<Move> moveList) {
        if (moveList instanceof MoveList typed && typed.getStartingPosition() != null) {
            return typed.getStartingPosition();
        }
        return ChessStartingPosition.STANDARD;
    }

    private void appendAnnotation(StringBuilder pgn, PgnMoveAnnotation annotation) {
        if (annotation == null || annotation.isEmpty()) return;
        Integer nagNumber = nagNumber(annotation.nag());
        if (nagNumber != null) pgn.append(" $").append(nagNumber);

        String comment = annotation.comment();
        String evaluation = annotation.evaluation();
        if (comment != null || evaluation != null) {
            pgn.append(" {");
            if (comment != null) pgn.append(sanitizeComment(comment));
            if (evaluation != null) {
                if (comment != null) pgn.append(' ');
                pgn.append("[%eval ").append(evaluation).append(']');
            }
            pgn.append('}');
        }
        for (String variation : annotation.variations()) {
            if (variation != null && !variation.isBlank()) pgn.append(" (").append(variation.trim()).append(')');
        }
    }

    private Integer nagNumber(String symbol) {
        if (symbol == null) return null;
        return switch (symbol) {
            case "!" -> 1;
            case "?" -> 2;
            case "!!" -> 3;
            case "??" -> 4;
            case "!?" -> 5;
            case "?!" -> 6;
            default -> null;
        };
    }

    private String sanitizeComment(String comment) {
        return comment.replace('{', '[').replace('}', ']');
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
                if (key != null && !key.isBlank() && value != null) tags.put(key, value);
            });
        }
        return tags;
    }

    private void appendTags(StringBuilder pgn, Map<String, String> tags) {
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            pgn.append('[').append(entry.getKey()).append(" \"")
                    .append(escapePgnTagValue(entry.getValue())).append("\"]\n");
        }
    }

    private String normalizeResult(String result) {
        if ("1-0".equals(result) || "0-1".equals(result) || "1/2-1/2".equals(result)) return result;
        return "*";
    }

    private String escapePgnTagValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
