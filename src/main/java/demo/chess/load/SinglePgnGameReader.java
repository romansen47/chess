package demo.chess.load;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads one PGN from a character stream and stops as soon as a second game is
 * detected.
 *
 * <p>The accepted single-game payload is preserved exactly. For multi-game
 * sources the reader intentionally returns immediately at the first reliable
 * game boundary instead of consuming the complete source.</p>
 */
public final class SinglePgnGameReader {

    private static final int BUFFER_SIZE = 8192;

    private static final Pattern PGN_TAG_PATTERN = Pattern.compile(
            "^\\s*\\[([A-Za-z0-9_]+)\\s+\"((?:\\\\.|[^\"])*)\"\\]\\s*$");
    private static final Pattern PGN_NAG_PATTERN = Pattern.compile("\\$\\d+");
    private static final Pattern MOVE_NUMBER_PREFIX_PATTERN = Pattern.compile("^\\d+\\.(?:\\.\\.)?");

    /**
     * Result of a streaming single-game probe.
     *
     * @param content exact PGN source when exactly one game was found; otherwise null
     * @param gameCount 0 for no game, 1 for exactly one game, 2 when a second game was detected
     * @param earlyAbort true when reading stopped immediately after detecting a second game
     */
    public record Result(String content, int gameCount, boolean earlyAbort) {
    }

    /**
     * Reads at most one complete PGN game.
     *
     * @param reader source reader
     * @return probe result
     * @throws IOException when the source cannot be read
     */
    public Result read(Reader reader) throws IOException {
        if (reader == null) {
            return new Result(null, 0, false);
        }

        StringBuilder content = new StringBuilder();
        StringBuilder line = new StringBuilder();
        StreamState state = new StreamState();
        char[] buffer = new char[BUFFER_SIZE];
        boolean firstLine = true;
        boolean previousWasCarriageReturn = false;

        int read;
        while ((read = reader.read(buffer, 0, buffer.length)) >= 0) {
            if (read == 0) {
                continue;
            }

            for (int index = 0; index < read; index++) {
                char current = buffer[index];
                content.append(current);

                if (current == '\r') {
                    if (startsSecondGame(line.toString(), state, firstLine)) {
                        return new Result(null, 2, true);
                    }
                    firstLine = false;
                    line.setLength(0);
                    previousWasCarriageReturn = true;
                    continue;
                }

                if (current == '\n') {
                    if (previousWasCarriageReturn) {
                        previousWasCarriageReturn = false;
                        continue;
                    }

                    if (startsSecondGame(line.toString(), state, firstLine)) {
                        return new Result(null, 2, true);
                    }
                    firstLine = false;
                    line.setLength(0);
                    continue;
                }

                previousWasCarriageReturn = false;
                line.append(current);
            }
        }

        if (line.length() > 0 && startsSecondGame(line.toString(), state, firstLine)) {
            return new Result(null, 2, true);
        }

        if (!state.hasMovetext) {
            return new Result(null, 0, false);
        }

        return new Result(content.toString(), 1, false);
    }

    private boolean startsSecondGame(String rawLine, StreamState state, boolean firstLine) {
        String line = firstLine ? stripBom(rawLine) : rawLine;

        boolean tagLine = !state.inBraceComment
                && state.variationDepth == 0
                && PGN_TAG_PATTERN.matcher(line).matches();
        if (tagLine) {
            return state.hasMovetext;
        }

        boolean previousGameFinished = state.lastMainlineTokenIsResult;
        boolean meaningfulMovetext = scanMainlineMovetext(line, state);

        if (previousGameFinished && meaningfulMovetext) {
            return true;
        }

        if (meaningfulMovetext) {
            state.hasMovetext = true;
        }
        return false;
    }

    private boolean scanMainlineMovetext(String line, StreamState state) {
        StringBuilder mainline = new StringBuilder(line.length());

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);

            if (state.inBraceComment) {
                if (current == '}') {
                    state.inBraceComment = false;
                    mainline.append(' ');
                }
                continue;
            }

            if (current == ';') {
                break;
            }

            if (current == '{') {
                state.inBraceComment = true;
                mainline.append(' ');
                continue;
            }

            if (current == '(') {
                state.variationDepth++;
                mainline.append(' ');
                continue;
            }

            if (current == ')') {
                if (state.variationDepth > 0) {
                    state.variationDepth--;
                }
                mainline.append(' ');
                continue;
            }

            if (state.variationDepth == 0) {
                mainline.append(current);
            }
        }

        String sanitized = PGN_NAG_PATTERN.matcher(mainline).replaceAll(" ").trim();
        if (sanitized.isEmpty()) {
            return false;
        }

        boolean meaningfulMovetext = false;
        for (String rawToken : sanitized.split("\\s+")) {
            String token = stripMoveNumberPrefix(rawToken.trim());
            if (token.isEmpty()
                    || "e.p.".equalsIgnoreCase(token)
                    || "ep".equalsIgnoreCase(token)) {
                continue;
            }

            if (isResultToken(token)) {
                state.lastMainlineTokenIsResult = true;
                continue;
            }

            meaningfulMovetext = true;
            state.lastMainlineTokenIsResult = false;
        }
        return meaningfulMovetext;
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

    private String stripBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private static final class StreamState {
        private boolean inBraceComment;
        private int variationDepth;
        private boolean hasMovetext;
        private boolean lastMainlineTokenIsResult;
    }
}
