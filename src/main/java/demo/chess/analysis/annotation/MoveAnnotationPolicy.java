package demo.chess.analysis.annotation;

/**
 * Central tuning policy for move annotations derived from finite engine
 * analysis.
 *
 * <p>The policy deliberately keeps the "!!" model small. An extraordinary
 * move is either backed by a real material sacrifice or by a simple deep-
 * discovery signal. More elaborate phase/rank heuristics belong in
 * diagnostics, not in the classification policy.</p>
 */
public final class MoveAnnotationPolicy {

    public static final double ONLY_MOVE_FINAL_WIN_PERCENT_GAP = 15.0;
    public static final double ONLY_MOVE_TRIVIAL_EARLY_START_RATIO = 0.30;
    public static final double ONLY_MOVE_TRIVIAL_EARLY_END_RATIO = 0.50;
    public static final double ONLY_MOVE_TRIVIAL_WIN_PERCENT_GAP = 10.0;
    public static final double ONLY_MOVE_TRIVIAL_SNAPSHOT_RATIO = 0.70;
    public static final int ONLY_MOVE_TRIVIAL_MIN_SNAPSHOTS = 2;

    /*
     * Extraordinary moves must still be objectively close to the engine's
     * best choice.
     */
    public static final int EXTRAORDINARY_MAX_FINAL_RANK = 3;
    public static final double EXTRAORDINARY_MAX_FINAL_REGRET_WIN_PERCENT = 10.0;

    /*
     * Deep discovery is intentionally simple and engine-relative:
     * - look at an early search window relative to the engine's own final depth;
     * - require the move to have been measurably inferior/uncertain there;
     * - require the move itself to gain substantial practical strength by the
     *   final search.
     */
    public static final double EXTRAORDINARY_DISCOVERY_EARLY_START_RATIO = 0.25;
    public static final double EXTRAORDINARY_DISCOVERY_EARLY_END_RATIO = 0.40;
    public static final int EXTRAORDINARY_DISCOVERY_MIN_EARLY_SNAPSHOTS = 2;
    public static final double EXTRAORDINARY_DISCOVERY_MIN_EARLY_REGRET_WIN_PERCENT = 1.0;
    public static final double EXTRAORDINARY_DISCOVERY_MIN_STRENGTH_GAIN_WIN_PERCENT = 20.0;

    /*
     * A material sacrifice must be real and relevant. Three points keeps the
     * established bishop/knight threshold. The practical-chance floor avoids
     * labelling routine liquidation in already hopeless positions as special.
     */
    public static final double EXTRAORDINARY_MATERIAL_INVESTMENT = 3.0;
    public static final int EXTRAORDINARY_MATERIAL_HORIZON_PLIES = 6;
    public static final double EXTRAORDINARY_MATERIAL_MIN_BEST_WIN_PERCENT = 15.0;

    public static final double MISTAKE_WIN_PERCENT_LOSS = 10.0;
    public static final double BLUNDER_WIN_PERCENT_LOSS = 25.0;

    private MoveAnnotationPolicy() {
    }
}
