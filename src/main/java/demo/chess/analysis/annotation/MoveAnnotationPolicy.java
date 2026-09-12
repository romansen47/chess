package demo.chess.analysis.annotation;

/**
 * Central tuning policy for experimental DeepAnalysis move annotations.
 */
public final class MoveAnnotationPolicy {

    public static final double ONLY_MOVE_FINAL_WIN_PERCENT_GAP = 15.0;
    public static final double ONLY_MOVE_TRIVIAL_EARLY_START_RATIO = 0.30;
    public static final double ONLY_MOVE_TRIVIAL_EARLY_END_RATIO = 0.50;
    public static final double ONLY_MOVE_TRIVIAL_WIN_PERCENT_GAP = 10.0;
    public static final double ONLY_MOVE_TRIVIAL_SNAPSHOT_RATIO = 0.70;
    public static final int ONLY_MOVE_TRIVIAL_MIN_SNAPSHOTS = 2;

    public static final int BRILLIANT_MAX_FINAL_RANK = 3;
    public static final double BRILLIANT_MAX_FINAL_REGRET_WIN_PERCENT = 10.0;

    /*
     * Deep-discovery phases are relative to the engine's own final depth.
     * This deliberately avoids Stockfish-specific absolute depths and keeps
     * the model usable for engines such as Lc0 whose depth scale differs.
     */
    public static final double BRILLIANT_DISCOVERY_EARLY_START_RATIO = 0.25;
    public static final double BRILLIANT_DISCOVERY_EARLY_END_RATIO = 0.40;
    public static final double BRILLIANT_DISCOVERY_MIDDLE_START_RATIO = 0.45;
    public static final double BRILLIANT_DISCOVERY_MIDDLE_END_RATIO = 0.65;
    public static final double BRILLIANT_DISCOVERY_LATE_START_RATIO = 0.75;
    public static final int BRILLIANT_DISCOVERY_MIN_PHASE_SNAPSHOTS = 2;

    /*
     * A) Rank/regret discovery: the move becomes substantially better
     * relative to the alternatives as the search develops.
     */
    public static final double BRILLIANT_DISCOVERY_MIN_EARLY_REGRET_WIN_PERCENT = 12.0;
    public static final double BRILLIANT_DISCOVERY_MIN_REGRET_IMPROVEMENT_WIN_PERCENT = 10.0;
    public static final double BRILLIANT_DISCOVERY_MIN_REGRET_PHASE_STEP_WIN_PERCENT = 3.0;

    /*
     * B) Strength discovery: the move is already a plausible candidate early,
     * but the engine discovers that the move itself is much stronger than it
     * first appeared. Winning chance is used instead of raw centipawns so the
     * signal naturally saturates in already decided positions.
     */
    public static final double BRILLIANT_DISCOVERY_MIN_STRENGTH_GAIN_WIN_PERCENT = 20.0;
    public static final double BRILLIANT_DISCOVERY_MIN_STRENGTH_PHASE_STEP_WIN_PERCENT = 5.0;
    public static final double BRILLIANT_DISCOVERY_EARLY_TOP_THREE_RATIO = 0.50;
    public static final double BRILLIANT_DISCOVERY_MIN_STRENGTH_EARLY_REGRET_WIN_PERCENT = 1.0;

    /*
     * A pure material sacrifice should still preserve meaningful practical
     * chances. This prevents forced/obvious liquidation while already
     * hopelessly lost from becoming "!!". Deep-discovery evidence is assessed
     * independently and is deliberately not subject to this threshold.
     */
    public static final double BRILLIANT_MATERIAL_MIN_BEST_WIN_PERCENT = 15.0;
    public static final double BRILLIANT_MATERIAL_INVESTMENT = 3.0;
    public static final int BRILLIANT_MATERIAL_HORIZON_PLIES = 6;

    public static final double MISTAKE_WIN_PERCENT_LOSS = 10.0;
    public static final double BLUNDER_WIN_PERCENT_LOSS = 25.0;

    private MoveAnnotationPolicy() {
    }
}
