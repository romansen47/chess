package demo.chess.notation;

import java.util.ArrayList;
import java.util.List;

/**
 * User-visible PGN data attached to one main-line half-move.
 *
 * @param nag symbolic NAG such as !, !!, !?, ?!, ? or ??
 * @param comment free user comment without surrounding braces
 * @param evaluation optional PGN eval payload without the [%eval ...] wrapper
 * @param variations raw PGN recursive-annotation variations without outer parentheses
 * @param clockMillis remaining time after this move, in milliseconds; null if unknown
 * @param elapsedMoveMillis time spent on this move, in milliseconds; null if unknown
 */
public record PgnMoveAnnotation(
        String nag,
        String comment,
        String evaluation,
        List<String> variations,
        Long clockMillis,
        Long elapsedMoveMillis) {

    public PgnMoveAnnotation(String nag, String comment, String evaluation, List<String> variations) {
        this(nag, comment, evaluation, variations, null, null);
    }

    public PgnMoveAnnotation {
        if (clockMillis != null && clockMillis < 0 || elapsedMoveMillis != null && elapsedMoveMillis < 0) {
            throw new IllegalArgumentException("PGN times must not be negative");
        }
        nag = normalizeNag(nag);
        comment = normalizeText(comment);
        evaluation = normalizeText(evaluation);

        List<String> safeVariations = new ArrayList<>();
        if (variations != null) {
            for (String variation : variations) {
                String normalized = normalizeText(variation);
                if (normalized != null) {
                    safeVariations.add(normalized);
                }
            }
        }
        variations = List.copyOf(safeVariations);
    }

    public boolean isEmpty() {
        return nag == null
                && comment == null
                && evaluation == null
                && variations.isEmpty()
                && clockMillis == null
                && elapsedMoveMillis == null;
    }

    private static String normalizeNag(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "!", "!!", "!?", "?!", "?", "??" -> normalized;
            default -> null;
        };
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
