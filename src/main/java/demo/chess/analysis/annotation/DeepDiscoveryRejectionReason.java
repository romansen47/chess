package demo.chess.analysis.annotation;

/** Why a sound best move did not qualify for the deep-discovery "!!" signal. */
public enum DeepDiscoveryRejectionReason {
    INSUFFICIENT_EARLY_SNAPSHOTS,
    EARLY_REGRET_TOO_SMALL,
    STRENGTH_GAIN_TOO_SMALL
}
