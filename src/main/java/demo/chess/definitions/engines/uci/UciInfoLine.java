package demo.chess.definitions.engines.uci;

/**
 * Parsed principal-variation snapshot from one UCI {@code info} line.
 *
 * <p>Exactly one of {@code centipawnScore} and {@code mateScore} is present.
 * The score is still from the engine's side-to-move perspective; callers that
 * expose white-normalized values apply the color factor afterwards.</p>
 */
public record UciInfoLine(
        int depth,
        int multiPv,
        Integer centipawnScore,
        Integer mateScore,
        String pv) {

    public UciInfoLine {
        if (depth < 0) throw new IllegalArgumentException("depth must not be negative");
        if (multiPv < 1) throw new IllegalArgumentException("multiPv must be at least 1");
        if (pv == null || pv.isBlank()) throw new IllegalArgumentException("pv must not be blank");
        if ((centipawnScore == null) == (mateScore == null)) {
            throw new IllegalArgumentException("Exactly one UCI score must be present");
        }
        pv = pv.trim();
    }

    public boolean isMateScore() {
        return mateScore != null;
    }
}
