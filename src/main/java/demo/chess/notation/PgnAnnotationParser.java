package demo.chess.notation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.DummyGame;
import demo.chess.game.impl.Simulation;

/**
 * Extracts comments, standard NAGs, eval tags and recursive variations that are
 * attached to main-line moves. The normal game loader remains responsible for
 * validating and loading the main line itself.
 */
public class PgnAnnotationParser {

    private static final Pattern TAG_LINE = Pattern.compile(
            "(?m)^\\s*\\[[A-Za-z0-9_]+\\s+\"(?:\\\\.|[^\"])*\"\\]\\s*$");
    private static final Pattern EVAL_TAG = Pattern.compile(
            "(?i)\\[%eval\\s+([^\\]]+)]");
    private static final Pattern MOVE_NUMBER_PREFIX = Pattern.compile("^\\d+\\.(?:\\.\\.)?");
    private static final Pattern SYMBOLIC_NAG_SUFFIX = Pattern.compile("(!!|\\?\\?|!\\?|\\?!|!|\\?)$");

    public Map<Integer, PgnMoveAnnotation> parse(String content)
            throws NoMoveFoundException, IOException {
        Map<Integer, MutableAnnotation> annotations = new LinkedHashMap<>();
        if (content == null || content.isBlank()) {
            return Map.of();
        }

        String movetext = TAG_LINE.matcher(stripBom(content)).replaceAll(" ");
        DummyGame game = Simulation.createDummySimulation();
        StringBuilder token = new StringBuilder();
        int ply = 0;

        for (int index = 0; index < movetext.length(); index++) {
            char current = movetext.charAt(index);

            if (Character.isWhitespace(current)) {
                ply = flushToken(token, game, annotations, ply);
                continue;
            }

            if (current == '{') {
                ply = flushToken(token, game, annotations, ply);
                int end = movetext.indexOf('}', index + 1);
                if (end < 0) {
                    end = movetext.length();
                }
                if (ply > 0) {
                    addComment(annotations, ply, movetext.substring(index + 1, end));
                }
                index = end;
                continue;
            }

            if (current == ';') {
                ply = flushToken(token, game, annotations, ply);
                int end = movetext.indexOf('\n', index + 1);
                if (end < 0) {
                    end = movetext.length();
                }
                if (ply > 0) {
                    addComment(annotations, ply, movetext.substring(index + 1, end));
                }
                index = end;
                continue;
            }

            if (current == '(') {
                ply = flushToken(token, game, annotations, ply);
                VariationCapture capture = captureVariation(movetext, index);
                if (ply > 0 && !capture.content().isBlank()) {
                    annotation(annotations, ply).variations.add(capture.content().trim());
                }
                index = capture.endIndex();
                continue;
            }

            token.append(current);
        }

        flushToken(token, game, annotations, ply);

        Map<Integer, PgnMoveAnnotation> result = new LinkedHashMap<>();
        annotations.forEach((key, value) -> {
            PgnMoveAnnotation annotation = value.toImmutable();
            if (!annotation.isEmpty()) {
                result.put(key, annotation);
            }
        });
        return Map.copyOf(result);
    }

    private int flushToken(
            StringBuilder token,
            DummyGame game,
            Map<Integer, MutableAnnotation> annotations,
            int currentPly) throws NoMoveFoundException, IOException {
        if (token.length() == 0) {
            return currentPly;
        }

        String raw = token.toString().trim();
        token.setLength(0);
        if (raw.isEmpty()) {
            return currentPly;
        }

        String value = stripMoveNumberPrefix(raw);
        if (value.isEmpty()
                || "e.p.".equalsIgnoreCase(value)
                || "ep".equalsIgnoreCase(value)
                || isResultToken(value)) {
            return currentPly;
        }

        if (value.startsWith("$")) {
            String nag = nagFromNumber(value.substring(1));
            if (nag != null && currentPly > 0) {
                annotation(annotations, currentPly).nag = nag;
            }
            return currentPly;
        }

        if (isSymbolicNag(value)) {
            if (currentPly > 0) {
                annotation(annotations, currentPly).nag = value;
            }
            return currentPly;
        }

        Matcher suffixMatcher = SYMBOLIC_NAG_SUFFIX.matcher(value);
        String nag = suffixMatcher.find() ? suffixMatcher.group(1) : null;

        Move move = PgnNotation.resolveSan(game, value);
        game.apply(move);
        int nextPly = currentPly + 1;
        if (nag != null) {
            annotation(annotations, nextPly).nag = nag;
        }
        return nextPly;
    }

    private void addComment(
            Map<Integer, MutableAnnotation> annotations,
            int ply,
            String rawComment) {
        if (rawComment == null) {
            return;
        }

        MutableAnnotation annotation = annotation(annotations, ply);
        Matcher matcher = EVAL_TAG.matcher(rawComment);
        StringBuffer humanText = new StringBuffer();

        while (matcher.find()) {
            String evaluation = matcher.group(1) == null ? null : matcher.group(1).trim();
            if (evaluation != null && !evaluation.isEmpty()) {
                annotation.evaluation = evaluation;
            }
            matcher.appendReplacement(humanText, " ");
        }
        matcher.appendTail(humanText);

        String comment = humanText.toString().trim();
        if (!comment.isEmpty()) {
            annotation.comment = annotation.comment == null
                    ? comment
                    : annotation.comment + System.lineSeparator() + comment;
        }
    }

    private VariationCapture captureVariation(String text, int startIndex) {
        int depth = 0;
        boolean inBraceComment = false;
        for (int index = startIndex; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inBraceComment) {
                if (current == '}') {
                    inBraceComment = false;
                }
                continue;
            }
            if (current == '{') {
                inBraceComment = true;
                continue;
            }
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')') {
                depth--;
                if (depth == 0) {
                    return new VariationCapture(
                            text.substring(startIndex + 1, index),
                            index);
                }
            }
        }
        return new VariationCapture(
                text.substring(Math.min(startIndex + 1, text.length())),
                text.length() - 1);
    }

    private MutableAnnotation annotation(
            Map<Integer, MutableAnnotation> annotations,
            int ply) {
        return annotations.computeIfAbsent(ply, ignored -> new MutableAnnotation());
    }

    private String stripMoveNumberPrefix(String raw) {
        String result = raw;
        while (true) {
            Matcher matcher = MOVE_NUMBER_PREFIX.matcher(result);
            if (!matcher.find()) {
                return result;
            }
            result = result.substring(matcher.end());
        }
    }

    private boolean isResultToken(String token) {
        return "1-0".equals(token)
                || "0-1".equals(token)
                || "1/2-1/2".equals(token)
                || "*".equals(token);
    }

    private boolean isSymbolicNag(String value) {
        return "!".equals(value)
                || "!!".equals(value)
                || "!?".equals(value)
                || "?!".equals(value)
                || "?".equals(value)
                || "??".equals(value);
    }

    private String nagFromNumber(String value) {
        return switch (value) {
            case "1" -> "!";
            case "2" -> "?";
            case "3" -> "!!";
            case "4" -> "??";
            case "5" -> "!?";
            case "6" -> "?!";
            default -> null;
        };
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static final class MutableAnnotation {
        private String nag;
        private String comment;
        private String evaluation;
        private final List<String> variations = new ArrayList<>();

        private PgnMoveAnnotation toImmutable() {
            return new PgnMoveAnnotation(nag, comment, evaluation, variations);
        }
    }

    private record VariationCapture(String content, int endIndex) {
    }
}
