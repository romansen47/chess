package demo.chess.analysis.annotation;

/**
 * Objective move quality derived solely from the final engine loss relative to
 * the best candidate. Human-difficulty signals are evaluated separately.
 */
enum ObjectiveMoveQuality {
    ACCEPTABLE,
    MISTAKE,
    BLUNDER
}
