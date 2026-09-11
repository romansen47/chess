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
    public static final double BRILLIANT_DISCOVERY_EARLY_DEPTH_RATIO = 0.60;
    public static final double BRILLIANT_DISCOVERY_LATE_DEPTH_RATIO = 0.75;
    public static final double BRILLIANT_DISCOVERY_WIN_PERCENT_GAIN = 15.0;
    public static final double BRILLIANT_MATERIAL_INVESTMENT = 2.0;
    public static final int BRILLIANT_MATERIAL_HORIZON_PLIES = 6;

    public static final double MISTAKE_LOSS = 1.0;
    public static final double BLUNDER_LOSS = 3.0;

    private MoveAnnotationPolicy() {
    }
}
