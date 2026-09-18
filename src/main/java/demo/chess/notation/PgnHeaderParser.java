package demo.chess.notation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

/**
 * Parses the self-describing header context of one PGN game.
 *
 * <p>Chess960 consumers must not infer an initial board from movetext alone.
 * This parser centralizes PGN tag extraction and resolves the initial
 * {@link ChessStartingPosition} from the standard {@code Variant} and
 * {@code FEN} tags. Classical chess remains Scharnagl position 518.</p>
 */
public final class PgnHeaderParser {

    private static final Pattern TAG_PATTERN = Pattern.compile(
            "(?m)^\\s*\\[([A-Za-z0-9_]+)\\s+\"((?:\\\\.|[^\"])*)\"\\]\\s*$");

    private PgnHeaderParser() {
    }

    /**
     * Parses the initial contiguous PGN tag section.
     *
     * @param content complete PGN content
     * @return tags in source order
     */
    public static Map<String, String> parseTags(String content) {
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

            Matcher matcher = TAG_PATTERN.matcher(line);
            if (!matcher.matches()) {
                break;
            }
            tags.put(matcher.group(1), unescapeTagValue(matcher.group(2)));
        }
        return tags;
    }

    /**
     * Resolves the initial chess position encoded by the PGN header.
     *
     * <p>A FEN tag is authoritative whenever present. A Chess960 variant
     * without FEN is rejected because its start position cannot be recovered
     * from SAN movetext.</p>
     *
     * @param content complete PGN content
     * @return resolved starting position
     * @throws NoMoveFoundException if a Chess960 header is incomplete or its
     *         FEN is not a supported initial position
     */
    public static ChessStartingPosition resolveStartingPosition(String content)
            throws NoMoveFoundException {
        Map<String, String> tags = parseTags(content);
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
                throw new NoMoveFoundException(
                        "Unsupported PGN initial FEN: " + e.getMessage());
            }
        }

        if (chess960) {
            throw new NoMoveFoundException(
                    "Chess960 PGN requires a FEN tag for the initial position");
        }
        return ChessStartingPosition.STANDARD;
    }

    private static String stripBom(String content) {
        return content != null && content.startsWith("\uFEFF")
                ? content.substring(1)
                : content;
    }

    private static String unescapeTagValue(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
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
